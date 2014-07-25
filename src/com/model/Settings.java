package com.model;

import com.sun.org.apache.xerces.internal.dom.PSVIAttrNSImpl;

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
    private int seconds = 600;

    public synchronized static Settings getInstance(){
        try {
            if(instance == null) instance = load();
        } catch (IOException e) {
            instance = new Settings();
        }
        return instance;
    }

    public static Settings load() throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return new Settings();
        }

        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
            return (Settings)ois.readObject();
        } catch (ClassNotFoundException e) {
            return new Settings();
        }
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
}
