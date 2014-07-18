package com.net.rtp;

import com.net.utils.BIT;

/**
 * Created by calc on 17.07.14.
 * http://tools.ietf.org/html/rfc3550#page-19
 */
public class RTCP {
    public static final int HEADER_LENGTH = 8;

    byte[] buffer;
    int length;

    public RTCP(byte[] buffer, int length) {
        this.buffer = buffer;
        this.length = length;
    }

    public byte getV(){
        return (byte)((buffer[0] >>> 6) & BIT._2);
    }

    public boolean getP(){
        return (buffer[0] & BIT.RFC_P2) != 0;
    }

    public byte getRC(){
        return (byte)(buffer[0] & BIT._5);
    }

    public int getPT(){
        return buffer[1] & BIT._8;
    }

    public int getSP(){
        return getPT();
    }

    public int getLength(){
        return BIT.makeShort(buffer, 2);
    }

    public int getSSRC(){
        return BIT.makeInt(buffer, 4);
    }
}
