package ru.xsrv.net.rtp.rfc;

/**
 * Created by calc on 29.07.14.
 *
 */
public enum rtcp_type_t{
    RTCP_SR   (200),
    RTCP_RR   (201),
    RTCP_SDES (202),
    RTCP_BYE  (203),
    RTCP_APP  (204);

    private final int type;
    rtcp_type_t(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
