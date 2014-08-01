package com.net.rtsp;

import com.net.rtp.H264RTP;
import com.net.rtp.IRaw;
import com.net.rtp.RTCP;
import com.net.rtp.RTPWrapper;
import com.net.utils.BIT;
import com.net.utils.OutputStreamHolder;
import com.net.utils.uInt;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by calc on 14.07.14.
 *
 */
public class Rtsp {
    public static final int RTSP_OK = 200;
    private static Logger log = Logger.getLogger(Rtsp.class.getName());

    //constants
    public static final int PACKET_BUFFER_COUNT = 100;
    public static final int DEFAULT_RTSP_PORT = 554;
    public static final int CR = 13;
    public static final int LF = 10;
    public static final String CRLF = "\r\n";
    public static final int SOCKET_READ_TIMEOUT = 3000;

    private final static String PROTOCOL = "RTSP/1.0";
    private final static String USER_AGENT = "LibVLC/2.1.4 (LIVE555 Streaming Media v2014.01.21)";
    private final static String C_SEQ = "CSeq: ";

    private URI uri;
    private InputStream in;
    private OutputStream out;
    private int localRtspPort = 0;

    private int sequence = 1;
    private Reply lastReply;

    public boolean stop = false;
    private boolean isInterleaved = false;
    private Process process = null;

    private int[] map;
    private String session = "";
    private List<Source> sources= new ArrayList<Source>();


    public void setMap(int[] map) {
        this.map = map;
    }

