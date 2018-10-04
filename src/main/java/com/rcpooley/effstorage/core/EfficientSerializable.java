package com.rcpooley.effstorage.core;

import java.io.DataInputStream;
import java.io.IOException;

public interface EfficientSerializable {

    byte[] serialize() throws IOException;

    void deserialize(DataInputStream dis) throws IOException;

}
