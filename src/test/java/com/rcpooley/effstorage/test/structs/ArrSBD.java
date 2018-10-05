package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class ArrSBD {

    @Efficient
    public StoreByDelta[] arr;

    private ArrSBD() {}

    public ArrSBD(StoreByDelta[] arr) {
        this.arr = arr;
    }
}
