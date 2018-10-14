package com.rcpooley.effstorage.bitio;

import java.io.IOException;
import java.io.InputStream;

public class BitReader {

    private InputStream is;

    private int curByte;

    private int availableBits;

    public BitReader(InputStream is) {
        this.is = is;
    }

    public long readBitsLong(int numBits) throws IOException {
        if (numBits > 64 || numBits < 0) {
            throw new RuntimeException("Cannot read " + numBits + " bits");
        }

        long b = 0;
        int todo = numBits;
        while (todo > 0) {
            if (availableBits == 0) {
                curByte = is.read();
                availableBits = 8;
            }

            int toRead = Math.min(todo, availableBits);
            int d = (curByte >> (availableBits - toRead)) & BitUtil.getMask(toRead);
            b = (b << toRead) | d;
            todo -= toRead;
            availableBits -= toRead;
        }
        return b;
    }

    public int readBits(int numBits) throws IOException {
        if (numBits > 32 || numBits < 0) {
            throw new RuntimeException("Cannot read " + numBits + " bits");
        }
        return (int) readBitsLong(numBits);
    }
}
