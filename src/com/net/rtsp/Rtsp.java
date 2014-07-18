package com.net.rtsp;

import com.net.rtp.H264RTP;
import com.net.rtp.JpegRTP;
import com.net.rtp.RTP;
import com.net.utils.BIT;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by calc on 14.07.14.
 *
 */
public class Rtsp {
    public static final int DEFAULT_RTSP_PORT = 554;
    private URI uri;

    private final static String PROTOCOL = "RTSP/1.0";
    private final static String USER_AGENT = "LibVLC/2.1.4 (LIVE555 Streaming Media v2014.01.21)";
    public static final String CRLF = "\r\n";

    private InputStream in;
    private OutputStream out;

    private int sequence = 1;
    private Reply lastReply;

    private boolean debug = false;

    public boolean stop = false;

    public void connect(URI uri) throws IOException {
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
                "cSeq: " + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                        CRLF;
        write(packet);
    }

    public SDP describe() throws IOException {
        String packet = "DESCRIBE " + uri + " " + PROTOCOL + CRLF +
                "cSeq: " + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Accept: application/sdp" + CRLF +
                CRLF;

        write(packet);

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
        else
            request = uri + path;

        if(!session.equals("")) session = Reply.SESSION + " " + session + CRLF;

        String transport;
        if(interleaved){
            transport = "RTP/AVP/TCP;unicast;interleaved=" + map1 + "-" + map2;
        }
        else{
            transport = "RTP/AVP;unicast;client_port=" + map1 + "-" + map2;
        }

        String packet = "SETUP " + request + " " + PROTOCOL + CRLF +
                "cSeq: " + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Transport: " + transport + CRLF +
                session +
                CRLF;
        write(packet);
    }

    public void play(String session) throws IOException {
        sequence++;

        String packet = "PLAY " + uri + " " + PROTOCOL + CRLF +
                "cSeq: " + sequence + CRLF +
                "User-Agent: " + USER_AGENT + CRLF +
                "Session: " + session + CRLF +
                "Range: npt=0.000-" + CRLF +
                CRLF;
        write(packet);
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


            if(readed >=4){
                if(
                    buffer[readed-4] == 13 &&
                    buffer[readed-3] == 10 &&
                    buffer[readed-2] == 13 &&
                    buffer[readed-1] == 10
                )
                    break;
                if(readed >= buffer.length)
                {
                    System.err.println("readed too many bytes: " + buffer.length);
                    lastReply = null;
                    return;
                }
            }
        }

