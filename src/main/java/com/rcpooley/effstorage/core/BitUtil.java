package com.rcpooley.effstorage.core;

public class BitUtil {
    public static int getMask(int numBits) {
        return ~(~0 << numBits);
    }
}
