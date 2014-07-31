package com.video;

import com.model.Archive;
import com.model.Settings;
import com.server.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Created by calc on 26.07.14.
 *
 */
public class FFmpeg {
    private static Logger log = Logger.getLogger(Server.class.getName());

    public void deleteOld(String archive){
        File f = new File(Settings.getInstance().getFullTmpPath() + archive);
        f.delete();
    }

    public void move(String archive){
        try {
            Settings settings = Settings.getInstance();
            String command = String.format("%s -i %s -codec copy %s",
                    settings.getFfmpegPath(),
                    settings.getFullTmpPath() + archive,
                    settings.getArchivePath() + "/" + settings.getRecPath() + "/" + archive + ".mp4");

            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);

            int exitVal = pr.waitFor();
            //delete old file
            if(exitVal == 0)
                deleteOld(archive);
            else
                log.warning("FFmpeg " + command + " exited with error code " + exitVal);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
