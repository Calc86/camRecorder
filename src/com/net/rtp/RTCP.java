package com.net.rtp;

import com.net.utils.BIT;

/**
 * Created by calc on 17.07.14.
 * http://tools.ietf.org/html/rfc3550#page-19
 */
public class RTCP extends RTP {
    public static final int TYPE_SENDER_REPORT = 200;
    public static final int TYPE_RECEIVER_REPORT = 201;
    public static final int TYPE_SOURCE_DESCRIPTION = 202;
    public static final int TYPE_GOODBYE = 203;

    public static final int HEADER_LENGTH = 8;  //header + SSRC

    public RTCP(RTP rtp) {
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

    @Override
    public int getLength(){
        //return BIT.makeShort(buffer, 2);
        //same bytes
        return getSequence();
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
     The total number of RTP data packets transmitted by the sender
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
     padding) transmitted in RTP data packets by the sender since
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
                getLength()
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
        return BIT.makeInt(getBuffer(), 4);
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
}
