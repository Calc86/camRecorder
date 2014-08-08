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
public class H264RTP implements IRaw{
    private NAL nal = new NAL();
    private FUHeader FUHeader = new FUHeader();
    private RTPWrapper rtp;
    //http://ip.hhi.de/imagecom_G1/assets/pdfs/h264_iso-iec_14496-10.pdf
    public static final byte[] NON_IDR_PICTURE = {0x00, 0x00, 0x00, 0x01};

    public H264RTP(RTPWrapper rtp) {
        this.rtp = rtp;
    }

    public void setValues(){
        nal.set(rtp.getBuffer()[rtp.getPayloadStart()]);
        FUHeader.set(rtp.getBuffer()[rtp.getPayloadStart() + 1]);
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
                return rtp.getPayloadStart() + 2;
            case NAL.SPS:
            case NAL.PPS:
                return rtp.getPayloadStart();
            default:
                throw new NotImplementedException("NAL type " + getNAL().getType() + " not implemented");
        }
    }

    public int getH264PayloadLength() {
        switch(getNAL().getType()){
            case NAL.FU_A:
                return rtp.getPayloadLength() - 2;
            case NAL.SPS:
            case NAL.PPS:
                return rtp.getPayloadLength();
            default:
                throw new NotImplementedException("NAL type " + getNAL().getType() + " not implemented");
        }
    }

    public byte getReconstructedNal(){
        byte nal = (byte)(rtp.getBuffer()[rtp.getPayloadStart()] & 0xE0);
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
                    out.write(rtp.getBuffer(), getH264PayloadStart(), getH264PayloadLength());
                } else if(fu.isEnd()){
                    //if(debug) System.out.println("end");
                    out.write(rtp.getBuffer(), getH264PayloadStart(), getH264PayloadLength());
                } else{
                    //if(debug) System.out.println("middle");
                    out.write(rtp.getBuffer(), getH264PayloadStart(), getH264PayloadLength());
                }
                break;
            case NAL.SPS: //Sequence parameter set
            case NAL.PPS: //Picture parameter set
                //System.out.println("sps or pps write");
                out.write(H264RTP.NON_IDR_PICTURE);
                out.write(rtp.getBuffer(), rtp.getPayloadStart(), rtp.getPayloadLength());
                break;
            default:
                throw new NotImplementedException("NAL type " + getNAL().getType() + " not implemented");
        }
    }

    public void writeRawToStream(OutputStream out) throws IOException {
        try {
            writeRawH264toStream(out);
        } catch (NotImplementedException e) {
            System.out.println(e.getMessage());
        }
    }
}
