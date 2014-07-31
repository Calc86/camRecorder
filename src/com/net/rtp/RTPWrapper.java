package com.net.rtp;

import com.net.utils.BIT;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by calc on 15.07.14.
 *
 */
public class RTPWrapper implements IRaw {
    public static final int MAX_RTP_PACKET_SIZE = 65536;

    public static final byte TYPE_JPEG = 26;    //rfc2435
    //72-76	Reserved for RTCP conflict avoidance
    public static final byte TYPE_RTCP = 72;
    public static final byte TYPE_DYNAMIC_96 = 96;    //rfc3551
    public static final byte TYPE_H264 = TYPE_DYNAMIC_96;
    public static final int RTP_HEADER_SIZE = 12;   //96 bits

    private enum Flag{
        START,
        MIDDLE,
        END
    }

    protected Flag flag;

    private byte[] packet;
    private int length;

    //wrappers
    private H264RTP h264 = new H264RTP(this);

    public static byte[] createBuffer(){
        return new byte[MAX_RTP_PACKET_SIZE];
    }

    public RTPWrapper(byte[] buffer, int length){
        this.length = length;
        packet = buffer;
    }

    public void fill(InputStream in, int length) throws IOException {
        if(packet.length < length)
            throw new IllegalStateException("buffer size is " + packet.length + " needed + " + length);

        this.length = length;
        int readed = 0;
        while(readed < length){
            readed += in.read(packet, readed, length - readed);
        }
    }

    public RTPWrapper(RTPWrapper rtp){
        length = rtp.getLength();
        packet = rtp.getBuffer();
    }

    /*public RTPWrapper(){

    }*/

    /**
     *
     * @return класс, который обрабатывает определенный тип payload
     * @throws NotImplementedException
     */
    public RTPWrapper getRtpByPayload() throws NotImplementedException {
        switch (getPayloadType()){
            case RTPWrapper.TYPE_H264:
                //return new H264RTP(this);
                return null;
            /*case RTPWrapper.TYPE_JPEG:
                return new JpegRTP(this);
            case RTPWrapper.TYPE_RTCP:
                return new RTCP(this);*/
            default:
                throw new NotImplementedException("rtp type " + getPayloadType() + " not implemented");
        }
    }

    /**
     *
     * @return класс, который обрабатывает определенный тип payload
     * @throws NotImplementedException
     */
    public IRaw getByPayload() throws NotImplementedException {
        switch (getPayloadType()){
            case RTPWrapper.TYPE_H264:
                h264.setValues();
                return h264;
            /*case RTPWrapper.TYPE_JPEG:
                return new JpegRTP(this);
            case RTPWrapper.TYPE_RTCP:
                return new RTCP(this);*/
            default:
                throw new NotImplementedException("rtp type " + getPayloadType() + " not implemented");
        }
    }

    public void writeRawToStream(OutputStream out) throws IOException {
        System.err.println("NOTICE: Raw RTP payload write");
        System.out.println("type: " + getPayloadType());
        out.write(packet, getPayloadStart(), getPayloadLength());
    }

    public byte[] getBuffer() {
        return packet;
    }

    /**
     * version
     * @return byte
     */
    public byte getV(){
        return (byte)((packet[0] >>> 6) & BIT._2);
    }

    /**
     * padding, дополнение
     * @return
     */
    public boolean getP(){
        return (packet[0] & 0x20) != 0;
    }

    /**
     * Extension
     * @return
     */
    public boolean getX(){
        return (packet[0] & 0x10) != 0;
    }

    /**
     * Количество CSRC идентификаторов
     * @return
     */
    public byte getCC(){
        return (byte)((packet[0]) & BIT._4);
    }

    /**
     * Маркер. Указывает на то, что этот пакет последний (в цепочке)
     * @return
     */
    public boolean getMarker(){
        return (packet[1] & 0x80) != 0;
    }

    /**
     * Формат payload, Payload type
     * http://www.iana.org/assignments/rtp-parameters/rtp-parameters.xhtml
     * @return
     */
    public byte getPayloadType(){
        return (byte)((packet[1]) & BIT._7);
    }

    /**
     * Sequence number, номер пакета, нужно для обнаружения потери пакетов
     * @return
     */
    public int getSequence(){
        return BIT.makeShort(packet, 2);
    }

    /**
     * @return 90khz timestamp
     */
    public int getTimestamp(){
        return BIT.makeInt(packet, 4);
    }

    /**
     * идентификатор синхронизатора, Определяет источник потока
     * @return SSRC
     */
    public int getSSRC() {
        return BIT.makeInt(packet, 8);
    }

    /**
     * Возвращает длину CSRC заголовков
     * @return length
     */
    /*public int getCSRCLength(){
        return 4 * getCC();
    }*/

    /*public int getExtensionHeaderStart(){
        return 12 + getCSRCLength();
    }*/

    /*public int getExtensionHeaderLength(){
        return 4;   //32 bits
    }*/

    public int getPayloadStart(){
        return RTP_HEADER_SIZE + getCC() * 4;
        //должен быть еще X, Extension Header (Length, etc)
    }

    public int getPayloadLength(){
        return length - getPayloadStart();
    }

    public void print(){
        System.out.println("|V |P|X|CC  |M|PT     |Seq             |");
        //                   v    p  x  cc  m  pt  seq
        System.out.printf("|%2d|%d|%d|%4d|%d|%7d|%16d|\n",
                getV(),
                getP() ? 1 : 0,
                getX() ? 1 : 0,
                getCC(),
                getMarker() ? 1 : 0,
                getPayloadType(),
                getSequence()
        );
        System.out.printf("|%32s|\n", "   timestamp");
        System.out.printf("|%32x|\n", getTimestamp());
        System.out.printf("|%32s|\n", "   SSRC Identifier");
        System.out.printf("|%32x|\n", getSSRC());
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
