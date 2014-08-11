package ru.xsrv.net.utils;

/**
 * Created by calc on 02.08.14.
 * Считает время между start и stop
 *
 */
public class Profiler {
    private long min = Long.MAX_VALUE;
    private long max = 0;
    private long last = 0;
    private long sum = 0;
    private long count = 0;
    private boolean started = false;
    private int skip = 0;

    public Profiler(int skip) {
        this.skip = skip;
    }

    private long time(){
        return System.nanoTime();
        //return System.currentTimeMillis();
    }

    public void start(){
        started = true;
        last = time();
    }

    public void stop(){
        if(skip > 0){
            skip --;
            return;
        }
        if(started){
            last = time() - last;
            min = Math.min(min, last);
            max = Math.max(max, last);
            sum += last; count ++;
            started = false;
        }
    }

    public void print(long min){
        if(skip > 0) return;
        if(last >= min)
            System.out.println(this.toString());
    }

    public static double toSeconds(long nano){
        return nano / 1000000000.0;
    }

    public long getLast() {
        return last;
    }

    @Override
    public String toString() {
        return "Profiler(ns): " +
                "last=" + toSeconds(last) +
                " [min=" + toSeconds(min) +
                ", max=" + toSeconds(max) +
                ", avg=" + toSeconds(sum/(count == 0 ? 1 : count)) + "(count=" + count +")" +
                ']';
    }
}
