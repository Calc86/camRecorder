package com.net.rtp.rfc;

/**
 * Created by calc on 29.07.14.
 */
public class Constants {
    public static final byte RTP_VERSION = 2;
    public static final int RTP_SEQ_MOD = (1<<16);
    public static final int RTP_MAX_SDES = 255;

    /*
    * Big-endian mask for version, padding bit and packet type pair
    */
    public static final int RTCP_VALID_MASK = (0xc000 | 0x2000 | 0xfe);
    public static final int RTCP_VALID_VALUE = ((RTP_VERSION << 14) | rtcp_type_t.RTCP_SR.getType());
}
