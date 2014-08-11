package ru.xsrv.net.rtsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by calc on 14.07.14.
 *
 */
public class Reply {
    public static final String SESSION = "Session:";
    public static final String PROTOCOL = "RTSP/1.0";
    private String reply;

    protected int code;   //first line
    protected int status;
    protected int sequence;
    protected String userAgent;
    protected String session;
    protected String transport;

    public Reply(String reply) {
        this.reply = reply;

        parse();
    }

    final protected void parse(){
        //StringReader reader;
        BufferedReader reader = new BufferedReader(new StringReader(reply));

        String line;
        try {
            while( (line = reader.readLine()) != null ){
                parseLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void parseLine(String line){
        if(line.contains(SESSION)){
            //Session: 5a8f46bcc635ade2;timeout=60
            String[] s = line.substring(SESSION.length()).trim().split(";", 2);
            session = s[0];
        } else
        if(line.contains(PROTOCOL)){
            String[] s = line.substring(PROTOCOL.length()).trim().split(" ", 2);
            try {
                code = Integer.parseInt(s[0]);
            } catch (NumberFormatException e) {
                System.err.println("Не правильный ответ, line: " + line);
                e.printStackTrace();
                code = -1;
            }
        }
    }

    //Getters

    public String getSession() {
        return session;
    }

    public int getCode() {
        return code;
    }

    public String getReply() {
        return reply;
    }
}
