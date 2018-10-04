package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class Arr {

    @Efficient
    public String[] str;

    @Efficient
    public int[] i;

    @Efficient
    public byte[] b;

    @Efficient
    public short[] s;

    @Efficient
    public long[] l;

    @Efficient
    public char[] c;

    @Efficient
    public float[] f;

    @Efficient
    public double[] d;

    @Efficient
    public boolean[] bool;

    private Arr() {}

    public Arr(String[] str, int[] i, byte[] b, short[] s, long[] l, char[] c, float[] f, double[] d, boolean[] bool) {
        this.str = str;
        this.i = i;
        this.b = b;
        this.s = s;
        this.l = l;
        this.c = c;
        this.f = f;
        this.d = d;
        this.bool = bool;
    }
}
