package com.net.rtp.rfc;

import com.net.utils.BIT;

/**
 * Created by calc on 29.07.14.
 * Reception report block
 */
public class rtcp_rr_t{
    public static final int SIZE = 24;
    byte[] block = new byte[SIZE];

    public int ssrc     (){ return getSSRC(); }
    public int fraction (){ return getFraction(); }
    public int lost     (){ return getLost(); }
    public int last_seq (){ return getLastSequence(); }
    public int jitter   (){ return getInternalJitter(); }
    public int lsr      (){ return getLastSRPacket(); }
    public int dlsr     (){ return getDelaySinceLastSRPacket(); }

    public int getSSRC(){
        return BIT.makeInt(block, 0);
    }

    public int getFraction(){
        return block[4] & BIT._8;
    }

    public int getLost(){
        return BIT.makeInt(block, 4) & BIT._24;
    }

    public int getLastSequence(){
        return BIT.makeInt(block, 8);
    }

    public int getInternalJitter(){
        return BIT.makeInt(block, 12);
    }

    public int getLastSRPacket(){
        return BIT.makeInt(block, 16);
    }

    public int getDelaySinceLastSRPacket(){
        return BIT.makeInt(block, 20);
    }
}