        reply = new String(buffer, 0, readed);
        lastReply = new Reply(reply);
        if(debug) System.out.println(reply);
    }

    private void write(String packet) throws IOException {
        sequence++;
        if(debug) System.out.println(packet);
        out.write(packet.getBytes());

        readReply();
    }

    public void nonInterleavedProcess(DatagramSocket s, OutputStream out) throws IOException {
        byte[] buffer = RTP.createBuffer();

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        TreeMap<Integer, byte[]> sequencedPackets = new TreeMap<Integer, byte[]>();
        int preFill = 100;   //буфер
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        int seqFloor = 0;   //"пол" - минимальное значение, до которого пакеты откидываются (нам уже не интересны)

        int count = 0;
        while(!stop){
            try {
                s.receive(packet);
            } catch (IOException e) {
                //flush treeMap to out on close socket
                for(Map.Entry<Integer, byte[]> entry : sequencedPackets.entrySet()){
                    System.out.println(e.getMessage());
                    if(out != null) out.write(entry.getValue());
                }
                break; // exit from while
            }

            RTP rtp = new RTP(buffer, packet.getLength());

            if(rtp.getPayloadType() == RTP.TYPE_H264){
                H264RTP h264rtp = new H264RTP(rtp);
                h264rtp.writeRawH264toStream(byteOut);  //пишем пакет в byteArray
                if(rtp.getSequence() > seqFloor){       //все пакеты меньше "пола" тупо пропускаем...
                    byteOut.flush();
                    sequencedPackets.put(rtp.getSequence(), byteOut.toByteArray()); //сохраняем пакет в сортировочную машину.
                    byteOut.reset();
                }

                //вообще нужно подумать об сборке полного фрейма и записи его в это дерево.
                if(sequencedPackets.size() > preFill){  //preFill - буфер для "сортировки" входящих пакетов
                    //sequencedPackets.floorKey();
                    seqFloor = sequencedPackets.firstKey();     //получить первый пакет
                    byte[] b = sequencedPackets.get(seqFloor);  //вынуть данные
                    if(out != null) out.write(b);               //записать в out stream
                    sequencedPackets.remove(seqFloor);          //удалить из списка
                }   //при стопе теряем все оставшиеся пакеты...)
            }
            /*if(rtp.getPayloadType() == RTP.TYPE_JPEG){
                JpegRTP jpegrtp = new JpegRTP(rtp);
                jpegrtp.writeRawJPEGtoStream(out);
            }
            else {
                //unknown type
                //just write...
                System.err.println("Unknown rtp payload type: " + rtp.getPayloadType());
                out.write(buffer, rtp.getPayloadStart(), rtp.getPayloadLength());
            }*/
        }
    }

    public void process(OutputStream out) throws IOException {
        OutputStream[] os = new OutputStream[1];
        os[0] = out;
        process(os);
    }

    class RTSPInterleavedFrame{

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
                System.err.println("$ error");
            }
        }

        /*public byte getMagic(){
            return buffer[0];
        }*/

        public byte getChannel(){
            return buffer[1];
        }

        public int getLength(){
            return BIT.makeShort(buffer, 2);
        }
    }

    /**
     *
     * @param os array of output streams, 1 stream for channel
     */
    public void process(OutputStream[] os) throws IOException {
        stop = false;

        byte[] buffer = RTP.createBuffer();

        while(!stop){
            RTSPInterleavedFrame frame = new RTSPInterleavedFrame(in);
            byte channel = frame.getChannel();
            int length = frame.getLength();

            RTP rtp = new RTP(in, buffer, length);

            if(channel == 0 && os.length > 0 && os[0] != null){
                //H264RTP rtp = new H264RTP(in, buffer, length);
                if(rtp.getPayloadType() == RTP.TYPE_H264){
                    H264RTP h264rtp = new H264RTP(rtp);
                    h264rtp.writeRawH264toStream(os[channel]);
                }
                if(rtp.getPayloadType() == RTP.TYPE_JPEG){
                    JpegRTP jpegrtp = new JpegRTP(rtp);
                    jpegrtp.writeRawJPEGtoStream(os[channel]);
                }
                else {
                    //unknown type
                    //just write...
                    System.err.println("Unknown rtp payload type: " + rtp.getPayloadType());
                    os[channel].write(buffer, rtp.getPayloadStart(), rtp.getPayloadLength());
                }
            }
            else if(os.length > channel && os[channel] != null){
                //RTP rtp = new RTP(in, buffer, length);
                os[channel].write(buffer, rtp.getPayloadStart(), rtp.getPayloadLength());
            } /*else{
               new RTP(in, buffer, length); //just read rtp packet from stream
            }*/
        }
        in.close();
        out.close();
    }

    public void stop(){
        stop = true;
    }

    public Reply getLastReply() {
        return lastReply;
    }

    public class UDPThread implements Runnable{
        private Rtsp rtsp;
        private DatagramSocket s;
        private OutputStream out;

        private UDPThread(Rtsp rtsp, DatagramSocket s, OutputStream out) {
            this.s = s;
            this.out = out;
            this.rtsp = rtsp;
        }

        @Override
        public void run() {
            try {
                rtsp.nonInterleavedProcess(s, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void nonInterleaved(){
        final Rtsp rtsp = new Rtsp();

        try {
            rtsp.setDebug(true);
            //rtsp.connect(new URI("rtsp://10.113.151.152:554/tcp_live/profile_token_0"));
            rtsp.connect(new URI("rtsp://10.154.28.203:8554/"));

            rtsp.options();
            SDP sdp = rtsp.describe();
            System.out.println(sdp.getO().getUnicastAddress());

            String video = sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(video);
            String audio = sdp.getMediaByType(SDP.MediaType.audio).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(audio);

            int[] ports = {49501, 49502, 49503, 49504};

            rtsp.setup(video, ports[0], ports[1], false, "");
            Reply reply = rtsp.getLastReply();
            String session = reply.getSession();
            System.out.println(session);
            rtsp.setup(audio, ports[2], ports[3], false, session);


            DatagramSocket[] ss = new DatagramSocket[ports.length];
            for (int i = 0; i < ports.length; i++) {
                ss[i] = new DatagramSocket(ports[i]);
            }

            File f = new File("udp.h264");
            FileOutputStream fOut = new FileOutputStream(f);

            OutputStream[] outs = new OutputStream[ports.length];

            outs[0] = fOut;

            Thread[] ts = new Thread[ports.length];
            for (int i = 0; i < ports.length; i++) {
                //ts[i] = new Thread(new Rtsp.UDPThread(null, null, null));
                ts[i] = new Thread(rtsp.new UDPThread(rtsp, ss[i], outs[i]));
            }

            for (int i = 0; i < ports.length; i++) {
                ts[i].start();
            }

            rtsp.play(session);

            Thread.sleep(5000);
            System.out.println("time is out");

            //rtsp.stop();

            for(DatagramSocket s : ss){
                //s.close();
            }

            for(Thread t : ts){
                t.join();
            }

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
            //rtsp.connect(new URI("rtsp://10.113.151.152:554/tcp_live/profile_token_0"));
            rtsp.connect(new URI("rtsp://10.113.151.152:554/tcp_live/profile_token_1"));
            //rtsp.connect(new URI("rtsp://10.154.28.203:8554/"));

            rtsp.options();
            SDP sdp = rtsp.describe();
            System.out.println(sdp.getO().getUnicastAddress());

            String video = sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(video);
            String audio = sdp.getMediaByType(SDP.MediaType.audio).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(audio);

            rtsp.setup(video, 0, 1, true, "");
            Reply reply = rtsp.getLastReply();
            String session = reply.getSession();
            System.out.println(session);
            rtsp.setup(audio, 2, 3, true, session);

            rtsp.play(session);

            final FileOutputStream out = new FileOutputStream("0.h264");

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        rtsp.process(out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();
            Thread.sleep(5000);
            System.out.println("try to stop...");
            rtsp.stop();
            t.join();
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
        //interleaved();
        nonInterleaved();
    }
}
