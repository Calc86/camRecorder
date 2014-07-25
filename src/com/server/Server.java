package com.server;

import com.model.Cam;
import com.model.Model;
import com.net.rtp.H264RTP;
import com.net.rtsp.Reply;
import com.net.rtsp.Rtsp;
import com.net.rtsp.SDP;
import com.net.utils.OutputStreamHolder;
import org.apache.commons.lang3.NotImplementedException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by calc on 22.07.14.
 *
 */
public class Server {
    public static final int LENGTH_OF_RECORD_IN_SECONDS = 5;
    List<Thread> threads = new ArrayList<Thread>();
    private boolean stop = true;

    private class CamHolder implements Runnable{
        private final Cam cam;

        private CamHolder(Cam cam) {
            this.cam = cam;
        }

        @Override
        public void run() {
            String proto = cam.getUrl().getScheme();
            if(proto.equals("rtsp")){
                rtsp(cam);
            } else if(proto.equals("http")){
                http(cam);
            }
            else{
                throw new NotImplementedException("protocol " + proto + " not implemented");
            }
        }
    }

    public void start() throws NotImplementedException {
        threads.clear();
        if(!isStop()) {
            System.err.println("server already started");
            return;
        }

        Cam cam = new Cam();

        try {
            List<Cam> list = Model.selectAll(cam);

            if(list.size() == 0) return;

            stop = false;

            for(Cam c : list){
                threads.add(new Thread(new CamHolder(c)));
            }

            for(Thread t : threads){
                t.start();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        stop = true;

        for(Thread t : threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private SDP.FMTP getFmtp(SDP sdp){
        SDP.FMTP fmtp = null;
        if(sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.fmtp) != null){
            fmtp = sdp.new FMTP(sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.fmtp).get(0));
        }
        return fmtp;
    }

    private void http(Cam cam){
        try {
            ArchiveRotator rotator = new ArchiveRotator(cam);
            rotator.rotate();
            URL url = new URL(cam.getUrl().toString());
            final HTTPReceiver HTTPReceiver = new HTTPReceiver(url, rotator);

            HTTPReceiver.play();

            while (!stop){
                Thread.sleep(LENGTH_OF_RECORD_IN_SECONDS * 1000);
                if(!stop){
                    rotator.rotate();
                }
                else
                    break;
            }

            HTTPReceiver.stop();
            rotator.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            stop = true;
        }
    }

    private void rtsp(Cam cam){
        final Rtsp rtsp = new Rtsp();

        try {
            rtsp.setDebug(true);
            rtsp.connect(cam.getUrl());

            rtsp.options();
            SDP sdp = rtsp.describe();

            //добавить проверки
            String video = sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println("video:" + video);
            String audio = sdp.getMediaByType(SDP.MediaType.audio).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println("video:" + audio);

            SDP.FMTP fmtp = getFmtp(sdp);

            int[] ports = {49501, 49502, 49503, 49504};
            rtsp.setMap(ports);
            boolean interleaved = true;

            //rtsp.setup(video, 0, 1, true, "");
            rtsp.setup(video, 0, 1, interleaved, "");
            Reply reply = rtsp.getLastReply();
            String session = reply.getSession();
            if(reply.getCode() == 403){
                System.err.println("Non interleaved mode not supported");
                interleaved = !interleaved;
                rtsp.setup(video, 0, 1, interleaved, session);
            }

            System.out.println("session: " + session);
            //rtsp.setup(audio, 2, 3, true, session);
            rtsp.setup(audio, 2, 3, interleaved, session);

            ByteArrayOutputStream fmtpBuffer = new ByteArrayOutputStream();
            if(fmtp != null){
                fmtpBuffer.write(H264RTP.NON_IDR_PICTURE);
                fmtpBuffer.write(fmtp.getSps());
                fmtpBuffer.write(H264RTP.NON_IDR_PICTURE);
                fmtpBuffer.write(fmtp.getPps());
            }

            OutputStream[] outs = new OutputStream[4];
            ArchiveRotator rotator = new ArchiveRotator(cam);
            if(fmtp != null) rotator.rotate(fmtpBuffer.toByteArray());
            else rotator.rotate();
            //save only video
            outs[0] = rotator;

            rtsp.play(session, outs);

            int i = 1;
            while (!stop){
                Thread.sleep(LENGTH_OF_RECORD_IN_SECONDS * 1000);
                if(!stop){
                    if(fmtp != null) rotator.rotate(fmtpBuffer.toByteArray());
                    else rotator.rotate();
                }
            }
            try {
                rtsp.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rotator.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            stop = true;
        }
    }

    public boolean isStop() {
        return stop;
    }
}
