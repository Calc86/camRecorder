package ru.xsrv.net.rtp.rfc;

import ru.xsrv.net.utils.BIT;

/**
 * Created by calc on 29.07.14.
 * RTCP common header word
 */
public class rtcp_common_t{
    public static final int SIZE = 4;
    byte[] header = new byte[SIZE];

    public byte[] getHeader(){
        return header;
    }

    public byte    v     (){ return getVersion(); }
    public boolean p     (){ return getPaddingFlag(); }
    public int     count (){ return getCount(); }
    public int     pt    (){ return getPayloadType(); }
    public int     length(){ return getLength();}

    public byte getVersion(){
        return (byte)((header[0] >>> 6) & BIT._2);
    }

    public boolean getPaddingFlag(){
        return (header[0] & 0x20) != 0;
    }

    public int getCount(){
        return (byte)((header[0]) & BIT._5);
    }

    public int getPayloadType(){
        return (byte)((header[1]) & BIT._8);
    }

    public int getLength(){
        return BIT.makeShort(header, 2);
    }
}
