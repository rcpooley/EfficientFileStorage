package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class StoreByDeltaTypes {

    @Efficient(storeByDelta = true)
    public int a;

    @Efficient(storeByDelta = true)
    public long b;

    private StoreByDeltaTypes() {}

    public StoreByDeltaTypes(int a, long b) {
        this.a = a;
        this.b = b;
    }
}
