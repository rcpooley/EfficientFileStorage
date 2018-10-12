package com.rcpooley.effstorage.test;

import com.rcpooley.effstorage.core.EfficientException;
import com.rcpooley.effstorage.core.EfficientStorage;
import com.rcpooley.effstorage.test.structs.*;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

public class TestEfficientStorage {

    @Test
    public void testClassPrimitiveTypes() throws EfficientException {
        byte[] data = EfficientStorage.serialize(new Person(
                "rob",
                20,
                (byte) 127,
                (short) 2,
                4L,
                'z',
                3.14F,
                15.92,
                false
        ));

        Person p = (Person) EfficientStorage.deserialize(Person.class, data);
        Assert.assertEquals("rob", p.getName());
        Assert.assertEquals(20, p.getAge());
        Assert.assertEquals(127, p.b);
        Assert.assertEquals(2, p.s);
        Assert.assertEquals(4L, p.l);
        Assert.assertEquals('z', p.c);
        Assert.assertEquals(3.14F, p.f, 10e-4);
        Assert.assertEquals(15.92, p.d, 10e-4);
        Assert.assertEquals(false, p.bool);
    }

    @Test
    public void testClassArrays() throws EfficientException {
        String[] a = new String[]{"a", "b"};
        int[] b = new int[]{1, 2};
        byte[] c = new byte[]{3, 4};
        short[] d = new short[]{5, 6};
        long[] e = new long[]{7, 8};
        char[] f = new char[]{'c', 'd'};
        float[] g = new float[]{1.23F, 4.56F};
        double[] h = new double[]{7.89, 10.1112};
        boolean[] i = new boolean[]{true, false};
        byte[] data = EfficientStorage.serialize(new Arr(a, b, c, d, e, f, g, h, i));

        Arr arr = (Arr) EfficientStorage.deserialize(Arr.class, data);
        Assert.assertArrayEquals(a, arr.str);
        Assert.assertArrayEquals(b, arr.i);
        Assert.assertArrayEquals(c, arr.b);
        Assert.assertArrayEquals(d, arr.s);
        Assert.assertArrayEquals(e, arr.l);
        Assert.assertArrayEquals(f, arr.c);
        Assert.assertArrayEquals(g, arr.f, 10e-4f);
        Assert.assertArrayEquals(h, arr.d, 10e-4);
        Assert.assertArrayEquals(i, arr.bool);
    }

    @Test
    public void testSimpleClassRef() throws EfficientException {
        byte[] data = EfficientStorage.serialize(new A(new B(127)));
        A a = (A) EfficientStorage.deserialize(A.class, data);
        Assert.assertEquals(a.getB().getVal(), 127);
    }

    @Test
    public void testClassSubclasses() throws EfficientException {
        byte[] data = EfficientStorage.serialize(new People(new Person[]{
                new Person(
                        "rob",
                        20,
                        (byte) 127,
                        (short) 2,
                        4L,
                        'z',
                        3.14F,
                        15.92,
                        false
                ),
                new Person(
                        "tom",
                        24,
                        (byte) 126,
                        (short) 3,
                        5L,
                        'y',
                        5.14F,
                        16.92,
                        true
                )
        }));

        People ppl = (People) EfficientStorage.deserialize(People.class, data);
        Person[] pa = ppl.getPpl();
        Person p = pa[0];
        Assert.assertEquals("rob", p.getName());
        Assert.assertEquals(20, p.getAge());
        Assert.assertEquals(127, p.b);
        Assert.assertEquals(2, p.s);
        Assert.assertEquals(4L, p.l);
        Assert.assertEquals('z', p.c);
        Assert.assertEquals(3.14F, p.f, 10e-4);
        Assert.assertEquals(15.92, p.d, 10e-4);
        Assert.assertEquals(false, p.bool);
        p = pa[1];
        Assert.assertEquals("tom", p.getName());
        Assert.assertEquals(24, p.getAge());
        Assert.assertEquals(126, p.b);
        Assert.assertEquals(3, p.s);
        Assert.assertEquals(5L, p.l);
        Assert.assertEquals('y', p.c);
        Assert.assertEquals(5.14F, p.f, 10e-4);
        Assert.assertEquals(16.92, p.d, 10e-4);
        Assert.assertEquals(true, p.bool);
    }

    @Test
    public void testNoDefaultConstructor() {
        try {
            EfficientStorage.deserialize(NoDefaultConstructor.class, EfficientStorage.serialize(new NoDefaultConstructor(1)));
            Assert.assertTrue(false);
        } catch (EfficientException e) {
            Assert.assertEquals("No default constructor found for class " + NoDefaultConstructor.class.getName(), e.getMessage());
        }
    }

