package com.server;

import com.model.Archive;
import com.model.Cam;
import com.net.utils.OutputStreamHolder;

import java.io.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by calc on 25.07.14.
 *
 */
public class ArchiveRotator {
    public static final String DEFAULT_PATH = "./archive/";
    private String path = DEFAULT_PATH;
    private OutputStreamHolder oh;
    private Cam cam;

    public ArchiveRotator(Cam cam) {
        this(cam, DEFAULT_PATH, null);
    }

    public ArchiveRotator(Cam cam, String path) {
        this(cam, DEFAULT_PATH, null);
    }

    public ArchiveRotator(Cam cam, OutputStreamHolder oh) {
        this(cam, DEFAULT_PATH, oh);
    }

    public ArchiveRotator(Cam cam, String path, OutputStreamHolder oh) {
        if(oh == null) oh = new OutputStreamHolder();

        this.oh = oh;
        this.path = path;
        this.cam = cam;

        File dir = new File(path);
        dir.mkdir();
    }

    protected FileOutputStream createFile(Archive archive) throws IOException {
        File f = new File(path + cam.getId() + "_" + archive.getId() + "");
        f.createNewFile();
        return new FileOutputStream(f);
    }

    public synchronized void rotate() throws SQLException, IOException {
        Archive archive = new Archive();
        archive.setCid(cam.getId());
        archive.setStart(new Date().getTime());
        archive.insert();

        oh.change(createFile(archive));
    }

    public OutputStream getOutputStream(){
        return oh;
    }

    public void close() throws IOException {
        oh.close();
    }
}
