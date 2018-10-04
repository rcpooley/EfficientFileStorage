package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class A {

    @Efficient
    private B b;

    private A() {}

    public A(B b) {
        this.b = b;
    }

    public B getB() {
        return b;
    }
}
