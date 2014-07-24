package com.net.utils;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamHolder extends OutputStream {
    volatile private OutputStream out;

    public OutputStreamHolder(final OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

        /*public void setOut(OutputStream out) {
            this.out = out;
        }*/

    public synchronized void change(final OutputStream out) throws IOException {
        this.out.flush();
        this.out.close();
        this.out = out;
    }
}