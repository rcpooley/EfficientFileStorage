package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;
import com.rcpooley.effstorage.core.EfficientSerializable;

import java.io.*;

@Efficient
public class CustomStruct implements EfficientSerializable {

    public String s;

    private CustomStruct() {}

    public CustomStruct(String s) {
        this.s = s;
    }

    @Override
    public void serialize(DataOutputStream dos) throws IOException {
        dos.writeInt(s.length());
        dos.write(s.getBytes());
    }

    @Override
    public void deserialize(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        byte[] d = new byte[len];
        int r = dis.read(d);
        if (r != len) {
            throw new RuntimeException("Failed to read " + len + " bytes for string");
        }
        this.s = new String(d).toUpperCase();
    }
}
