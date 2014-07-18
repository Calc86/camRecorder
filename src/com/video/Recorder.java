package com.video;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by calc on 14.07.14.
 *
 */
public class Recorder {

    private boolean stop = true;

    public void open(URL url) throws IOException {
        System.out.println("start");
        stop = false;

        URLConnection connection = url.openConnection();

        InputStream in = connection.getInputStream();
        //InputStreamReader reader = new InputStreamReader(connection.getInputStream());

        File f = new File("rec.avi");
        FileOutputStream out = new FileOutputStream(f);

        byte buffer[] = new byte[1024];

        int readed = 0;
        while( (readed = in.read(buffer)) != -1 && !stop){
            out.write(buffer, 0, readed);
            System.out.println("Readed: " + readed);
        }

        out.close();
        in.close();
    }

    public void close(){
        System.out.println("stop");
        stop = true;
    }
}
