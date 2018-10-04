package com.rcpooley.effstorage.test;

import com.rcpooley.effstorage.core.EfficientException;
import com.rcpooley.effstorage.core.EfficientStorage;
import com.rcpooley.effstorage.test.structs.*;
import org.junit.Assert;
import org.junit.Test;

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
}
