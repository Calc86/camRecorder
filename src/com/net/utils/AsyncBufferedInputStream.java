package com.net.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by calc on 07.08.14.
 * Асинхронно читает InputStream и кладет в BlockingDeque
 * Так что read "ждет" данные (BlockingDeque.take)
 */
public class AsyncBufferedInputStream extends InputStream {
    private boolean stop = false;
    private IOException exception = null;

    /**
     * put and take
     */
    private BlockingDeque<byte[]> data = new LinkedBlockingDeque<byte[]>();
    /**
     * Позиция в текущем byte[] блоке
     */
    private int nextPos = 0;
    /**
     * Текущий блок с которым работаем
     */
    private byte[] current = null;
    private Thread reader;

    /**
     * Сокет с которым работаем
     */
    //private Socket socket;
    private InputStream in;

    public AsyncBufferedInputStream(InputStream in) {
        this.in = in;
        reader = new Thread(new ReaderThread());
        reader.start();
    }

    @Override
    public int read() throws IOException {
        if(current == null || nextPos >= current.length){
            try {
                current = data.take();
                nextPos = 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //нужно до read весь наш буффер и умереть.
        if(isStop() && data.size() == 0){
            if(exception != null) throw exception;
            else return -1;
        }
        
        return current[nextPos++] & BIT._8; // unsigned byte
    }

    public void stop(){
        stop = true;
        reader.interrupt();
        data.add(new byte[0]);  //нулевой байт, чтобы основной поток прочел наш -1
    }

    public boolean isStop() {
        return stop;
    }

    private class ReaderThread implements Runnable{
        byte[] buffer = new byte[65536];

        @Override
        public void run() {
            try {
                TimeRuler ruler = new TimeRuler(1000);
                while(!isStop()){
                    if(ruler.isTime() && data.size() > 500)
                        System.out.println("WARNING: count in blocking deque: " + data.size());

                    int count = in.read(buffer);
                    if(count == -1) break;
                    if(count == 0) continue;

                    byte[] b = new byte[count];
                    System.arraycopy(buffer, 0, b, 0, count);
                    try {
                        data.put(b);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                exception = e;
                stop();
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        stop();
    }

    /*public static void main(String[] args) {
        byte[] randData = new byte[65536];

        for (int i = 0; i < 65536; i++) {
            randData[i] = (byte)(Math.random() * 255 - 128);
        }

        byte[] buffer;
        Profiler p = new Profiler(0);
        p.start();
        for (int i = 0; i < 1000; i++) {
            int size = (int)(Math.random() * 2048 + 200);
            buffer = new byte[size];
            System.arraycopy(randData, 0, buffer, 0, size);
            //System.out.println(size);
        }
        p.stop();
        System.out.println(p);
    }*/
}
