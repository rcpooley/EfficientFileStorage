package com.rcpooley.effstorage.core;

import java.io.IOException;
import java.io.OutputStream;

public class BitWriter {

    private OutputStream os;

    private int curByte;

    private int curBits;

    public BitWriter(OutputStream os) {
        this.os = os;
    }

    public void writeBits(int data, int numBits) throws IOException {
        int todo = numBits;
        while (todo > 0) {
            int toRead = Math.min(8 - curBits, todo);
            int b = (data >> (todo - toRead)) & BitUtil.getMask(toRead);
            curByte = (curByte << toRead) | b;
            todo -= toRead;
            curBits += toRead;
            if (curBits == 8) {
                os.write(curByte);
                curByte = 0;
                curBits = 0;
            }
        }
    }

    public void finish() throws IOException {
        if (curBits > 0) {
            os.write(curByte << (8 - curBits));
            curByte = 0;
            curBits = 0;
        }
    }

}
