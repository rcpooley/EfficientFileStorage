package com.rcpooley.effstorage.core;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EfficientStorage {

    public static byte[] serialize(Object object) throws EfficientException {
        // Create byte stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Serialize the object
        try {
            serialize(object, dos);
        } catch (IOException | IllegalAccessException e) {
            throw new EfficientException(e);
        }

        // Return the final byte array
        return baos.toByteArray();
    }

    public static Object deserialize(Class<?> clazz, byte[] data) throws EfficientException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        try {
            return deserialize(clazz, dis);
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new EfficientException(e);
        }
    }

    private static void serialize(Object obj, DataOutputStream dos) throws IOException, IllegalAccessException, EfficientException {
        Class<?> type = obj.getClass();

        if (obj instanceof EfficientSerializable) {
            EfficientSerializable es = (EfficientSerializable) obj;
            dos.write(es.serialize());
            return;
        } else if (type == Integer.class) {
            dos.writeInt((int) obj);
            return;
        } else if (type == String.class) {
            String s = (String) obj;
            dos.writeInt(s.length());
            dos.write(s.getBytes());
            return;
        } else if (type == Byte.class) {
            dos.writeByte((byte) obj);
            return;
        } else if (type == Short.class) {
            dos.writeShort((short) obj);
            return;
        } else if (type == Long.class) {
            dos.writeLong((long) obj);
            return;
        } else if (type == Character.class) {
            dos.writeChar((char) obj);
            return;
        } else if (type == Float.class) {
            dos.writeFloat((float) obj);
            return;
        } else if (type == Double.class) {
            dos.writeDouble((double) obj);
            return;
        } else if (type == Boolean.class) {
            dos.writeBoolean((boolean) obj);
            return;
        } else if (type.isArray()) {
            int len = Array.getLength(obj);
            dos.writeInt(len);
            for (int i = 0; i < len; i++) {
                serialize(Array.get(obj, i), dos);
            }
            return;
        } else if (type.isAnnotationPresent(Efficient.class)) {
            // Get all fields annotated with @Efficient
            List<Field> fields = getEfficientFields(type);

            // Now write the value of each field
            for (Field field : fields) {
                field.setAccessible(true);
                serialize(field.get(obj), dos);
                field.setAccessible(false);
            }
            return;
        }

        throw new EfficientException("Unrecognized field type: " + type.getName());
    }

    private static Object deserialize(Class<?> type, DataInputStream dis) throws IOException, EfficientException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (type == Integer.TYPE) {
            return dis.readInt();
        } else if (type == String.class) {
            int len = dis.readInt();
            byte[] bytes = new byte[len];
            int r = dis.read(bytes);
            if (r != len) {
                throw new RuntimeException("Failed to read " + len + " bytes for string");
            }
            return new String(bytes);
        } else if (type == Byte.TYPE) {
            return dis.readByte();
        } else if (type == Short.TYPE) {
            return dis.readShort();
        } else if (type == Long.TYPE) {
            return dis.readLong();
        } else if (type == Character.TYPE) {
            return dis.readChar();
        } else if (type == Float.TYPE) {
            return dis.readFloat();
        } else if (type == Double.TYPE) {
            return dis.readDouble();
        } else if (type == Boolean.TYPE) {
            return dis.readBoolean();
        } else if (type.isArray()) {
            int len = dis.readInt();
            Object arr = Array.newInstance(type.getComponentType(), len);
            for (int i = 0; i < len; i++) {
                Array.set(arr, i, deserialize(type.getComponentType(), dis));
            }
            return arr;
        } else if (type.isAnnotationPresent(Efficient.class)) {
            // Get the default constructor
            Constructor c;
            try {
                c = type.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new EfficientException("No default constructor found for class " + type.getName());
            }

            // Create new instance of clazz
            c.setAccessible(true);
            Object obj = c.newInstance();
            c.setAccessible(false);

            if (EfficientSerializable.class.isAssignableFrom(type)) {
                ((EfficientSerializable)obj).deserialize(dis);
            } else {
                // Get all fields annotated with @Efficient
                List<Field> fields = getEfficientFields(type);

                // Read field values
                for (Field field : fields) {
                    field.setAccessible(true);
                    field.set(obj, deserialize(field.getType(), dis));
                    field.setAccessible(false);
                }
            }

            return obj;
        }

        throw new EfficientException("Unrecognized field type: " + type.getName());
    }

    private static List<Field> getEfficientFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Efficient.class))
                .sorted(Comparator.comparing(Field::getName))
                .collect(Collectors.toList());
    }
}
