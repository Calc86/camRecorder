package com.net.rtp.rfc;

/**
 * Created by calc on 29.07.14.
 */
public enum rtcp_sdes_type_t{
    RTCP_SDES_END   (0),
    RTCP_SDES_CNAME (1),
    RTCP_SDES_NAME  (2),
    RTCP_SDES_EMAIL (3),
    RTCP_SDES_PHONE (4),
    RTCP_SDES_LOC   (5),
    RTCP_SDES_TOOL  (6),
    RTCP_SDES_NOTE  (7),
    RTCP_SDES_PRIV  (8);

    private final int type;

    rtcp_sdes_type_t(int type) {
        this.type = type;
    }
}
