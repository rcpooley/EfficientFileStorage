package com.rcpooley.effstorage.test.structs;

import com.rcpooley.effstorage.core.Efficient;

@Efficient
public class Person {
    @Efficient
    private String name;

    @Efficient
    private int age;

    @Efficient
    public byte b;

    @Efficient
    public short s;

    @Efficient
    public long l;

    @Efficient
    public char c;

    @Efficient
    public float f;

    @Efficient
    public double d;

    @Efficient
    public boolean bool;

    private Person() {}

    public Person(String name, int age, byte b, short s, long l, char c, float f, double d, boolean bool) {
        this.name = name;
        this.age = age;
        this.b = b;
        this.s = s;
        this.l = l;
        this.c = c;
        this.f = f;
        this.d = d;
        this.bool = bool;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}