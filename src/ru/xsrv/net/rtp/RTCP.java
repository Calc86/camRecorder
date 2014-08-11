package ru.xsrv.net.rtp;

import ru.xsrv.net.utils.BIT;

/**
 * Created by calc on 17.07.14.
 * http://tools.ietf.org/html/rfc3550#page-19
 *
 * From wireshark:
 * RTCP senders report (200)
 *  01234567 01234567 01234567 01234567
 * +--------+--------+--------+--------+
 * |V P RC  | type201| length (octets) |
 * v-2bit
 * p-1bit
 *  01234567 01234567 01234567 01234567
 * +--------+--------+--------+--------+
 *   SSRC
 *  NTP timestamp MSW
 *  NTP timestamp LSW
 *  RTPWrapper timestamp
 *  senders packet count
 *  senders octet count
 *
 *  RTCP source description (202)
 * +--------+--------+--------+--------+
 * |V P SC  | type202| length          |
 * chunks:
 * Chunk 1:
 * +--------+--------+--------+--------+
 *   Identifier (SSRC?)
 * SDES:
 * +--------+--------+
 *  type     length    Text... End(0)
 *
 *
 * +--------+--------+--------+--------+
 * |V P RC  | type201| length (octets) |
 *  ssrc
 *  Sources:
 *  +-Source1:
 *    +-Identifier (4bytes)
 *    +-SSRC content
 *      + fraction lost 1byte
 *      + Cumulative number of packets lost: -1 (3 bytes)
 *    + extended highest sequence number received
 *      + cycle 2b
 *      + number 2b
 *    + jitter 4b
 *    + LSR (middle of NTP timestamp)
 *    + Delay since last SR 4b
 *
 *
 *
 */
public class RTCP extends RTPWrapper {
    public static final int TYPE_SENDER_REPORT = 200;
    public static final int TYPE_RECEIVER_REPORT = 201;
    public static final int TYPE_SOURCE_DESCRIPTION = 202;
    public static final int TYPE_GOODBYE = 203;

    private static byte[] ssrc = {'c', 'a', 'l', 'c'};
    private static byte[] sdes = {'c', 'a', 'l', 'c', '-', '2', '3'};

    public static final int HEADER_LENGTH = 8;  //header + SSRC

    public RTCP(RTPWrapper rtp) {
        super(rtp);
    }

    public RTCP(byte[] buffer, int length) {
        super(buffer, length);
    }

    /**
     * reception report count (RC): 5 bits
     * The number of reception report blocks contained in this packet.  A
     * value of zero is valid.
     * @return byte
     */
    public byte getRC(){
        return (byte)(getBuffer()[0] & BIT._5);
    }

    public int getPT(){
        return getBuffer()[1] & BIT._8; //to unsigned
    }

    public void setPT(int type){
        getBuffer()[1] = (byte)(type & BIT._8);
    }

    public int getRTCPLength(){
        //return BIT.makeShort(buffer, 2);
        //same bytes
        return fromOctetCount(getSequence());
    }

    /**
     * return (octet+1) * 8
     * @param octet
     * @return
     */
    protected int fromOctetCount(int octet){
        return (octet + 1) * 4;
    }

    public int getHiNTPTimestamp(){
        return BIT.makeInt(getBuffer(), HEADER_LENGTH  + 0 * 4);
    }

    public int getLowNTPTimestamp(){
        return BIT.makeInt(getBuffer(), HEADER_LENGTH + 1 * 4);
    }

    public int getRTPTimestamp(){
        return BIT.makeInt(getBuffer(), HEADER_LENGTH + 2 * 4);
    }

    /**
     * sender's packet count: 32 bits
     The total number of RTPWrapper data packets transmitted by the sender
     since starting transmission up until the time this SR packet was
     generated.  The count SHOULD be reset if the sender changes its
     SSRC identifier.
     * @return
     */
    public int getSendersPacketCount(){
        return BIT.makeInt(getBuffer(), HEADER_LENGTH + 3 * 4);
    }

    /**
     *  sender's octet count: 32 bits
     The total number of payload octets (i.e., not including header or
     padding) transmitted in RTPWrapper data packets by the sender since
     starting transmission up until the time this SR packet was
     generated.  The count SHOULD be reset if the sender changes its
     SSRC identifier.  This field can be used to estimate the average
     payload data rate.
     * @return
     */
    public int getSendersOctetCount(){
        return BIT.makeInt(getBuffer(), HEADER_LENGTH + 4 * 4);
    }

    @Override
    public void print() {
        System.out.println("|V |P| RC   | PT     |Length          |");
        //                   v    p  x  cc  m  pt  seq
        System.out.printf("|%2d|%d|%5d|%8d|%16d|\n",
                getV(),
                getP() ? 1 : 0,
                getRC(),
                getPT(),
                getRTCPLength()
        );
        System.out.printf("|%32x|\n", getSSRC());
        System.out.printf("|%32x|\n", getHiNTPTimestamp());
        System.out.printf("|%32x|\n", getLowNTPTimestamp());
        System.out.printf("|%32x|\n", getRTPTimestamp());
        System.out.printf("|%32x|\n", getSendersPacketCount());
        System.out.printf("|%32x|\n", getSendersOctetCount());
    }

    @Override
    public int getSSRC() {
        return BIT.makeInt(getBuffer(), getSSRCStart());
    }

    public int getSSRCStart(){
        return 4;
    }

    public boolean isHaveNextRTCP(){
        return getLength() > getRTCPLength();
    }

