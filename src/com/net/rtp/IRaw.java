package com.net.rtp;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by calc on 30.07.14.
 *
 */
public interface IRaw {
    public void writeRawToStream(OutputStream out) throws IOException;
}
