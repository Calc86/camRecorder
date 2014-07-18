package com.net.jpeg;

/**
 *
 * Created by calc on 17.07.14.
 */
public class JpegRfc2345 {

    //Appendix A
    /*
    * Table K.1 from JPEG spec.
    */
    public static final byte jpeg_luma_quantizer[] = {
        16, 11, 10, 16, 24, 40, 51, 61,
        12, 12, 14, 19, 26, 58, 60, 55,
        14, 13, 16, 24, 40, 57, 69, 56,
        14, 17, 22, 29, 51, 87, 80, 62,
        18, 22, 37, 56, 68, 109, 103, 77,
        24, 35, 55, 64, 81, 104, 113, 92,
        49, 64, 78, 87, 103, 121, 120, 101,
        72, 92, 95, 98, 112, 100, 103, 99
    };

    /*
    * Table K.2 from JPEG spec.
    */
    public static final byte jpeg_chroma_quantizer[] = {
        17, 18, 24, 47, 99, 99, 99, 99,
        18, 21, 26, 66, 99, 99, 99, 99,
        24, 26, 56, 99, 99, 99, 99, 99,
        47, 66, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99
    };

    /*
    * Call MakeTables with the Q factor and two u_char[64] return arrays
    */
    public void MakeTables(int q, byte[] lqt, byte[] cqt)
    {
        int i;
        int factor = q;

        if (q < 1) factor = 1;
        if (q > 99) factor = 99;
        if (q < 50)
            q = 5000 / factor;
        else
            q = 200 - factor*2;
        for (i=0; i < 64; i++) {
            int lq = (jpeg_luma_quantizer[i] * q + 50) / 100;
            int cq = (jpeg_chroma_quantizer[i] * q + 50) / 100;

            /* Limit the quantizers to 1 <= q <= 255 */
            if (lq < 1) lq = 1;
            else if (lq > 255) lq = 255;
            lqt[i] = (byte)(lq & 0xff); //java

            if (cq < 1) cq = 1;
            else if (cq > 255) cq = 255;
            cqt[i] = (byte)(cq & 0xff); //java
        }
    }

    //Appendix B
    public static final byte lum_dc_codelens[] = {
            0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
    };

