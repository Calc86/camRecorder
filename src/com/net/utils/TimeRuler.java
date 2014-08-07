package com.net.utils;

/**
 * Created by calc on 07.08.14.
 *
 * Дает true после исчисления какого то времени
 */
public class TimeRuler {
    private long time = 0;
    private int len;

    public TimeRuler(int len) {
        this.len = len;
    }

    public boolean isTime(){
        if(time == 0){
            setTime();
            return false;
        }
        if(System.currentTimeMillis() - time > len){
            setTime();
            return true;
        }
        return false;
    }

    private void setTime(){
        time = System.currentTimeMillis();
    }
}
