package ru.xsrv.recorder.server;

import ru.xsrv.recorder.model.Archive;
import ru.xsrv.recorder.model.Cam;
import ru.xsrv.recorder.model.Settings;
import ru.xsrv.net.utils.OutputStreamHolder;
import ru.xsrv.recorder.video.FFmpeg;

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

    protected FileOutputStream createFile(Archive archive) throws IOException {
        File f = new File(Settings.getInstance().getFullTmpPath() + archive.getFileName());
        f.createNewFile();
        return new FileOutputStream(f);
    }

    public synchronized void rotate() throws SQLException, IOException {
        rotate(null);
    }

    private void createArchive() throws SQLException {
        archive = new Archive();
        archive.setCid(cam.getId());
        archive.setStart(new Date().getTime());
        archive.insert();
        log.info("new archive id: " + archive.getId());
    }

    public synchronized void rotate(byte[] preWrite) throws SQLException, IOException {
        Archive oldArchive = archive;

        createArchive();

        FileOutputStream fOut = createFile(archive);
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fOut, 5*1024*1024);
        if(preWrite != null) bufferedOut.write(preWrite);

        change(bufferedOut);

        if(oldArchive != null)
            move(oldArchive);
    }

    private void move(Archive archive){
        FFmpeg ffmpeg = new FFmpeg();
        ffmpeg.move(archive.getFileName());
    }

    @Override
    public void close() throws IOException {
        move(archive);
        archive = null;
        super.close();
    }
}
