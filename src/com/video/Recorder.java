package com.video;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by calc on 14.07.14.
 *
 */
public class Recorder {
    final protected OutputStream out;
    private URL url;
    private boolean stop = true;

    public Recorder(URL url, OutputStream out) {
        this.url = url;
        this.out = out;
    }

    public void play() throws IOException {
        stop = false;
        URLConnection connection = url.openConnection();
        final InputStream in = connection.getInputStream();
        connection.setConnectTimeout(3000);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                byte buffer[] = new byte[1024];

                int readed = 0;

                try {
                    while( (readed = in.read(buffer)) != -1 && !stop){
                        synchronized (out){
                            out.write(buffer, 0, readed);    //записать в out stream
                        }
                    }
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();

        /*byte buffer[] = new byte[1024];

        int readed = 0;
        while( (readed = in.read(buffer)) != -1 && !stop){
            synchronized (out){
                out.write(buffer, 0, readed);    //записать в out stream
            }
        }

        in.close();*/
    }

    public boolean isStop() {
        return stop;
    }

    public void stop(){
        stop = true;
    }
}
