package ru.xsrv.recorder.model;

import java.io.*;

/**
 * Created by calc on 25.07.14.
 *
 */
public class Settings implements Serializable {
    transient private static Settings instance = null;
    transient private static File file = new File("settings.dat");

    private String vlcPath = "";
    private String ffmpegPath = "";
    private boolean debug = true;
    private int seconds = 600;

    private String archivePath = "archive";
    private String tmpPath = "temp";
    private String recPath = "rec";

    public void mkDirs(){
        File f;

        f = new File(archivePath);
        if(f.exists())
            f.mkdir();

        f = new File(archivePath + "/" + tmpPath);
        if(!f.exists())
            f.mkdir();

        f = new File(archivePath + "/" + recPath);
        if(!f.exists())
            f.mkdir();
    }

    public synchronized static Settings getInstance(){
        try {
            if(instance == null) instance = load();
        } catch (IOException e) {
            instance = new Settings();
        }
        return instance;
    }

    public static Settings load() throws IOException {
        Settings settings = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            ObjectInputStream ois = new ObjectInputStream(fis);
            settings = (Settings)ois.readObject();

        } catch (FileNotFoundException e) {
            settings = new Settings();
        } catch (ClassNotFoundException e) {
            settings = new Settings();
        }

        settings.mkDirs();
        return settings;
    }

    public void save() throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(this);
        oos.flush();
        oos.close();
    }

    public String getVlcPath() {
        return vlcPath;
    }

    public void setVlcPath(String vlcPath) {
        this.vlcPath = vlcPath;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public String getTmpPath() {
        return tmpPath;
    }

    public void setTmpPath(String tmpPath) {
        this.tmpPath = tmpPath;
    }

    public String getRecPath() {
        return recPath;
    }

    public void setRecPath(String recPath) {
        this.recPath = recPath;
    }

    public String getFullTmpPath(){
        return getArchivePath() + "/" + getTmpPath() + "/";
    }

    public String getFullRecPath(){
        return getArchivePath() + "/" + getRecPath() + "/";
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
