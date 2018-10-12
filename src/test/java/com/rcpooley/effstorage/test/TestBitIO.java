package com.rcpooley.effstorage.test;

import com.rcpooley.effstorage.core.BitReader;
import com.rcpooley.effstorage.core.BitUtil;
import com.rcpooley.effstorage.core.BitWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TestBitIO {

    @Test
    public void testSimpleIO() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitWriter writer = new BitWriter(baos);
        for (int i = 0; i < 70; i++) {
            writer.writeBits(i, 3);
        }
        writer.finish();
        byte[] data = baos.toByteArray();
        Assert.assertEquals(27, data.length);

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BitReader reader = new BitReader(bais);
        for (int i = 0; i < 70; i++) {
            Assert.assertEquals(i & BitUtil.getMask(3), reader.readBits(3));
        }
    }

    @Test
    public void testMultipleOf8() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitWriter writer = new BitWriter(baos);
        writer.writeBits(999, 10);
        writer.writeBits(22, 6);
        writer.finish();
        byte[] data = baos.toByteArray();
        Assert.assertEquals(2, data.length);
        BitReader reader = new BitReader(new ByteArrayInputStream(data));
        Assert.assertEquals(999, reader.readBits(10));
        Assert.assertEquals(22, reader.readBits(6));
    }

    @Test
    public void testLongs() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitWriter writer = new BitWriter(baos);
        writer.writeBits(0xABCDEFEDCBA98765L, 64);
        writer.writeBits(0xAAAAAABBCCDDEEFFL, 5 * 8);
        writer.writeBits(999, 10);
        writer.writeBits(0x1234, 8);
        writer.finish();
        byte[] data = baos.toByteArray();
        BitReader reader = new BitReader(new ByteArrayInputStream(data));
        Assert.assertEquals(0xABCDEFED, reader.readBits(32));
        Assert.assertEquals(0xCBA98765BBCCDDEEL, reader.readBitsLong(64));
        Assert.assertEquals(0xFF, reader.readBitsLong(8));
        Assert.assertEquals(999, reader.readBits(10));
        Assert.assertEquals(0x34, reader.readBits(8));
    }
}
