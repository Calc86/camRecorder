package com.net.h264;

/**
 * Created by calc on 16.07.14.
 *
 */
public class NAL {
    public static final int FU_A = 28;
    public static final int SPS = 7;
    public static final int PPS = 8;

    byte nal;

    public NAL(byte nal) {
        this.nal = nal;
    }

    public byte getF(){
        return (byte)((nal >>> 8) & 0x1);
    }

    public byte getNRI(){
        return (byte)((nal >>> 5) & 0x3);
    }

    public byte getType(){
        return (byte)(nal & 0x1f);
    }

    public void print(){
        System.out.printf("|f|NRI type|\n");
        System.out.printf("|%d|%2d|%5d|\n", getF(), getNRI(), getType());
    }
}
