package com.rcpooley.effstorage.core;

public interface EfficientDeltaValue<T> {

    int getNumInitialBits();

    long[] getValues(T[] values);

    T[] convertValues(long[] values);
}
