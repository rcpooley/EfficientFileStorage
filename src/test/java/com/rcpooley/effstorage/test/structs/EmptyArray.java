package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class EmptyArray {

    @Efficient
    public double[] arr;

    @Efficient
    public StoreByDelta[] delta;

    public EmptyArray() {
        this.arr = new double[0];
        this.delta = new StoreByDelta[0];
    }
}
