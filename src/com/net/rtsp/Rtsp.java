package com.net.rtsp;

import com.net.rtp.H264RTP;
import com.net.rtp.RTCP;
import com.net.rtp.RTP;
import com.net.utils.BIT;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.apache.commons.lang3.NotImplementedException;
import sun.text.resources.FormatData_ja;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by calc on 14.07.14.
 *
 */
public class Rtsp {
    public static final int PACKET_BUFFER_COUNT = 100;
    public static final int DEFAULT_RTSP_PORT = 554;
    public static final int CR = 13;
    public static final int LF = 10;
    public static final String CRLF = "\r\n";

    private URI uri;
    private final static String PROTOCOL = "RTSP/1.0";
    private final static String USER_AGENT = "LibVLC/2.1.4 (LIVE555 Streaming Media v2014.01.21)";
    private final static String C_SEQ = "CSeq: ";

    private InputStream in;
    private OutputStream out;

    private int sequence = 1;
    private Reply lastReply;

    private boolean debug = false;
    public boolean stop = false;
    private boolean isInterleaved = false;
    private Process process = null;

    private int[] map;

    public void setMap(int[] map) {
        this.map = map;
    }

    public void connect(final URI uri) throws IOException {
        this.uri = uri;
        String host = uri.getHost();
        int port = uri.getPort();
        if(port == -1) port = DEFAULT_RTSP_PORT;

        Socket socket = new Socket(host, port);

        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public void setDebug(boolean on){
        debug = on;
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
        String packet = "DESCRIBE " + uri + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Accept: application/sdp" + CRLF +
                CRLF;

        send(packet);

        String response = lastReply.getReply();

        if(lastReply.getCode() != 200){
            return null;
        }

        if(!response.contains(SDP.CONTENT_LENGTH)){
            System.err.println("SDP не получен, нет Content-length");
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
            System.err.println("error with " + SDP.CONTENT_LENGTH);
            System.err.println(response);
            e.printStackTrace();
            return null;
        }

        byte[] b = read(length);
        if(debug) System.out.println(new String(b));

        return new SDP(new String(b));
    }

    public void setup(String path, int map1, int map2, boolean interleaved, String session) throws IOException {
        String request;
        if(path.substring(0, 5).equals("rtsp:"))
            request = path;
        else{
            if(!uri.toString().substring(uri.toString().length() - 2).equals("/") && !path.substring(0, 1).equals("/"))
                request = uri + "/" + path;
            else
                request = uri + path;
        }

        if(!session.equals("")) session = Reply.SESSION + " " + session + CRLF;

        String transport;
        if(interleaved){
            isInterleaved = true;
            transport = "RTP/AVP/TCP;unicast;interleaved=" + map1 + "-" + map2;
        }
        else{
            transport = "RTP/AVP;unicast;client_port=" + map[map1] + "-" + map[map2];
        }

        String packet = "SETUP " + request + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Transport: " + transport + CRLF +
                session +
                CRLF;
        send(packet);
    }

    public void play(final String session, final OutputStream[] os) throws IOException {
        sequence++;

        String packet = "PLAY " + uri + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Session: " + session + CRLF +
                "Range: npt=0.000-" + CRLF +
                CRLF;
        send(packet);

        if(isInterleaved){
            //System.out.println("interleaved");
            process = new InterleavedProcess(os);
        }
        else{
            //System.out.println("nonInterleaved");
            process = new NonInterleavedProcess(os);
        }

        process.processAll();
    }

    public void getParameter(final String session) throws IOException {
        sequence++;

        String packet = "GET_PARAMETER " + uri + " " + PROTOCOL + CRLF +
                C_SEQ + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Session: " + session + CRLF +
                CRLF;
        send(packet);
    }

    private byte[] read(int length) throws IOException {
        byte[] buffer = new byte[length];

        for (int i = 0; i < length; i++) {
            buffer[i] = (byte)in.read();
        }

        return buffer;
    }

    private void readReply() throws IOException {
        String reply;

        byte[] buffer = new byte[1024];
        int readed = 0;

        while(true){
            buffer[readed++] = (byte)in.read();

            if(readed >= 4){
                if(
                    buffer[readed-4] == CR &&
                    buffer[readed-3] == LF &&
                    buffer[readed-2] == CR &&
                    buffer[readed-1] == LF
                )
                    break;
                if(readed >= buffer.length)
                    throw new IOException("readed too many bytes: " + buffer.length + " readed: " + readed);
            }
        }

        reply = new String(buffer, 0, readed);
        lastReply = new Reply(reply);
        if(debug) System.out.println(reply);
    }

    private void send(String packet) throws IOException {
        sequence++;
        if(debug) System.out.println(packet);
        out.write(packet.getBytes());

        readReply();
    }

    public class OutputStreamHolder extends OutputStream {
        volatile private OutputStream out;

        public OutputStreamHolder(final OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        /*public void setOut(OutputStream out) {
            this.out = out;
        }*/

        public synchronized void change(final OutputStream out) throws IOException {
            this.out.flush();
            this.out.close();
            this.out = out;
        }
    }

    private abstract class Process{
        protected Process(OutputStream[] os) {
            this.os = os;
        }

        protected OutputStream[] os;
        abstract public void processAll() throws IOException;
        abstract public void stop();
    }

    private class NonInterleavedProcess extends Process{
        DatagramSocket[] ss;
        Thread[] ts;

        public NonInterleavedProcess(OutputStream[] os) {
            super(os);
        }

        @Override
        public void processAll() throws SocketException {
            ss = new DatagramSocket[map.length];

            for (int i = 0; i < map.length; i++) {
                ss[i] = new DatagramSocket(map[i]);
            }

            ts = new Thread[map.length];
            for (int i = 0; i < map.length; i++) {
                ts[i] = new Thread(new UDPThread(this, i));
            }

            for (int i = 0; i < map.length; i++) {
                ts[i].start();
            }
        }

        public void process(int channel) throws IOException {
            byte[] buffer = RTP.createBuffer();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            TreeMap<Integer, byte[]> sequencedPackets = new TreeMap<Integer, byte[]>();
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            int seqFloor = 0;   //"пол" - минимальное значение, до которого пакеты откидываются (нам уже не интересны)

            while(!stop){
                try {
                    ss[channel].receive(packet);
                } catch (SocketException e) {
                    System.out.println(e.getMessage());
                    break;
                }

                //RTP rtp = new RTP(buffer, packet.getLength());
                RTP rtp;
                try {
                    rtp = (new RTP(buffer, packet.getLength())).getRtpByPayload();
                } catch (NotImplementedException e){
                    //System.err.println(e.getMessage());
                    continue;
                }

                if(rtp.getPayloadType() == RTP.TYPE_RTCP){
                    RTCP rtcp = (RTCP)rtp;
                    rtcp.justCopy();
                    DatagramPacket reply = new DatagramPacket(rtcp.getBuffer(), 52, packet.getAddress(), packet.getPort());
                    ss[channel].send(reply);
                }

                if(os[channel] == null) continue;

                rtp.writeRawToStream(byteOut);

                //if(rtp.getSequence() > seqFloor){       //все пакеты меньше "пола" тупо пропускаем...
                byteOut.flush();
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
            for(DatagramSocket s : ss){
                s.close();
            }

            for(Thread t : ts){
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class InterleavedProcess extends Process{
        Thread t;

        public InterleavedProcess(OutputStream[] os) {
            super(os);
        }

        @Override
        public void processAll() throws IOException {
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

        public void process() throws IOException {
            stop = false;

            byte[] buffer = RTP.createBuffer();

            while(!stop){
                RTSPInterleavedFrame frame;
                try {
                    frame = new RTSPInterleavedFrame(in);
                } catch (SocketException e) {
                    //socket closed?
                    System.err.println(e.getMessage());
                    break;
                }
                byte channel = frame.getChannel();

                //по любому читаем rtp пакет
                RTP rtp;
                try {
                    rtp = (new RTP(in, buffer, frame.getLength())).getRtpByPayload();
                } catch (NotImplementedException e) {
                    continue;
                } catch (SocketException e) {
                    //socket closed?
                    System.err.println(e.getMessage());
                    break;
                }

                if(os.length <= channel){
                    System.err.println("Нужно больше out стримов: " + channel);
                    continue;
                }

                if(os[channel] == null) continue;

                synchronized (os[channel]){
                    rtp.writeRawToStream(os[channel]);
                }
            }
        }

        @Override
        public void stop() {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class RTSPInterleavedFrame{

        private byte[] buffer = new byte[4];

        InputStream in;

        RTSPInterleavedFrame(InputStream in) throws IOException {
            this.in = in;

            readAndWaitMagicDollar();
            //read another 3 bytes
            buffer[1] = (byte)in.read();
            buffer[2] = (byte)in.read();
            buffer[3] = (byte)in.read();
        }

        private void readAndWaitMagicDollar() throws IOException {
            while( (buffer[0] = (byte)in.read()) != 0x24){
                System.err.println("$ error: " + buffer[0]);
            }
        }

        public byte getChannel(){
            return buffer[1];
        }

        public int getLength(){
            return BIT.makeShort(buffer, 2);
        }
    }

    public void stop(){
        stop = true;

        try {
            //Закрыть сокеты
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
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
                e.printStackTrace();
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
            rtsp.setup(video, ports[0], ports[1], false, "");
            Reply reply = rtsp.getLastReply();
            String session = reply.getSession();
            System.out.println(session);
            rtsp.setup(audio, ports[2], ports[3], false, session);

            File f = new File("0.udp");
            FileOutputStream fOut = new FileOutputStream(f);

            OutputStream[] outs = new OutputStream[ports.length];
            OutputStreamHolder oh = rtsp.new OutputStreamHolder(fOut);

            //outs[0] = fOut;
            outs[0] = oh;

            rtsp.play(session, outs);

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
            rtsp.setDebug(true);
            //rtsp.connect(new URI("rtsp://10.112.28.231:554/live1.sdp"));
            rtsp.connect(new URI("rtsp://10.112.28.231:554/live3.sdp"));
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

            rtsp.setup(video, 0, 1, true, "");
            Reply reply = rtsp.getLastReply();
            String session = reply.getSession();
            System.out.println(session);
            rtsp.setup(audio, 2, 3, true, session);

            FileOutputStream out = new FileOutputStream("0.tcp");

            OutputStream[] outs = new OutputStream[4];
            OutputStreamHolder oh = rtsp.new OutputStreamHolder(out);

            outs[0] = oh;
            if(fmtp != null){
                out.write(H264RTP.NON_IDR_PICTURE);
                out.write(fmtp.getSps());
                out.write(H264RTP.NON_IDR_PICTURE);
                out.write(fmtp.getPps());
            }

            rtsp.play(session, outs);

            for (int i = 1; i < 6; i++) {
                Thread.sleep(5000);
                FileOutputStream newOut = new FileOutputStream(i + ".tcp");
                if(fmtp != null){
                    out.write(H264RTP.NON_IDR_PICTURE);
                    out.write(fmtp.getSps());
                    out.write(H264RTP.NON_IDR_PICTURE);
                    out.write(fmtp.getPps());
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
