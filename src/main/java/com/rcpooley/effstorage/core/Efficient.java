package com.rcpooley.effstorage.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Efficient {
    boolean storeByDelta() default false;
}
