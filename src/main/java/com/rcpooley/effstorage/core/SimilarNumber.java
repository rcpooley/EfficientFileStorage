package com.rcpooley.effstorage.core;

public class SimilarNumber {

    private static int nextID = 1;

    public static SimilarNumber getNewNumber() {
        return new SimilarNumber(nextID++);
    }

    private int id;

    private SimilarNumber(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
