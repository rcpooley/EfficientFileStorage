package com.rcpooley.effstorage.core;

import java.io.IOException;

public interface EfficientDeltaValue {

    public static class Values {
        public long[] values;
        public int scale;
        public Values(long[] values, int scale) {
            this.values = values;
            this.scale = scale;
        }
        public Values(long[] values) {
            this(values, 0);
        }
    }

    int getNumInitialBits();

    Values getValues(Object[] values) throws IOException;

    Object[] convertValues(Values values) throws IOException;

    default public boolean useScale() {
        return false;
    }
}