    public RTCP getNextRTCP(){
        //return new RTCP();

        if(!isHaveNextRTCP()) return null;

        byte[] rtcpBuffer = new byte[getLength() - getRTCPLength()];

        System.arraycopy(getBuffer(), getRTCPLength(), rtcpBuffer, 0, rtcpBuffer.length);

        return new RTCP(rtcpBuffer, rtcpBuffer.length);
    }

    public class ReportBlock{
        int num = 0;

        private int startPosition(){
            return HEADER_LENGTH;
        }

        public ReportBlock(int num) {
            this.num = num;

        }

        /*public void setSSRC(int SSRC){
            getBuffer()[startPosition()] = BIT.HiByte(BIT.HiWord(SSRC));
            getBuffer()[startPosition() + 1] = BIT.LoByte(BIT.HiWord(SSRC));
            getBuffer()[startPosition() + 2] = BIT.HiByte(BIT.LoWord(SSRC));
            getBuffer()[startPosition() + 3] = BIT.LoByte(BIT.LoWord(SSRC));
        }*/
    }

    public void justCopy(){
        getBuffer()[0] = (byte)0x81;  //v2, p=0, RC=1
        getBuffer()[1] = (byte)0xC9;  //201
        getBuffer()[2] = 0x00;  //
        getBuffer()[3] = 0x07;  // 8*4-1

        //copy SSRC
        for (int i = 4; i <=7 ; i++) {
            getBuffer()[i+4] = getBuffer()[i];
        }

        for (int i = 12; i < 32; i++) {
            getBuffer()[i] = 0;
        }

        String text = "just text header";

        for (int i = 32; i < 48 ; i++) {
            getBuffer()[i] = text.getBytes()[i-32];
        }

        for (int i = 48; i <=51 ; i++) {
            getBuffer()[i] = 0;
        }
    }

    //вынести source наружу
    public static byte[] response201(RTCP rtcp, int loop, int seq, int channel, long last, int jitter){
        byte[] buffer = new byte[32];

        int i = 0;

        //header
        buffer[i++] = (byte)0x81;   //1000 0001
        buffer[i++] = (byte)RTCP.TYPE_RECEIVER_REPORT;
        buffer[i++] = 0; buffer[i++] = 7;   // 32/4-1

        //my ssrc
        System.arraycopy(ssrc, 0, buffer, i, ssrc.length);
        i += ssrc.length;
        buffer[i-1] = (byte)channel;

        //source 1
        System.arraycopy(rtcp.getBuffer(), rtcp.getSSRCStart(), buffer, i, 4);
        i += 4;

        //fraction lost
        buffer[i++] = (byte)0x00;
        //Cumulative number of packets lost: -1
        /*buffer[i++] = (byte)0xff;
        buffer[i++] = (byte)0xff;
        buffer[i++] = (byte)0xff;*/
        buffer[i++] = 0;
        buffer[i++] = 0;
        buffer[i++] = 0;

        //Extended highest sequence number received:
        buffer[i++] = BIT.HiByte(BIT.LoWord(loop));
        buffer[i++] = BIT.LoByte(BIT.LoWord(loop));
        buffer[i++] = BIT.HiByte(BIT.LoWord(seq));
        buffer[i++] = BIT.LoByte(BIT.LoWord(seq));

        //Interarrival jitter:
        buffer[i++] = BIT.HiByte(BIT.HiWord(jitter));
        buffer[i++] = BIT.LoByte(BIT.HiWord(jitter));
        buffer[i++] = BIT.HiByte(BIT.LoWord(jitter));
        buffer[i++] = BIT.LoByte(BIT.LoWord(jitter));

        //Last SR timestamp: 3810671619 (0xe3223c03)
        buffer[i++] = BIT.HiByte(BIT.LoWord(rtcp.getHiNTPTimestamp()));
        buffer[i++] = BIT.LoByte(BIT.LoWord(rtcp.getHiNTPTimestamp()));
        buffer[i++] = BIT.HiByte(BIT.HiWord(rtcp.getLowNTPTimestamp()));
        buffer[i++] = BIT.LoByte(BIT.HiWord(rtcp.getLowNTPTimestamp()));

        //Delay since last SR timestamp: 71531 (1091 milliseconds)
        //1/65536
        long now = System.currentTimeMillis();
        int range = (int)(((double)(now - last)/1000) * 65536);

        buffer[i++] = BIT.HiByte(BIT.HiWord(range));
        buffer[i++] = BIT.LoByte(BIT.HiWord(range));
        buffer[i++] = BIT.HiByte(BIT.LoWord(range));
        buffer[i++] = BIT.LoByte(BIT.LoWord(range));
        /*buffer[i++] = 0;
        buffer[i++] = 0;
        buffer[i++] = 0x06;
        buffer[i++] = 0x68;*/

        return buffer;
    }

    public static byte[] response202(RTCP rtcp, int channel){
        byte[] buffer = new byte[20];

        int i = 0;

        //header
        buffer[i++] = (byte)0x81;   //1000 0001
        buffer[i++] = (byte)RTCP.TYPE_SOURCE_DESCRIPTION;
        buffer[i++] = 0; buffer[i++] = 4;   // 32/4-1

        // chunk 1
        System.arraycopy(ssrc, 0, buffer, i, ssrc.length);
        i += ssrc.length;
        buffer[i-1] = (byte)channel;

        //sdes
        buffer[i++] = 0x01; //Text
        buffer[i++] = (byte)sdes.length;

        //text
        System.arraycopy(sdes, 0, buffer, i, sdes.length);
        i += sdes.length;

        buffer[i++] = 0;    //end(0)


        return buffer;
    }
}
