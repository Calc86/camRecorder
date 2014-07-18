package com.net.rtsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by calc on 15.07.14.
 *
 v=0
 o=- 0 0 IN IP4 10.113.151.152
 s=RTSP Session
 c=IN IP4 10.113.151.152
 m=video 0 RTP/AVP 96
 a=rtpmap:96 H264/90000
 a=control:/video/h264
 a=fmtp:96 profile-level-id=420028; packetization-mode=1;sprop-parameter-sets=Z0IAKOkAoAty,aM4xUg==
 m=audio 0 RTP/AVP 97
 a=rtpmap:97 PCMA/8000/1
 a=control:/audio/pcma

 http://ru.wikipedia.org/wiki/Session_Description_Protocol
 http://tools.ietf.org/html/rfc4566.html
 */

public class SDP {
    public static final String CONTENT_LENGTH = "Content-Length:";
    String packet;

    //Описание сеанса
    private int v;
    private Origin o;
    private String s;
    /*private String i; //*
    private String u; //*
    private String e; //*
    private String p; //**/
    private String c; //*
    /*private String b; //*
    private String z; //*
    private String k; //*
    private ArrayList<String> a; //**/

    //Описание параметров времени
    /*private String t;
    private String r; //**/

    private List<Media> m = new ArrayList<Media>();
    private Media currentMedia = null;

    public SDP(String packet) {
        this.packet = packet;

        parse();
    }

    public int getV() {
        return v;
    }

    public Origin getO() {
        return o;
    }

    public String getS() {
        return s;
    }

    public List<Media> getM() {
        return m;
    }

    public List<Media> getMediaByType(MediaType type){
        List<Media> list = new ArrayList<Media>();

        for(Media media: m){
            if(media.getMedia().equals(type))
                list.add(media);
        }

        return list;
    }

    protected void parse(){
        BufferedReader reader = new BufferedReader(new StringReader(packet));

        String line;
        try {
            while( (line = reader.readLine()) != null ){
                parseLine(line);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    protected void parseLine(String line){
        if(line.substring(0, 2).equals("v=")){
            v = Integer.parseInt(line.substring(2).trim());
        } else
        if(line.substring(0, 2).equals("o=")){
            o = new Origin(line.substring(2).trim());
        } else
        if(line.substring(0, 2).equals("s=")){
            s = line.substring(2).trim();
        } else
        if(line.substring(0, 2).equals("c=")){
            c = line.substring(2).trim();
        } else
        if(line.substring(0, 2).equals("m=")){
            currentMedia = new Media(line.substring(2).trim());
            m.add(currentMedia);
        } else
        if(line.substring(0, 2).equals("a=")){
            if(currentMedia != null){
                currentMedia.addAttributes(line.substring(2));
            }/*else
            {
                //hm... эти атрибуты пока не интересуют
            }*/
        }


    }

    public class Origin{
        private String line;

        private String username;
        private String sessId;
        private String sessVersion;
        private String nettype;
        private String addrtype;
        private String unicastAddress;

        public Origin(String line) {
            this.line = line;

            parse();
        }

        private void parse(){
            String[] elem = line.split(" ");

            if(elem.length < 6)
                System.err.println("SDP Origin length error: " + line + " " + " have " + elem.length + " elements");
            else{
                username = elem[0];
                sessId = elem[1];
                sessVersion = elem[2];
                nettype = elem[3];
                addrtype = elem[4];
                unicastAddress = elem[5];
            }
        }

        public String getUsername() {
            return username;
        }

        public String getSessId() {
            return sessId;
        }

        public String getSessVersion() {
            return sessVersion;
        }

        public String getNettype() {
            return nettype;
        }

        public String getAddrtype() {
            return addrtype;
        }

        public String getUnicastAddress() {
            return unicastAddress;
        }
    }

    public enum MediaType{
        audio,
        video,
        text,
        application,
        message
    }

    public enum AttributeName{
        cat,
        keywds,
        tool,
        ptime,
        maxptime,
        rtpmap,
        recvonly,
        sendrecv,
        sendonly,
        inactive,
        orient,
        type,
        charset,
        sdplang,
        lang,
        framerate,
        quality,
        control,
        fmtp
    }

    public class Media{
        private String line;

        private MediaType media;
        /**
         * may be <port>/<number of ports>
         */
        private String port;
        private String proto;
        private String fmt;

        /**
         * name, list of values
         */
        private Map<AttributeName, List<String>> a = new HashMap<AttributeName, List<String>>();

        public Media(String line) {
            this.line = line;

            parse();
        }

        public void addAttributes(String line){
            String[] elem = line.split(":", 2);

            try {
                AttributeName name = AttributeName.valueOf(elem[0]);

                if(elem.length >1 && a.containsKey(name)){
                    a.get(name).add(elem[1]);
                } else{
                    String value = "";
                    if(elem.length > 1) value = elem[1];
                    ArrayList<String> list = new ArrayList<String>();
                    list.add(value);
                    a.put(name, list);
                }
            } catch (IllegalArgumentException e1) {
                System.err.println("Unknown a=<name>:... " + elem[0]);
                e1.printStackTrace();
            }
        }

        public List<String> getAttribute(AttributeName name){
            return a.get(name);
        }

        private void parse(){
            String[] elem = line.split(" ");

            if(elem.length < 4)
                System.err.println("SDP Media length error: " + line + " " + " have " + elem.length + " elements");
            else{
                try {
                    media = MediaType.valueOf(elem[0]);
                } catch (IllegalArgumentException e1) {
                    System.err.println("Unknown media type: " + elem[0]);
                    e1.printStackTrace();
                }
                port = elem[1];
                proto = elem[2];
                fmt = elem[3];
            }
        }

        public MediaType getMedia() {
            return media;
        }

        public String getPort() {
            return port;
        }

        public String getProto() {
            return proto;
        }

        public String getFmt() {
            return fmt;
        }
    }
}
