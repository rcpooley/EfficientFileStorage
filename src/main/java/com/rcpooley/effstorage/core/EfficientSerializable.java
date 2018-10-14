package com.rcpooley.effstorage.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface EfficientSerializable {

    void serialize(DataOutputStream dos) throws IOException;

    void deserialize(DataInputStream dis) throws IOException;

}
