package com.net.utils;

/**
 * Created by calc on 29.07.14.
 */
abstract public class uShort {
    public static int get(short u){
        return u & BIT._16;
    }

    public static short add(short s1, short s2){
        return (short)(BIT.uShort(s1) + BIT.uShort(s2));
    }

    public static short sub(short s1, short s2){
        return (short)(BIT.uShort(s1) - BIT.uShort(s2));
    }
}
