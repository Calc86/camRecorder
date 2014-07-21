package com.net.rtp;

import com.net.jpeg.JpegRfc2345;
import com.net.utils.BIT;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by calc on 16.07.14.
 *
 */
public class JpegRTP extends RTP {

    public static final int JPEG_HEADER_SIZE = 8;
    public static final int JPEG_Q_TABLE_SIZE = 4;

    public JpegRTP(RTP rtp) {
        super(rtp);
    }

    public byte getTypeSpecific(){
        return getBuffer()[getPayloadStart()];
    }

    public int getFragmentOffset(){
        return 0;
    }

    public byte getType(){
        return getBuffer()[getPayloadStart() + 4];
    }

    public byte getQuality(){
        return getBuffer()[getPayloadStart() + 5];
    }

    public byte getWidth(){
        return getBuffer()[getPayloadStart() + 6];
    }

    public byte getHeight(){
        return getBuffer()[getPayloadStart() + 7];
    }

    /**
     * get mbz
     * @return
     */
    public boolean isMustBeZero(){
        //костыль
        return getBuffer()[getPayloadStart() + 8] == 0 && getBuffer()[getPayloadStart() + 9] == 0;
    }

    public byte getQPrecision(){
        return getBuffer()[getPayloadStart() + 9];
    }

    public int getQLength(){
        int l = BIT.makeShort(getBuffer(), getPayloadStart() + 10);
        if(l != 128){
            System.out.println(l);
        }

        return l;
    }

    public byte[] getLqtTable(){
        byte[] tbl = new byte[64];
        System.arraycopy(getBuffer(), getPayloadStart() + JPEG_HEADER_SIZE + JPEG_Q_TABLE_SIZE, tbl, 0, 64);

        return tbl;
    }

    public byte[] getCqtTable(){
        byte[] tbl = new byte[64];
        System.arraycopy(getBuffer(), getPayloadStart() + JPEG_HEADER_SIZE + JPEG_Q_TABLE_SIZE + 64, tbl, 0, 64);

        return tbl;
    }

    public int getJpegPayloadOffset(){
        if(isMustBeZero() && !getMarker())
            return JPEG_HEADER_SIZE + JPEG_Q_TABLE_SIZE + getQLength();
        else
            return JPEG_HEADER_SIZE;
    }

    public int getJPEGPayloadStart() throws NotImplementedException {
        if(getType() < 64){
            return getPayloadStart() + getJpegPayloadOffset();
        }
        else{
            throw new NotImplementedException("jpeg type: " + getType() + " not implemented");
        }
    }

    public int getJPEGPayloadLength() throws NotImplementedException {
        if(getType() < 64){
            return getPayloadLength() - getJpegPayloadOffset();
        }
        else{
            throw new NotImplementedException("jpeg type: " + getType() + " not implemented");
        }
    }

    public void writeRawJPEGtoStream(OutputStream out) throws IOException {
        if(isMustBeZero()){
            //first
            byte[] headers = new byte[1024];
            int length = makeJpeg(headers);
            out.write(headers, 0, length);
            out.write(getBuffer(), getJPEGPayloadStart(), getJPEGPayloadLength());
        }else
        if(getMarker()){
            //end
            out.write(getBuffer(), getJPEGPayloadStart(), getJPEGPayloadLength());
            //EOI
        } else {
          //middle
            out.write(getBuffer(), getJPEGPayloadStart(), getJPEGPayloadLength());
        }
    }

    public int makeJpeg(byte[] h){
        /*byte[] lqt = new byte[64];
        byte[] cqt = new byte[64];*/
        byte[] lqt = getLqtTable();
        byte[] cqt = getCqtTable();

        int dri = 0;

        JpegRfc2345 rfc = new JpegRfc2345();

        //jpeg.MakeTables(getQuality() & 0xFF, lqt, cqt);

        int l = rfc.MakeHeaders(h, 0, getType(), getWidth(), getHeight(), lqt, cqt, dri);

        return l;
    }

    @Override
    public void writeRawToStream(OutputStream out) throws IOException {
        writeRawJPEGtoStream(out);
    }
}
