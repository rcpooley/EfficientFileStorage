package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class People {

    @Efficient
    private Person[] ppl;

    private People() {}

    public People(Person[] ppl) {
        this.ppl = ppl;
    }

    public Person[] getPpl() {
        return ppl;
    }
}
