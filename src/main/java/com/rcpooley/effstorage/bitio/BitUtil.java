package com.rcpooley.effstorage.bitio;

public class BitUtil {
    public static int getMask(int numBits) {
        return ~(~0 << numBits);
    }
}
