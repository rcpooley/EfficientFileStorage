package com.rcpooley.effstorage.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface EfficientSerializer<T> {

    void serialize(T obj, DataOutputStream dos) throws IOException;

    T deserialize(DataInputStream dis) throws IOException;

}
