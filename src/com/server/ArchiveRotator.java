package com.server;

import com.model.Archive;
import com.model.Cam;
import com.model.Settings;
import com.net.utils.OutputStreamHolder;
import com.video.FFmpeg;

import java.io.*;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by calc on 25.07.14.
 *
 */
public class ArchiveRotator extends OutputStreamHolder {
    private static Logger log = Logger.getLogger("main");
    private Cam cam;
    Archive archive = null;

    public ArchiveRotator(Cam cam) {
        this.cam = cam;
    }

    private String getFilename(Archive archive){
        return cam.getId() + "_" + archive.getId();
    }

    protected FileOutputStream createFile(Archive archive) throws IOException {
        File f = new File(Settings.getInstance().getFullTmpPath() + getFilename(archive));
        f.createNewFile();
        return new FileOutputStream(f);
    }

    public synchronized void rotate() throws SQLException, IOException {
        rotate(null);
    }

    public synchronized void rotate(byte[] preWrite) throws SQLException, IOException {
        Archive oldArchive = archive;

        archive = new Archive();
        archive.setCid(cam.getId());
        archive.setStart(new Date().getTime());
        archive.insert();
        log.info("new archive id: " + archive.getId());

        FileOutputStream fOut = createFile(archive);
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fOut, 5*1024*1024);
        if(preWrite != null) bufferedOut.write(preWrite);

        change(bufferedOut);

        if(oldArchive != null){
            FFmpeg ffmpeg = new FFmpeg();
            ffmpeg.move(getFilename(oldArchive));
        }
    }
}
