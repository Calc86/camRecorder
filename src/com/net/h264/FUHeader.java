package com.net.h264;

/**
 * Created by calc on 16.07.14.
 *
 */
public class FUHeader {
    private byte fu;

    public FUHeader(byte fu) {
        this.fu = fu;
    }

    public boolean isFirst(){
        int f = (fu & 0x80);
        //System.out.printf("F: %x\n", f);
        return f !=0;
    }

    public boolean isEnd(){
        int e = (fu & 0x40);
        //System.out.printf("E: %x\n", e);
        return e != 0;
    }

    public byte getType(){
        return (byte)(fu & 0x1f);
    }

    public void print(){
        //System.out.printf("FU: %x\n", fu);
    }
}
