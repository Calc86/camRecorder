package com.net.rtsp;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.omg.PortableServer.SERVANT_RETENTION_POLICY_ID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

 sprop-parameter-sets
 The sprop-parameter-sets is an optional parameter that encodes H.264 sequence parameter set (SPS) and picture parameter set (PPS) Network Adaptation Layer NAL) units. These parameter sets provide essential information necessary to decode an H.264 bitstream; encoding them in SDP ensures that they are delivered reliably.
 Not Used -  When the sprop-parameter-sets optional parameter is received in the incoming offer, it is discarded. The sprop-parameter-sets parameter is not displayed in the outgoing offer.
 Include Out-of-band DCI - When the sprop-parameter-sets optional parameter is received it must contain only parameter sets that conform to the Profile Level set above.
 a=fmtp:96 packetization-mode=1;profile-level-id=640028;sprop-parameter-sets=Z2QAKK2EBUViuKxUdCAqKxXFYqOhAVFYrisVHQgKisVxWKjoQFRWK4rFR0ICorFcVio6ECSFITk8nyfk/k/J8nm5s00IEkKQnJ5Pk/J/J+T5PNzZprQCgDLYCqQAAAMABAAAAwJZgQAAW42AABm/yve+F4RCNQ==,aO48sA==;
 a=fmtp:96 packetization-mode=1;profile-level-id=640028;sprop-parameter-sets=Z2QAKK2EBUViuKxUdCAqKxXFYqOhAVFYrisVHQgKisVxWKjoQFRWK4rFR0ICorFcVio6ECSFITk8nyfk/k/J8nm5s00IEkKQnJ5Pk/J/J+T5PNzZprQCgDLYCqQAAAMABAAAAwJZgQAAW42AABm/yve+F4RCNQ==,aO48sA==;

 */

public class SDP {
    private static Logger log = Logger.getLogger(SDP.class.getName());

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

    public class FMTP{
        String fmtp;

        byte[] sps = null;
        byte[] pps = null;

        Map<String, String> params = new HashMap<String, String>();

        public FMTP(String fmtp) {
            this.fmtp = fmtp;

            parse();
        }

        private void parse(){
            //String[] tmp1 = fmtp.split(";");

            for(String s : fmtp.split(";")){
                String[] ts = s.split("=", 2);
                if(ts.length < 2){
                    System.err.println("wrong fmtp param: " + s);
                    continue;
                }
                params.put(ts[0], ts[1]);
            }

            String sprop;
            if( (sprop = params.get("sprop-parameter-sets")) != null){
                //we have sprop-parameter-sets
                String[] props = sprop.split(",");
                if(props.length != 2){
                    System.err.println("wrong sprop-parameter-sets: " + sprop);
                }
                else{
                    try {
                        sps = Base64.decode(props[0].getBytes());
                        pps = Base64.decode(props[1].getBytes());
                    } catch (Base64DecodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public byte[] getSps() {
            return sps;
        }

        public byte[] getPps() {
            return pps;
        }
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
        //x-dimensions
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
                AttributeName name = null;

                name = AttributeName.valueOf(elem[0]);


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
                log.warning("Unknown a=<name>:... " + elem[0] + ", " + e1.getMessage());
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