    @Test
    public void testCustomSerializable() throws EfficientException {
        byte[] data = EfficientStorage.serialize(new CustomStruct("hello world"));
        CustomStruct cs = (CustomStruct) EfficientStorage.deserialize(CustomStruct.class, data);
        Assert.assertEquals("HELLO WORLD", cs.s);
    }

    @Test
    public void testCustomSubclass() throws EfficientException {
        byte[] data = EfficientStorage.serialize(new ACustomStruct(
                new CustomStruct("hello"),
                new CustomStruct[]{
                        new CustomStruct("what"),
                        new CustomStruct("hmmm")
                }
        ));

        ACustomStruct acs = (ACustomStruct) EfficientStorage.deserialize(ACustomStruct.class, data);
        Assert.assertEquals("HELLO", acs.a.s);
        Assert.assertEquals("WHAT", acs.b[0].s);
        Assert.assertEquals("HMMM", acs.b[1].s);
    }

    @Test
    public void testStoreByDeltaUnsetPrecision() {
        try {
            EfficientStorage.serialize(new ArrSBD(new StoreByDelta[]{new StoreByDelta("a", 1)}));
            Assert.assertTrue(false);
        } catch (EfficientException e) {
            Assert.assertEquals("Decimal precision not set for field d in class " + StoreByDelta.class.getName(), e.getMessage());
        }
    }

    @Test
    public void testStoreByDelta() throws EfficientException, NoSuchFieldException, IllegalAccessException {
        String[] strs = {
                "hello",
                "goodbye",
                "hmmmm",
                "what"
        };
        double[] dubs = {
                7.123,
                7.125,
                7.124,
                7.12
        };
        StoreByDelta[] arr = new StoreByDelta[strs.length];
        for (int i = 0; i < strs.length; i++) {
            arr[i] = new StoreByDelta(strs[i], dubs[i]);
        }
        EfficientStorage.setPrecision(arr, "d", 3);
        byte[] data = EfficientStorage.serialize(new ArrSBD(arr));
        ArrSBD a = (ArrSBD) EfficientStorage.deserialize(ArrSBD.class, data);
        for (int i = 0; i < arr.length; i++) {
            Assert.assertEquals(arr[i].s, a.arr[i].s);
            Assert.assertEquals(arr[i].d, a.arr[i].d, 10e-5);
        }

        // Make sure memory is cleaned up
        Field f = EfficientStorage.class.getDeclaredField("precisionMap");
        f.setAccessible(true);
        Map<Object, Map<String, Integer>> map = (Map<Object, Map<String, Integer>>) f.get(null);
        f.setAccessible(false);
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testStoreByDeltaTypes() throws EfficientException {
        int[] vals = {0x7FFFFFFF, 0x7FFFFFEE, 0x7FFFFFEF, 0x7FFFFFDC, 0x7FFFFFBB, 0x7FFFFF00};
        long s = 0x7FFFFFFFFFFFFF00L;
        long[] longVals = {s, s + 0x10, s + 0x18, s + 0x20, s + 0x30, s + 0x90};

        StoreByDeltaTypes[] arr = new StoreByDeltaTypes[vals.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new StoreByDeltaTypes(vals[i], longVals[i]);
        }

        byte[] data = EfficientStorage.serialize(new ArrSBDT(arr));

        ArrSBDT arrSBDT = (ArrSBDT) EfficientStorage.deserialize(ArrSBDT.class, data);
        for (int i = 0; i < vals.length; i++) {
            Assert.assertEquals(vals[i], arrSBDT.sbdt[i].a);
            Assert.assertEquals(longVals[i], arrSBDT.sbdt[i].b);
        }
    }

    @Test
    public void testStoreByDeltaLargeOffsets() throws EfficientException {
        int[] vals = {0, 0x7FFFFFEE, 1, 0x7FFFFFDC, 2, 0x7FFFFF00};
        long s = 0x7FFFFFFFFFFFFF00L;
        long[] longVals = {s, 5, s + 0x18, 10, s + 0x30, s + 0x90};

        StoreByDeltaTypes[] arr = new StoreByDeltaTypes[vals.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new StoreByDeltaTypes(vals[i], longVals[i]);
        }

        byte[] data = EfficientStorage.serialize(new ArrSBDT(arr));

        ArrSBDT arrSBDT = (ArrSBDT) EfficientStorage.deserialize(ArrSBDT.class, data);
        for (int i = 0; i < vals.length; i++) {
            Assert.assertEquals(vals[i], arrSBDT.sbdt[i].a);
            Assert.assertEquals(longVals[i], arrSBDT.sbdt[i].b);
        }
    }
}
