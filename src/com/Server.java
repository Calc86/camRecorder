package com;

import com.model.Cam;
import com.net.rtsp.Reply;
import com.net.rtsp.Rtsp;
import com.net.rtsp.SDP;
import org.apache.commons.lang3.NotImplementedException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
            } else{
                throw new NotImplementedException("protocol " + proto + " not implemented");
            }
        }
    }

    public void start() throws NotImplementedException {
        if(!isStop()) {
            System.err.println("server already started");
            return;
        }

        Cam cam = new Cam();

        try {
            List<Cam> list = cam.selectAll();

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

    private void rtsp(Cam cam){
        final Rtsp rtsp = new Rtsp();

        try {
            rtsp.connect(cam.getUrl());

            rtsp.options();
            SDP sdp = rtsp.describe();

            //добавить проверки
            String video = sdp.getMediaByType(SDP.MediaType.video).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(video);
            String audio = sdp.getMediaByType(SDP.MediaType.audio).get(0).getAttribute(SDP.AttributeName.control).get(0);
            System.out.println(audio);

            rtsp.setup(video, 0, 1, true, "");
            Reply reply = rtsp.getLastReply();
            String session = reply.getSession();
            System.out.println(session);
            rtsp.setup(audio, 2, 3, true, session);

            String fileNamePrefix = cam.getId() + "_";

            final FileOutputStream out = new FileOutputStream(fileNamePrefix + "0.mp4");

            OutputStream[] outs = new OutputStream[4];
            Rtsp.OutputStreamHolder oh = rtsp.new OutputStreamHolder(out);

            //save only video
            outs[0] = oh;

            rtsp.play(session, outs);

            int i = 1;
            while (!stop){
                Thread.sleep(LENGTH_OF_RECORD_IN_SECONDS * 1000);
                oh.change(new FileOutputStream(fileNamePrefix + (i++) + ".mp4"));
            }
            try {
                rtsp.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                oh.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isStop() {
        return stop;
    }
}
