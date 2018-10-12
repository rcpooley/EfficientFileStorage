package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class ArrSBDT {

    @Efficient
    public StoreByDeltaTypes[] sbdt;

    private ArrSBDT() {}

    public ArrSBDT(StoreByDeltaTypes[] arr) {
        this.sbdt = arr;
    }

}