    public void connect(final URI uri) throws IOException {
        log.info("connect to: " + uri.toString());
        this.uri = uri;
        String host = uri.getHost();
        int port = uri.getPort();
        if(port == -1) port = DEFAULT_RTSP_PORT;

        Socket socket = new Socket(host, port);
        localRtspPort = socket.getLocalPort();
        socket.setSoTimeout(SOCKET_READ_TIMEOUT);
        socket.setReceiveBufferSize(5 * 1024 * 1024);
        log.info("socket receive buffer size: " + socket.getReceiveBufferSize());

        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public void options() throws IOException {
        String packet =
                "OPTIONS " + uri + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                        CRLF;
        send(packet);
    }

    public SDP describe() throws IOException {
        log.info("describe");
        String packet = "DESCRIBE " + uri + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Accept: application/sdp" + CRLF +
                CRLF;

        send(packet);

        String response = lastReply.getReply();

        if(lastReply.getCode() != RTSP_OK){
            log.warning("describe return code: " + lastReply.getCode());
            return null;
        }

        if(!response.contains(SDP.CONTENT_LENGTH)){
            log.warning("SDP не получен, нет Content-length");
            return null;
        }

        /*
        Ищем "Content-length:" и <value> \r\n
                              ^          ^
         */
        int startIndex = response.indexOf(SDP.CONTENT_LENGTH) + SDP.CONTENT_LENGTH.length();
        int length;
        try {
            length = Integer.parseInt(
                    response.substring(
                        startIndex,
                        response.indexOf("\r\n", startIndex)
                    ).trim()
            );

        } catch (NumberFormatException e) {
            log.log(Level.SEVERE, "error with " + SDP.CONTENT_LENGTH);
            log.log(Level.SEVERE, response);
            log.log(Level.SEVERE, ExceptionUtils.getStackTrace(e));
            return null;
        }

        byte[] buffer = new byte[length];
        int readed = in.read(buffer);
        if(readed < length){
            log.warning("can not read SDP, readed: " + readed + "/" + length);
            return null;
        }
        //byte[] b = read(length);
        //if(log.isLoggable(Level.FINE)) log.fine(new String(b));

        return new SDP(new String(buffer));
    }

    public int setup(String path, boolean interleaved) throws IOException {
        int map1 = (sources.size()) * 2;
        int map2 = map1 + 1;

        log.info(String.format("setup path %s, map %d-%d, interleaved %b, session %s",
                path, map1, map2, interleaved, session));
        String request;
        if(path.substring(0, 5).equals("rtsp:"))
            request = path;
        else{
            if(!uri.toString().substring(uri.toString().length() - 2).equals("/") && !path.substring(0, 1).equals("/"))
                request = uri + "/" + path;
            else
                request = uri + path;
        }

        String transport;
        if(interleaved){
            isInterleaved = true;
            transport = "RTP/AVP/TCP;unicast;interleaved=" + map1 + "-" + map2;
        }
        else{
            if(map.length <= map1 || map.length <= map2)
                throw new IllegalStateException("Please set port map before using non interleaved mode");
            transport = "RTP/AVP;unicast;client_port=" + map[map1] + "-" + map[map2];
        }

        String sessionString = "";
        if(!session.equals("")){
            sessionString = "Session: " + session + CRLF;
        }

        String packet = "SETUP " + request + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Transport: " + transport + CRLF +
                sessionString +
                CRLF;
        send(packet);

        if(session.equals("")){
            session = lastReply.getSession();
            log.info("session: " + session);
        }

        if(lastReply.getCode() == RTSP_OK){
            Source s = new Source();
            s.ch = map1;
            s.controlCh = map2;
            sources.add(s);
        }else{
            log.warning("RTSP Reply code: " + lastReply.getCode());
        }

        return lastReply.getCode();
    }

    public int play(final OutputStream[] os) throws IOException {
        String packet = "PLAY " + uri + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Session: " + session + CRLF +
                "Range: npt=0.000-" + CRLF +
                CRLF;
        send(packet);

        if(lastReply.getCode() == RTSP_OK){
            if(isInterleaved){
                process = new InterleavedProcess(os);
            }
            else{
                process = new NonInterleavedProcess(os);
            }

            process.processAll();
        }
        else{
            log.warning("RTSP Reply code: " + lastReply.getCode());
        }

        return lastReply.getCode();
    }

    public void getParameter() throws IOException {
        String packet = "GET_PARAMETER " + uri + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Session: " + session + CRLF +
                CRLF;
        send(packet, false);
    }

    /*private byte[] read(int length) throws IOException {
        byte[] buffer = new byte[length];

        for (int i = 0; i < length; i++) {
            buffer[i] = (byte)in.read();
        }

        return buffer;
    }*/

    private String readLine() throws IOException {
        int ch = -1;
        byte[] buffer = new byte[8192];
        int readed = 0;
        boolean end = false;
        while(!end){
            try {
                ch = in.read();
            } catch (IOException e) {
                log.warning("readed: " + readed);
                e.printStackTrace();
            }
            if(ch == -1) throw new IOException("End of stream");
            if(ch == 10) end = true;
            buffer[readed++] = (byte)ch;
        }

        String s = new String(buffer, 0, readed);
        return s.trim();
    }

    private void readReply() throws IOException {
        String reply = "";

        boolean end = false;
        while(!end){
            String line = readLine();
            reply += line + CRLF;
            if(log.isLoggable(Level.FINE)) log.fine(line);
            if(line.equals("")) end = true;
        }

        lastReply = new Reply(reply);
        if(log.isLoggable(Level.FINE)) log.fine(reply);
    }

    private void send(String packet) throws IOException {
        send(packet, true);
    }

    private void send(String packet, boolean needReply) throws IOException {
        sequence++;
        log.info(packet);
        out.write(packet.getBytes());

        if(needReply) readReply();
    }

    private abstract class Process{
        private Thread getParameterThread;

        protected Process(OutputStream[] os) {
            this.os = os;
        }

        protected OutputStream[] os;
        public void processAll() throws IOException{
            getParameterThread = createGetParameterThread();
            getParameterThread.start();
        }

        public void stop(){
            getParameterThread.interrupt();
            try {
                getParameterThread.join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        protected Thread createGetParameterThread(){
            return new Thread(new Runnable() {
                @Override
                public void run() {
                    while(!stop){
                        try {
                            Thread.sleep(55000);
                            getParameter();
                        } catch (InterruptedException e) {
                            //e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        }
    }

    private class NonInterleavedProcess extends Process{
        DatagramSocket[] ss;
        Thread[] ts;

        public NonInterleavedProcess(OutputStream[] os) {
            super(os);
        }

        private void createSockets() throws SocketException {
            ss = new DatagramSocket[map.length];

            for (int i = 0; i < map.length; i++) {
                ss[i] = new DatagramSocket(map[i]);
            }
        }

        private void createThreads(){
            ts = new Thread[map.length];
            for (int i = 0; i < map.length; i++) {
                ts[i] = new Thread(new UDPThread(this, i));
            }
        }

        private void runThreads(){
            for (int i = 0; i < map.length; i++) {
                ts[i].start();
            }
        }

        @Override
        public void processAll() throws IOException {
            super.processAll();

            createSockets();
            createThreads();
            runThreads();
        }

        public void process(int channel) throws IOException {
            byte[] buffer = RTPWrapper.createBuffer();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            TreeMap<Integer, byte[]> sequencedPackets = new TreeMap<Integer, byte[]>();
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            int seqFloor = 0;   //"пол" - минимальное значение, до которого пакеты откидываются (нам уже не интересны)

            RTPWrapper rtp = new RTPWrapper(buffer, buffer.length);
            while(!stop){
                try {
                    ss[channel].receive(packet);
                } catch (SocketException e) {
                    log.warning(e.getMessage());
                    break;
                }

                //RTPWrapper rtp = new RTPWrapper(buffer, packet.getLength());
                rtp.setLength(packet.getLength());
                IRaw raw = rtp;
                try {
                    raw = rtp.getByPayload();
                } catch (NotImplementedException e){
                    log.warning(e.getMessage());
                    continue;
                }

                //процесс RTCP
                /*if(rtp.getPayloadType() == RTPWrapper.TYPE_RTCP){
                    RTCP rtcp = (RTCP)rtp;

                    if(rtcp.getPT() == RTCP.TYPE_SENDER_REPORT){
                        byte[] b = RTCP.response201(rtcp, 0, 0, channel, 0, 0);
                        DatagramPacket reply = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
                        ss[channel].send(reply);
                    } else if(rtcp.getPT() == RTCP.TYPE_SOURCE_DESCRIPTION){
                        byte[] b = RTCP.response202(rtcp, channel);
                        DatagramPacket reply = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
                        ss[channel].send(reply);
                    }

                    if(rtcp.isHaveNextRTCP()){
                        rtcp = rtcp.getNextRTCP();

                        if(rtcp.getPT() == RTCP.TYPE_SENDER_REPORT){
                            byte[] b = RTCP.response201(rtcp, 0, 0, channel, 0, 0);
                            DatagramPacket reply = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
                            ss[channel].send(reply);
                        } else if(rtcp.getPT() == RTCP.TYPE_SOURCE_DESCRIPTION){
                            byte[] b = RTCP.response202(rtcp, channel);
                            DatagramPacket reply = new DatagramPacket(b, b.length, packet.getAddress(), packet.getPort());
                            ss[channel].send(reply);
                        }
                    }
                }*/

                //вообще перестать заботится об ауте RTCP
                if(os[channel] == null) continue;

                raw.writeRawToStream(byteOut);

                //if(rtp.getSequence() > seqFloor){       //все пакеты меньше "пола" тупо пропускаем...
                sequencedPackets.put(rtp.getSequence(), byteOut.toByteArray()); //сохраняем пакет в сортировочную машину.
                byteOut.reset();
                //}

                if(sequencedPackets.size() > PACKET_BUFFER_COUNT){
                    //Если прошел круг (65536), то "пол" будет явно больше первого элемента
                    // 65534 и 1 && 1 > 100 .... 65535 и 100
                    if(seqFloor > sequencedPackets.firstKey()){
                        //буферим
                        if(sequencedPackets.size() > PACKET_BUFFER_COUNT * 2){
                            SortedMap<Integer, byte[]> tail = sequencedPackets.tailMap(seqFloor);   //всё что выше пола
                            //пишем хвост
                            for(Map.Entry<Integer, byte[]> entry : tail.entrySet()){
                                synchronized (os[channel]){
                                    os[channel].write(entry.getValue());
                                }
                            }
                            tail.entrySet().clear();    //чистим хвост
                            seqFloor = 0;   //сброс пола
                        }
                    }
                    else
                    {
                        seqFloor = sequencedPackets.firstKey();     //получить ключ первого пакет
                        byte[] b = sequencedPackets.get(seqFloor);  //вынуть данные
                        synchronized (os[channel]){
                            /*if(out != null)*/
                            os[channel].write(b);               //записать в out stream
                        }
                        sequencedPackets.remove(seqFloor);          //удалить из списка
                    }
                }
            }
            //flush treeMap to out on close socket
            for(Map.Entry<Integer, byte[]> entry : sequencedPackets.entrySet()){
                synchronized (os[channel]){
                    if(os[channel] != null) os[channel].write(entry.getValue());
                }
            }
        }

        public void stop() {
            super.stop();

            for(DatagramSocket s : ss){
                s.close();
            }

            for(Thread t : ts){
                try {
                    t.interrupt();
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class Source{
        public int ch;
        public int controlCh;
        public int SSRC;
        public RTCP lastRTCP;
        public long lastRTCPTime;
        public long jitter;
        public long transit;
        public int loop;
        public int sequence;

        //Перевезти в эти переменные массив os[]
        private OutputStream dataOut;
        private OutputStream controlOut;

        public void calculate(RTPWrapper rtp){
            long ts = uInt.get(rtp.getTimestamp());
            long arrival = System.currentTimeMillis() / 1000L;
            long transit = arrival - ts;
            long d = transit - this.transit;
            this.transit = transit;
            if(d < 0) d = -d;
            jitter += (1./16.) * ((double)d - jitter);

            int oldSeq = sequence;
            int newSeq = rtp.getSequence();
            if(newSeq < oldSeq) loop++;
            sequence = newSeq;
        }

        public void setDataOut(OutputStream dataOut) {
            this.dataOut = dataOut;
        }

        public void setControlOut(OutputStream controlOut) {
            this.controlOut = controlOut;
        }
    }


    private class InterleavedProcess extends Process{
        Thread t;

        public InterleavedProcess(OutputStream[] os) {
            super(os);
        }

        @Override
        public void processAll() throws IOException {
            super.processAll();

            t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        process();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();
        }

        private int source(int ch){
            return (ch / 2);
        }

        private Thread createRTCPThread(){
            return new Thread(new Runnable() {
                private void send(Source s){
                    if(s.lastRTCP == null) return;

                    ByteArrayOutputStream bo = new ByteArrayOutputStream();

                    byte[] frame = {'$', (byte)s.controlCh, 0, 52};
                    try {
                        bo.write(frame);
                        RTCP r = s.lastRTCP;
                        do {
                            if(r.getPT() == RTCP.TYPE_SENDER_REPORT){
                                bo.write(
                                        RTCP.response201(r, s.loop, s.sequence, s.controlCh,
                                                s.lastRTCPTime, (int)s.jitter));
                            }
                            else if(r.getPT() == RTCP.TYPE_SOURCE_DESCRIPTION){
                                bo.write(RTCP.response202(r, s.controlCh));
                            } else {
                                System.out.println("RTCP Thread - Херня какая то: " + r.getPT());
                            }
                        }while ( (r = r.getNextRTCP()) != null);
                        out.write(bo.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void run() {
                    while(!stop){
                        try {
                            Thread.sleep(4500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        send(sources.get(0));
                        //send ch1
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //send ch2
                        send(sources.get(1));
                    }
                }
            });
        }

        public void process() throws IOException {
            stop = false;

            byte[] buffer = RTPWrapper.createBuffer();

            Thread t = createRTCPThread();
            //t.start();
            //Thread t2 = createGetParameterThread();
            //t2.start();

            //for test purposes
            boolean justRead = true;

            //Wrappers
            RTSPInterleavedFrameWrapper frame = new RTSPInterleavedFrameWrapper();
            RTPWrapper rtp = new RTPWrapper(buffer, buffer.length);

            while(!stop){
                IRaw raw = rtp;
                //читаем фрейм
                try {
                    while(!frame.fill(in));

                    //полюбому читаем rtp пакет
                    rtp.fill(in, frame.getLength());
                    try {
                        raw = rtp.getByPayload();
                    } catch (NotImplementedException e) {
                        System.out.println(frame);
                        log.warning("rtp seq=" + rtp.getSequence() + ": " + e.getMessage());
                    }
                } catch (SocketException e) {
                    log.warning(e.getMessage()); //socket closed?
                    break;
                }

                byte ch = frame.getChannel();

                //RTCP? //прошивка D-link DCS2103 1.00 слала RTCP и interleaved
                Source s = sources.get(source(ch));
                if(rtp.getPayloadType() == RTPWrapper.TYPE_RTCP){
                    byte[] rb = new byte[frame.getLength()];
                    System.arraycopy(buffer, 0, rb, 0, rb.length);
                    s.lastRTCP = new RTCP(rb, rb.length);    //save last rtsp
                    s.lastRTCPTime = System.currentTimeMillis();
                    System.out.println(frame.getLength());
                } else {
                    s.calculate(rtp); //вычисление для source параметров (для нужд RTCP)
                }

                if(os.length <= ch){
                    log.warning("Нужно больше out стримов: " + ch);
                    continue;
                }

                if(os[ch] == null) continue;

                synchronized (os[ch]){
                    raw.writeRawToStream(os[ch]);
                }
            }

            t.interrupt();
            //t2.interrupt();
            try {
                t.join();
                //t2.join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        @Override
        public void stop() {
            super.stop();

            try {
                t.interrupt();
                t.join();
            } catch (InterruptedException e) {
                log.warning(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private byte[] interleavedFrame = new byte[4];

    /**
     * Magic: 0x24      1 byte
     * Channel: 0x00    1 byte
     * Length: 0x0000   2 bytes
     */
    private class RTSPInterleavedFrameWrapper {
        private int magic = -1;
        private int channel = -1;
        private byte[] length = new byte[2];

        public boolean fill(InputStream in) throws IOException {
            waitMagic();
            channel = in.read();
            if(!validate()) return false;
            //read another 3 bytes
            //buffer[1] = (byte)in.read();
            length[0] = (byte)in.read();
            length[1] = (byte)in.read();
            return true;
        }

        private void waitMagic() throws IOException {
            int readed = 0;
            int ch;
            while( (ch = in.read()) != 0x24){
                if(ch == -1) throw new IOException("End of stream reached");
                readed++;
                System.out.print(String.format("%c", (char) ch));
            }
        }

        private boolean validate(){
            if(channel < 0 || channel > sources.size() * 2){
                log.warning("wrong frame, channel: " + channel +
                    "\nlast seq of source 0 (on port=" + localRtspPort + "): " + sources.get(0).sequence);
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "RTSP Interleaved Frame:"
                    + "\nMagic: " + magic
                    + "\nChannel: " + channel
                    + "\nLength: " + getLength();
        }

        public byte getChannel(){
            return (byte)channel;
        }

        public int getLength(){
            return BIT.makeShort(length, 0);
        }

        /*public void setLength(int length){
            buffer[2] = BIT.HiByte(BIT.LoWord(length));
            buffer[3] = BIT.LoByte(BIT.LoWord(length));
        }*/
    }

    public void stop(){
        stop = true;

        try {
            //Закрыть сокеты
            in.close();
            out.close();
        } catch (IOException e) {
            log.warning(ExceptionUtils.getStackTrace(e));
        }

        process.stop();
    }

    public Reply getLastReply() {
        return lastReply;
    }

    private class UDPThread implements Runnable{
        private NonInterleavedProcess process;
        private int channel;

        private UDPThread(NonInterleavedProcess process, int channel) {
            this.channel = channel;
            this.process = process;
        }

        @Override
        public void run() {
            try {
                process.process(channel);
            } catch (IOException e) {
                log.log(Level.SEVERE, ExceptionUtils.getStackTrace(e));
            }
        }
    }

    public static void nonInterleaved(){
        final Rtsp rtsp = new Rtsp();

        try {
            //rtsp.setDebug(true);
            //rtsp.connect(new URI("rtsp://10.113.151.152:554/tcp_live/profile_token_0"));
            // cvlc -vvv --rtsp-tcp rtsp://10.113.151.152:554/tcp_live/profile_token_0 :sout=#rtp{sdp=rtsp://:8554/} :sout-keep
            //rtsp.connect(new URI("rtsp://10.154.28.203:8554/"));
            rtsp.connect(new URI("rtsp://10.112.28.231:554/live1.sdp"));

            rtsp.options();
            SDP sdp = rtsp.describe();
            System.out.println(sdp.getO().getUnicastAddress());

            String video = sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(video);
            String audio = sdp.getMediaByType(SDP.MediaType.audio).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(audio);

            int[] ports = {49501, 49502, 49503, 49504};

            rtsp.setMap(ports);
            rtsp.setup(video, false);
            Reply reply = rtsp.getLastReply();
            rtsp.setup(audio, false);

            File f = new File("0.udp");
            FileOutputStream fOut = new FileOutputStream(f);

            OutputStream[] outs = new OutputStream[ports.length];
            OutputStreamHolder oh = new OutputStreamHolder(fOut);

            //outs[0] = fOut;
            outs[0] = oh;

            rtsp.play(outs);

            for (int i = 1; i < 6; i++) {
                Thread.sleep(5000);
                oh.change(new FileOutputStream(i + ".udp"));
            }

            /*for (int i = 0; i < 10; i++) {
                Thread.sleep(55000);
                rtsp.getParameter(session);
            }*/

            rtsp.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void interleaved(){
        final Rtsp rtsp = new Rtsp();

        try {
            rtsp.connect(new URI("rtsp://10.112.28.231:554/live1.sdp"));
            //rtsp.connect(new URI("rtsp://10.112.28.231:554/live3.sdp"));
            //rtsp.connect(new URI("rtsp://10.113.151.152:554/tcp_live/profile_token_0"));
            //rtsp.connect(new URI("rtsp://10.113.151.152:554/tcp_live/profile_token_1"));
            //rtsp.connect(new URI("rtsp://10.154.28.203:8554/"));

            rtsp.options();
            SDP sdp = rtsp.describe();
            System.out.println(sdp.getO().getUnicastAddress());

            String video = sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(video);

            SDP.FMTP fmtp = null;
            if(sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.fmtp) != null){
                fmtp = sdp.new FMTP(sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.fmtp).get(0));
            }

            String audio = sdp.getMediaByType(SDP.MediaType.audio).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(audio);

            rtsp.setup(video, true);
            rtsp.setup(audio, true);

            FileOutputStream out = new FileOutputStream("0.tcp");

            OutputStream[] outs = new OutputStream[4];
            OutputStreamHolder oh = new OutputStreamHolder(out);

            outs[0] = oh;
            if(fmtp != null){
                out.write(H264RTP.NON_IDR_PICTURE);
                out.write(fmtp.getSps());
                out.write(H264RTP.NON_IDR_PICTURE);
                out.write(fmtp.getPps());
            }

            rtsp.play(outs);
            //hello git


            for (int i = 1; i < 6; i++) {
                Thread.sleep(5000);
                FileOutputStream newOut = new FileOutputStream(i + ".tcp");
                if(fmtp != null){
                    newOut.write(H264RTP.NON_IDR_PICTURE);
                    newOut.write(fmtp.getSps());
                    newOut.write(H264RTP.NON_IDR_PICTURE);
                    newOut.write(fmtp.getPps());
                }
                oh.change(newOut);
            }

            System.out.println("try to stop...");
            rtsp.stop();

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        interleaved();
        //nonInterleaved();
    }
}
