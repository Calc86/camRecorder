package com.net.rtp.rfc;

import com.net.utils.BIT;

/**
 * Created by calc on 29.07.14.
 *
 */
public class rtp_hdr_t{
    public static final int SIZE = 16;
    byte[] header = new byte[SIZE];

    public byte    v   (){ return getVersion(); }
    public boolean p   (){ return getPaddingFlag(); }
    public boolean x   (){ return getHeaderExtensionFlag(); }
    public int     cc  (){ return getCSRCCount(); }
    public boolean m   (){ return getMarkerBit(); }
    public int     pt  (){ return getPayloadType(); }
    public int     seq (){ return getSequenceNumber(); }
    public int     ts  (){ return getTimestamp();}
    public int     ssrc(){ return getSSRC();}
    //csrc optional

    public byte getVersion(){
        return (byte)((header[0] >>> 6) & BIT._2);
    }

    public boolean getPaddingFlag(){
        return (header[0] & 0x20) != 0;
    }

    public boolean getHeaderExtensionFlag(){
        return (header[0] & 0x10) != 0;
    }

    public int getCSRCCount(){
        return (byte)((header[0]) & BIT._4);
    }

    public boolean getMarkerBit(){
        return (header[1] & 0x80) != 0;
    }

    public int getPayloadType(){
        return (byte)((header[1]) & BIT._7);
    }

    public int getSequenceNumber(){
        return BIT.makeShort(header, 2);
    }

    public int getTimestamp(){
        return BIT.makeInt(header, 4);
    }

    public int getSSRC() {
        return BIT.makeInt(header, 8);
    }
}
