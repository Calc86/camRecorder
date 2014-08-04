package com.net.utils;

/**
 * Created by calc on 02.08.14.
 *
 * Класс для подсчета количества пакетов(count), трафика(traffic) и
 * времени выполнения за единицу времени (len).
 *
 */
public class Counter {
    private String name;
    private int len = 1;
    private long count = 0;
    private long time;
    private long workTime;
    private double traffic;   //in K
    private boolean print = false;

    public Counter(String name) {
        this.name = name;
    }

    public void count(long workTime, double traffic)
    {
        this.workTime += workTime;
        this.traffic += traffic;
        count++;
        test();
    }

    public void print(){
        System.out.println(name +": count=" + count + " in " + len + "sec " + " time=" + Profiler.toSeconds(workTime) + " traffic=" + traffic + "K");
    }

    private void test(){
        if((System.currentTimeMillis() - time) >= len * 1000){
            if(print) print();
            workTime = 0;
            traffic = 0;
            count = 0;
            time = 0;
        }
        if(time == 0){
            time = System.currentTimeMillis();
        }
    }

    public void setPrint(boolean print) {
        this.print = print;
    }
}
