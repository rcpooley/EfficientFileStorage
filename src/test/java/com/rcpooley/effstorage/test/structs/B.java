package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class B {

    @Efficient
    private int val;

    private B() {}

    public B(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
