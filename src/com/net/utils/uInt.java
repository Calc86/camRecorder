package com.net.utils;

/**
 * Created by calc on 29.07.14.
 *
 */
abstract public class uInt {
    public static long get(int u){
        return u & BIT._32;
    }

    public static int add(int s1, int s2){
        return (int)(get(s1) + get(s2));
    }

    public static int sub(int s1, int s2){
        return (int)(get(s1) - get(s2));
    }
}
