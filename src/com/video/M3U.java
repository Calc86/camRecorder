package com.video;

import com.model.Archive;
import com.model.Settings;

import java.io.*;
import java.util.List;

/**
 * Created by calc on 24.07.14.
 *
 */
public class M3U {
    public String create(long camID, List<Archive> list) throws IOException {
        String filename = "cam_" + camID + ".m3u";

        File playlist = new File(filename);
        if(playlist.exists())
            playlist.delete();

        playlist.createNewFile();
        FileWriter out = new FileWriter(playlist);

        for(Archive archive: list){
            String path = Settings.getInstance().getFullRecPath() + archive.getFileName() + ".mp4" + "\n";
            out.write(path);
        }
        out.flush();
        out.close();

        return filename;
    }
}
