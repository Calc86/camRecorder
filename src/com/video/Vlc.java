package com.video;

import com.model.Settings;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by calc on 24.07.14.
 *
 */
abstract public class Vlc {
    public static void play(String m3u){

        String command = Settings.getInstance().getVlcPath() + " " + m3u;

        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