    public static final byte lum_dc_symbols[] = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    };

    public static final byte lum_ac_codelens[] = {
            0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d,
    };

    public static final byte lum_ac_symbols[] = {
            0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12,
            0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
            0x22, 0x71, 0x14, 0x32, (byte)0x81, (byte)0x91, (byte)0xa1, 0x08,
            0x23, 0x42, (byte)0xb1, (byte)0xc1, 0x15, 0x52, (byte)0xd1, (byte)0xf0,
            0x24, 0x33, 0x62, 0x72, (byte)0x82, 0x09, 0x0a, 0x16,
            0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
            0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
            0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
            0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
            0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
            0x7a, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89,
            (byte)0x8a, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96, (byte)0x97, (byte)0x98,
            (byte)0x99, (byte)0x9a, (byte)0xa2, (byte)0xa3, (byte)0xa4, (byte)0xa5, (byte)0xa6, (byte)0xa7,
            (byte)0xa8, (byte)0xa9, (byte)0xaa, (byte)0xb2, (byte)0xb3, (byte)0xb4, (byte)0xb5, (byte)0xb6,
            (byte)0xb7, (byte)0xb8, (byte)0xb9, (byte)0xba, (byte)0xc2, (byte)0xc3, (byte)0xc4, (byte)0xc5,
            (byte)0xc6, (byte)0xc7, (byte)0xc8, (byte)0xc9, (byte)0xca, (byte)0xd2, (byte)0xd3, (byte)0xd4,
            (byte)0xd5, (byte)0xd6, (byte)0xd7, (byte)0xd8, (byte)0xd9, (byte)0xda, (byte)0xe1, (byte)0xe2,
            (byte)0xe3, (byte)0xe4, (byte)0xe5, (byte)0xe6, (byte)0xe7, (byte)0xe8, (byte)0xe9, (byte)0xea,
            (byte)0xf1, (byte)0xf2, (byte)0xf3, (byte)0xf4, (byte)0xf5, (byte)0xf6, (byte)0xf7, (byte)0xf8,
            (byte)0xf9, (byte)0xfa,
    };

    public static final byte chm_dc_codelens[] = {
            0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
    };

    public static final byte chm_dc_symbols[] = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    };

    public static final byte chm_ac_codelens[] = {
            0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77,
    };

    public static final byte chm_ac_symbols[] = {
            0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
            0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
            0x13, 0x22, 0x32, (byte)0x81, 0x08, 0x14, 0x42, (byte)0x91,
            (byte)0xa1, (byte)0xb1, (byte)0xc1, 0x09, 0x23, 0x33, 0x52, (byte)0xf0,
            0x15, 0x62, 0x72, (byte)0xd1, 0x0a, 0x16, 0x24, 0x34,
            (byte)0xe1, 0x25, (byte)0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
            0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
            0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
            0x79, 0x7a, (byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87,
            (byte)0x88, (byte)0x89, (byte)0x8a, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96,
            (byte)0x97, (byte)0x98, (byte)0x99, (byte)0x9a, (byte)0xa2, (byte)0xa3, (byte)0xa4, (byte)0xa5,
            (byte)0xa6, (byte)0xa7, (byte)0xa8, (byte)0xa9, (byte)0xaa, (byte)0xb2, (byte)0xb3, (byte)0xb4,
            (byte)0xb5, (byte)0xb6, (byte)0xb7, (byte)0xb8, (byte)0xb9, (byte)0xba, (byte)0xc2, (byte)0xc3,
            (byte)0xc4, (byte)0xc5, (byte)0xc6, (byte)0xc7, (byte)0xc8, (byte)0xc9, (byte)0xca, (byte)0xd2,
            (byte)0xd3, (byte)0xd4, (byte)0xd5, (byte)0xd6, (byte)0xd7, (byte)0xd8, (byte)0xd9, (byte)0xda,
            (byte)0xe2, (byte)0xe3, (byte)0xe4, (byte)0xe5, (byte)0xe6, (byte)0xe7, (byte)0xe8, (byte)0xe9,
            (byte)0xea, (byte)0xf2, (byte)0xf3, (byte)0xf4, (byte)0xf5, (byte)0xf6, (byte)0xf7, (byte)0xf8,
            (byte)0xf9, (byte)0xfa,
    };

    public int MakeQuantHeader(byte[] p, int start, byte[] qt, int tableNo)
    {
        int i = start;
        p[i++] = (byte)0xff;
        p[i++] = (byte)0xdb;            /* DQT */
        p[i++] = 0;               /* length msb */
        p[i++] = 67;              /* length lsb */
        p[i++] = (byte)tableNo;
        //memcpy(p, qt, 64);
        System.arraycopy(qt, 0, p, i, 64);
        return (i + 64);
    }

    public int MakeHuffmanHeader(byte[] p, int start, byte[] codelens, int ncodes,
                      byte[] symbols, int nsymbols, int tableNo,
                      int tableClass)
    {
        int i = start;
        p[i++] = (byte)0xff;
        p[i++] = (byte)0xc4;            /* DHT */
        p[i++] = 0;               /* length msb */
        p[i++] = (byte)(3 + ncodes + nsymbols); /* length lsb */
        p[i++] = (byte)((tableClass << 4) | tableNo);
        //memcpy(p, codelens, ncodes);
        System.arraycopy(codelens, 0, p, i, ncodes);
        i += ncodes;
        //memcpy(p, symbols, nsymbols);
        System.arraycopy(symbols, 0, p, i, nsymbols);
        i += nsymbols;
        return i;
    }

    /**
     *
     * @ param start положение в буере
     * @ param dri
     * @ return next position of buffer
     */
    public int MakeDRIHeader(byte[] p, int start, int dri) {
        int i = start;
        p[i++] = (byte)0xff;
        p[i++] = (byte)0xdd;            /* DRI */
        p[i++] = 0x0;             /* length msb */
        p[i++] = 4;               /* length lsb */
        p[i++] = (byte)(dri >>> 8);       /* dri msb */
        p[i++] = (byte)(dri & 0xff);      /* dri lsb */
        return i;
    }

    /*
 *  Arguments:
 *    type, width, height: as supplied in RTP/JPEG header
 *    lqt, cqt: quantization tables as either derived from
 *         the Q field using MakeTables() or as specified
 *         in section 4.2.
 *    dri: restart interval in MCUs, or 0 if no restarts.
 *
 *    p: pointer to return area
 *
 *  Return value:
 *    The length of the generated headers.
 *
 *    Generate a frame and scan headers that can be prepended to the
 *    RTP/JPEG data payload to produce a JPEG compressed image in
 *    interchange format (except for possible trailing garbage and
 *    absence of an EOI marker to terminate the scan).
 */
    public int MakeHeaders(byte[] p, int start,  int type, int w, int h, byte[] lqt,
                    byte[] cqt, int dri)
    {
        //u_char *start = p;    //start param
        int i = start;

        /* convert from blocks to pixels */
        w <<= 3;
        h <<= 3;
        p[i++] = (byte)0xff;
        p[i++] = (byte)0xd8;            /* SOI */

        i = MakeQuantHeader(p, i, lqt, 0);
        i = MakeQuantHeader(p, i, cqt, 1);

        if (dri != 0)
            i = MakeDRIHeader(p, i, dri);

        p[i++] = (byte)0xff;
        p[i++] = (byte)0xc0;            /* SOF */
        p[i++] = 0;               /* length msb */
        p[i++] = 17;              /* length lsb */
        p[i++] = 8;               /* 8-bit precision */
        p[i++] = (byte)(h >> 8);          /* height msb */
        p[i++] = (byte)h;               /* height lsb */
        p[i++] = (byte)(w >> 8);          /* width msb */
        p[i++] = (byte)w;               /* wudth lsb */
        p[i++] = 3;               /* number of components */
        p[i++] = 0;               /* comp 0 */
        if (type == 0)
            p[i++] = 0x21;    /* hsamp = 2, vsamp = 1 */
        else
            p[i++] = 0x22;    /* hsamp = 2, vsamp = 2 */
        p[i++] = 0;               /* quant table 0 */
        p[i++] = 1;               /* comp 1 */
        p[i++] = 0x11;            /* hsamp = 1, vsamp = 1 */
        p[i++] = 1;               /* quant table 1 */
        p[i++] = 2;               /* comp 2 */
        p[i++] = 0x11;            /* hsamp = 1, vsamp = 1 */
        p[i++] = 1;               /* quant table 1 */
        i = MakeHuffmanHeader(p, i, lum_dc_codelens,
                lum_dc_codelens.length,
                lum_dc_symbols,
                lum_dc_symbols.length, 0, 0);
        i = MakeHuffmanHeader(p, i, lum_ac_codelens,
                lum_ac_codelens.length,
                lum_ac_symbols,
                lum_ac_symbols.length, 0, 1);
        i = MakeHuffmanHeader(p, i, chm_dc_codelens,
                chm_dc_codelens.length,
                chm_dc_symbols,
                chm_dc_symbols.length, 1, 0);
        i = MakeHuffmanHeader(p, i, chm_ac_codelens,
                chm_ac_codelens.length,
                chm_ac_symbols,
                chm_ac_symbols.length, 1, 1);
        p[i++] = (byte)0xff;
        p[i++] = (byte)0xda;            /* SOS */
        p[i++] = 0;               /* length msb */
        p[i++] = 12;              /* length lsb */
        p[i++] = 3;               /* 3 components */
        p[i++] = 0;               /* comp 0 */
        p[i++] = 0;               /* huffman table 0 */
        p[i++] = 1;               /* comp 1 */
        p[i++] = 0x11;            /* huffman table 1 */
        p[i++] = 2;               /* comp 2 */
        p[i++] = 0x11;            /* huffman table 1 */
        p[i++] = 0;               /* first DCT coeff */
        p[i++] = 63;              /* last DCT coeff */
        p[i++] = 0;               /* sucessive approx. */

        return (i - start);
    }
}
