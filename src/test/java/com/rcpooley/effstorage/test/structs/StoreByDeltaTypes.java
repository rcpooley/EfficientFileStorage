package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

import java.math.BigDecimal;

@Efficient
public class StoreByDeltaTypes {

    @Efficient(storeByDelta = true)
    public int a;

    @Efficient(storeByDelta = true)
    public long b;

    @Efficient(storeByDelta = true)
    public BigDecimal c;

    private StoreByDeltaTypes() {}

    public StoreByDeltaTypes(int a, long b, BigDecimal c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
