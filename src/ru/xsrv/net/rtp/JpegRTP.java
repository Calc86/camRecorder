package ru.xsrv.net.rtp;

import ru.xsrv.net.jpeg.JpegRfc2345;
import ru.xsrv.net.utils.BIT;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * Created by calc on 16.07.14.
 * http://tools.ietf.org/html/rfc2435
 *
 */
public class JpegRTP implements IRaw {
    private static Logger log = Logger.getLogger("main");

    public static final int JPEG_HEADER_SIZE = 8;
    public static final int JPEG_RESTART_MARKER_HEADER_SIZE = 4;
    public static final int JPEG_QUANTIZATION_TABLE_HEADER_SIZE = 4;

    private RTPWrapper rtp;

    public JpegRTP(RTPWrapper rtp) {
        this.rtp = rtp;
    }

    public byte getTypeSpecific(){
        //3.1.  JPEG header
        return rtp.getBuffer()[rtp.getPayloadStart()];
    }

    public int getFragmentOffset(){
        //3.1.  JPEG header
        return 0;
    }

    public byte getType(){
        //3.1.  JPEG header
        return rtp.getBuffer()[rtp.getPayloadStart() + 4];
    }

    public byte getQuality(){
        //3.1.  JPEG header
        return rtp.getBuffer()[rtp.getPayloadStart() + 5];
    }

    public byte getWidth(){
        //3.1.  JPEG header
        return rtp.getBuffer()[rtp.getPayloadStart() + 6];
    }

    public byte getHeight(){
        //3.1.  JPEG header
        return rtp.getBuffer()[rtp.getPayloadStart() + 7];
    }

    /**
     * get mbz
     * 3.1.8.  Quantization Table header
     * @return
     */
    public boolean isMustBeZero(){
        //костыль
        //return getBuffer()[getPayloadStart() + 8] == 0 && getBuffer()[getPayloadStart() + 9] == 0;
        //return getBuffer()[getPayloadStart() + getJpegQuantizationTableOffset()] == 0 && getQPrecision() == 0;
        return rtp.getBuffer()[rtp.getPayloadStart() + getJpegQuantizationTableOffset()] == 0;
    }

    public byte getQPrecision(){
        //3.1.8.  Quantization Table header
        return rtp.getBuffer()[rtp.getPayloadStart() + getJpegQuantizationTableOffset() + 1];
    }

    public int getQLength(){
        //int l = BIT.makeShort(getBuffer(), getPayloadStart() + 10);
        //3.1.8.  Quantization Table header
        int l = BIT.makeShort(rtp.getBuffer(), rtp.getPayloadStart() + getJpegQuantizationTableOffset() + 2);
        //if(l != 128)
        //    throw new NotImplementedException("jpeg quantization table length not equal 128: " + l);

        return l;
    }

    byte[] tbl1 = new byte[64];
    byte[] tbl2 = new byte[64];

    public byte[] getLqtTable(){
        //System.arraycopy(getBuffer(), getPayloadStart() + JPEG_HEADER_SIZE + JPEG_QUANTIZATION_TABLE_HEADER_SIZE, tbl, 0, 64);
        System.arraycopy(rtp.getBuffer(), rtp.getPayloadStart() + getJpegQuantizationTableOffset() + JPEG_QUANTIZATION_TABLE_HEADER_SIZE, tbl1, 0, 64);

        return tbl1;
    }

    public byte[] getCqtTable(){

        System.arraycopy(rtp.getBuffer(), rtp.getPayloadStart() + getJpegQuantizationTableOffset() + JPEG_QUANTIZATION_TABLE_HEADER_SIZE + 64, tbl2, 0, 64);

        return tbl2;
    }

    public int getJpegQuantizationTableOffset(){
        if(getType() < 64){
            return JPEG_HEADER_SIZE;
        } else if(getType() < 128){
            //we have 3.1.7.  Restart Marker header
            return JPEG_HEADER_SIZE + JPEG_RESTART_MARKER_HEADER_SIZE;
        } else {
            throw new NotImplementedException("jpeg type: " + getType() + " not implemented");
        }
    }

    public int getJpegPayloadOffset(){
        //if(isMustBeZero() && !getMarker())
        if(isStart())
            return getJpegQuantizationTableOffset() + JPEG_QUANTIZATION_TABLE_HEADER_SIZE + getQLength();
        else
            return getJpegQuantizationTableOffset();
    }

    public int getJPEGPayloadStart() throws NotImplementedException {
        if(getType() < 64){
            return rtp.getPayloadStart() + getJpegPayloadOffset();
        }else
        if(getType() < 128){
            //we have 3.1.7.  Restart Marker header
            return rtp.getPayloadStart() + getJpegPayloadOffset();
        }
        else{
            throw new NotImplementedException("jpeg type: " + getType() + " not implemented");
        }
    }

    public int getJPEGPayloadLength() throws NotImplementedException {
        if(getType() < 64){
            return rtp.getPayloadLength() - getJpegPayloadOffset();
        }
        else if(getType() < 128){
            //we have 3.1.7.  Restart Marker header
            return rtp.getPayloadLength() - getJpegPayloadOffset();
        }
        else{
            throw new NotImplementedException("jpeg type: " + getType() + " not implemented");
        }
    }

    protected boolean isStart(){
        return isMustBeZero() && getQPrecision() == 0 && getQLength() == 128;
    }

    protected boolean isEnd(){
        return rtp.getMarker();
    }

    public void writeRawJPEGtoStream(OutputStream out) throws IOException {
        //if(isMustBeZero()){
        if(isStart()){
            //first
            //System.out.println("first");
            byte[] headers = new byte[1024];
            int length = makeJpeg(headers);
            out.write(headers, 0, length);
            out.write(rtp.getBuffer(), getJPEGPayloadStart(), getJPEGPayloadLength());
        }else
        //if(getMarker()){
        if(isEnd()){
            //end
            //System.out.println("end");
            out.write(rtp.getBuffer(), getJPEGPayloadStart(), getJPEGPayloadLength());
            //EOI
        } else {
          //middle
            //System.out.println("middle");
            out.write(rtp.getBuffer(), getJPEGPayloadStart(), getJPEGPayloadLength());
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
