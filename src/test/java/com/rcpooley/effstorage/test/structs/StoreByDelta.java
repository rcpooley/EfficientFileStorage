package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class StoreByDelta {

    @Efficient
    public String s;

    @Efficient(storeByDelta = true)
    public double d;

    private StoreByDelta() {}

    public StoreByDelta(String s, double d) {
        this.s = s;
        this.d = d;
    }

}
