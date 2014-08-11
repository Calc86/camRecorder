package ru.xsrv.net.utils;

/**
 * Created by calc on 07.08.14.
 *
 */
public class RandomSleep {
    public static void sleep(int min, int max){
        int time = (int)(Math.random() * (max-min) + min);
        System.out.println("Sleep: " + time);
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
