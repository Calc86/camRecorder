package com.net.utils;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamHolder extends OutputStream {
    volatile private OutputStream out = null;

    public OutputStreamHolder() {
        this(null);
    }

    public OutputStreamHolder(final OutputStream out) {
        if(this.out != null)
            this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        if(this.out != null)
            out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if(this.out != null)
            out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(this.out != null)
            out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if(this.out != null)
            out.flush();
    }

    @Override
    public void close() throws IOException {
        if(this.out != null)
            out.close();
    }

        /*public void setOut(OutputStream out) {
            this.out = out;
        }*/

    public synchronized void change(final OutputStream out) throws IOException {
        flush();
        close();
        this.out = out;
    }
}