package com.net.rtp;

import com.net.h264.FUHeader;
import com.net.h264.NAL;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by calc on 16.07.14.
 *
 */
//need to check packetization-mode
// (a=fmtp:96 profile-level-id=420028; packetization-mode=1;sprop-parameter-sets=Z0IAKOkAoAty,aM4xUg==)
public class H264RTP extends RTP {
    private NAL nal;
    private FUHeader FUHeader;
    //http://ip.hhi.de/imagecom_G1/assets/pdfs/h264_iso-iec_14496-10.pdf
    public static final byte[] NON_IDR_PICTURE = {0x00, 0x00, 0x00, 0x01};

    private boolean debug = true;

    /*public H264RTP(InputStream in, byte[] buffer, int length) throws IOException {
        super(in, buffer, length);

        setValues();
    }*/

    public H264RTP(RTP rtp) {
        super(rtp);

        setValues();

        //if(debug) print();
    }

    private void setValues(){
        nal = new NAL(getBuffer()[super.getPayloadStart()]);
        FUHeader = new FUHeader(getBuffer()[super.getPayloadStart() + 1]);
    }

    public NAL getNAL(){
        return nal;
    }

    public FUHeader getFUHeader(){
        return FUHeader;
    }

    public int getH264PayloadStart() {
        switch(getNAL().getType()){
            case NAL.FU_A:
                return super.getPayloadStart() + 2;
            case NAL.SPS:
            case NAL.PPS:
                return super.getPayloadStart();
            default:
                throw new NotImplementedException("NAL type " + getNAL().getType() + " not implemented");
        }
    }

    public int getH264PayloadLength() {
        switch(getNAL().getType()){
            case NAL.FU_A:
                return super.getPayloadLength() - 2;
            case NAL.SPS:
            case NAL.PPS:
                return super.getPayloadLength();
            default:
                throw new NotImplementedException("NAL type " + getNAL().getType() + " not implemented");
        }
    }

    public byte getReconstructedNal(){
        byte nal = (byte)(getBuffer()[super.getPayloadStart()] & 0xE0);
        nal += getFUHeader().getType();

        return nal;
    }

    public void writeRawH264toStream(OutputStream out) throws IOException, NotImplementedException {
        switch (nal.getType()){
            case NAL.FU_A:    //FU-A, 5.8.  Fragmentation Units (FUs)/rfc6184
                FUHeader fu = getFUHeader();

                if(fu.isFirst()){
                    //if(debug) System.out.println("first");
                    out.write(H264RTP.NON_IDR_PICTURE);
                    out.write(getReconstructedNal());
                    out.write(getBuffer(), getH264PayloadStart(), getH264PayloadLength());
                } else if(fu.isEnd()){
                    //if(debug) System.out.println("end");
                    out.write(getBuffer(), getH264PayloadStart(), getH264PayloadLength());
                } else{
                    //if(debug) System.out.println("middle");
                    out.write(getBuffer(), getH264PayloadStart(), getH264PayloadLength());
                }
                break;
            case NAL.SPS: //Sequence parameter set
            case NAL.PPS: //Picture parameter set
                //System.out.println("sps or pps write");
                out.write(H264RTP.NON_IDR_PICTURE);
                out.write(getBuffer(), getPayloadStart(), getPayloadLength());
                break;
            default:
                throw new NotImplementedException("NAL type " + getNAL().getType() + " not implemented");
        }
    }

    @Override
    public void writeRawToStream(OutputStream out) throws IOException {
        writeRawH264toStream(out);
    }
}
