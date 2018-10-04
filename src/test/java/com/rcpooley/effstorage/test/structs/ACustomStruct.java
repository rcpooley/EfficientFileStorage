package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class ACustomStruct {

    @Efficient
    public CustomStruct a;

    @Efficient
    public CustomStruct[] b;

    private ACustomStruct() {}

    public ACustomStruct(CustomStruct a, CustomStruct[] b) {
        this.a = a;
        this.b = b;
    }
}
