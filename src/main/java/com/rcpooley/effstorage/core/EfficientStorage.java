package com.rcpooley.effstorage.core;

import com.rcpooley.effstorage.bitio.BitReader;
import com.rcpooley.effstorage.bitio.BitWriter;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class EfficientStorage {

    private static class FieldData {
        Field field;
        Efficient efficient;

        FieldData(Field field, Efficient efficient) {
            this.field = field;
            this.efficient = efficient;
        }
    }

    private static class TypeData {
        Class<?> objClass;
        Class<?> primClass;
        String funcName;
        Class<?> funcParam;

        TypeData(Class<?> objClass, Class<?> primClass, String funcName, Class<?> funcParam) {
            this.objClass = objClass;
            this.primClass = primClass;
            this.funcName = funcName;
            this.funcParam = funcParam;
        }

        TypeData(Class<?> objClass, Class<?> primClass, String funcName) {
            this.objClass = objClass;
            this.primClass = primClass;
            this.funcName = funcName;
            this.funcParam = primClass;
        }
    }

    private static Map<Class, EfficientSerializer> serializers = new HashMap<>();

    private static Map<Class, EfficientDeltaValue> deltaValues = new HashMap<>();

    static {
        TypeData[] types = {
                new TypeData(Integer.class, Integer.TYPE, "Int"),
                new TypeData(Byte.class, Byte.TYPE, "Byte", Integer.TYPE),
                new TypeData(Short.class, Short.TYPE, "Short", Integer.TYPE),
                new TypeData(Long.class, Long.TYPE, "Long"),
                new TypeData(Character.class, Character.TYPE, "Char", Integer.TYPE),
                new TypeData(Float.class, Float.TYPE, "Float"),
                new TypeData(Double.class, Double.TYPE, "Double"),
                new TypeData(Boolean.class, Boolean.TYPE, "Boolean")
        };

        // Handle primitive data types
        for (TypeData check : types) {
            EfficientSerializer es = new EfficientSerializer() {
                @Override
                public void serialize(Object obj, DataOutputStream dos) {
                    try {
                        Method m = dos.getClass().getMethod("write" + check.funcName, check.funcParam);
                        m.invoke(dos, obj);
                    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public Object deserialize(DataInputStream dis) {
                    try {
                        Method m = dis.getClass().getMethod("read" + check.funcName);
                        return m.invoke(dis);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };

            serializers.put(check.objClass, es);
            serializers.put(check.primClass, es);
        }

        // Handle strings
        serializers.put(String.class, new EfficientSerializer<String>() {
            @Override
            public void serialize(String obj, DataOutputStream dos) throws IOException {
                dos.writeInt(obj.length());
                dos.write(obj.getBytes());
            }

            @Override
            public String deserialize(DataInputStream dis) throws IOException {
                int len = dis.readInt();
                byte[] bytes = new byte[len];
                int r = dis.read(bytes);
                if (r != len) {
                    throw new RuntimeException("Failed to read " + len + " bytes for string");
                }
                return new String(bytes);
            }
        });

        // Set delta values
        EfficientDeltaValue edv = new EfficientDeltaValue<Integer>() {
            @Override
            public int getNumInitialBits() {
                return 32;
            }

            @Override
            public long[] getValues(Integer[] values) {
                long[] vals = new long[values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = values[i];
                return vals;
            }

            @Override
            public Integer[] convertValues(long[] values) {
                Integer[] vals = new Integer[values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = (int) values[i];
                return vals;
            }
        };
        deltaValues.put(Integer.class, edv);
        deltaValues.put(Integer.TYPE, edv);

        edv = new EfficientDeltaValue<Long>() {
            @Override
            public int getNumInitialBits() {
                return 64;
            }

            @Override
            public long[] getValues(Long[] values) {
                long[] vals = new long[values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = values[i];
                return vals;
            }

            @Override
            public Long[] convertValues(long[] values) {
                Long[] vals = new Long[values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = values[i];
                return vals;
            }
        };
        deltaValues.put(Long.class, edv);
        deltaValues.put(Long.TYPE, edv);
    }

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

        // Handle serializers
        if (serializers.containsKey(type)) {
            serializers.get(type).serialize(obj, dos);
            return;
        }

        // Handle EfficientSerializable interfaces
        if (obj instanceof EfficientSerializable) {
            EfficientSerializable es = (EfficientSerializable) obj;
            es.serialize(dos);
            return;
        }

        // Handle arrays
        if (type.isArray()) {
            int len = Array.getLength(obj);
            dos.writeInt(len);
            for (int i = 0; i < len; i++) {
                serialize(Array.get(obj, i), dos);
            }

            // Store storeByDelta fields
            List<FieldData> deltaFields = getEfficientFields(type.getComponentType())
                    .stream()
                    .filter(data -> data.efficient.storeByDelta())
                    .collect(Collectors.toList());

            BitWriter bw = new BitWriter(dos);
            for (FieldData data : deltaFields) {
                Class<?> dataType = data.field.getType();

                long[] vals = new long[len];
                boolean isInt = dataType == Integer.TYPE || dataType == Integer.class;

                if (dataType == Double.TYPE || dataType == Double.class) {

                    for (int i = 0; i < len; i++) {
                        double d = (double) data.field.get(Array.get(obj, i));
                        vals[i] = (int) (d * Math.pow(10, precision));
                    }
                } else if (isInt) {
                    for (int i = 0; i < len; i++) {
                        vals[i] = (int) data.field.get(Array.get(obj, i));
                    }
                }  else if (dataType == Long.TYPE || dataType == Long.class) {
                    for (int i = 0; i < len; i++) {
                        vals[i] = (long) data.field.get(Array.get(obj, i));
                    }
                } else {
                    throw new EfficientException("Field " + data.field.getName() + " in class " + type.getName() + " is marked as storeByDelta, but is not of type double, long, or int");
                }

                int offsetBits = 1;
                long maxOffset = 1 << (offsetBits - 1);
                long[] offsets = new long[vals.length - 1];
                for (int i = 0; i < len - 1; i++) {
                    offsets[i] = vals[i + 1] - vals[i];
                    while (offsetBits < 64 && (offsets[i] >= maxOffset || offsets[i] < -maxOffset)) {
                        offsetBits++;
                        maxOffset = 1L << (offsetBits - 1);
                    }
                }

                // Write number of offset bits
                bw.writeBits(offsetBits, 8);

                if (offsetBits == 64) {
                    for (long val : vals) {
                        bw.writeBits(val, 64);
                    }
                } else {
                    // Write the first value
                    bw.writeBits(vals[0], isInt ? 32 : 64);

                    // Write the offsets
                    for (long o : offsets) {
                        if (o < 0) {
                            bw.writeBits(1, 1);
                            bw.writeBits(maxOffset + o, offsetBits - 1);
                        } else {
                            bw.writeBits(0, 1);
                            bw.writeBits(o, offsetBits - 1);
                        }
                    }
                }
            }
            bw.finish();

            return;
        }

        // Handle other Efficient classes
        if (type.isAnnotationPresent(Efficient.class)) {
            // Get all fields annotated with @Efficient
            List<FieldData> fields = getEfficientFields(type)
                    .stream()
                    .filter(data -> !data.efficient.storeByDelta())
                    .collect(Collectors.toList());

            // Now write the value of each field
            for (FieldData data : fields) {
                Field field = data.field;
                field.setAccessible(true);
                serialize(field.get(obj), dos);
                field.setAccessible(false);
            }
            return;
        }

        // Throw exception for unrecognized type
        throw new EfficientException("Unrecognized field type: " + type.getName());
    }

    private static Object deserialize(Class<?> type, DataInputStream dis) throws IOException, EfficientException, IllegalAccessException, InvocationTargetException, InstantiationException {
        // Handle serializers
        if (serializers.containsKey(type)) {
            return serializers.get(type).deserialize(dis);
        }

        // Handle arrays
        if (type.isArray()) {
            int len = dis.readInt();
            Object arr = Array.newInstance(type.getComponentType(), len);
            for (int i = 0; i < len; i++) {
                Array.set(arr, i, deserialize(type.getComponentType(), dis));
            }

            // Retrieve storeByDelta fields
            List<FieldData> deltaFields = getEfficientFields(type.getComponentType())
                    .stream()
                    .filter(data -> data.efficient.storeByDelta())
                    .collect(Collectors.toList());

            BitReader br = new BitReader(dis);
            for (FieldData data : deltaFields) {
                Class<?> dataType = data.field.getType();

                boolean isInt = dataType == Integer.TYPE || dataType == Integer.class;

                int precision = 0;

                if (dataType == Double.TYPE || dataType == Double.class) {
                    precision = br.readBits(8);
                }

                int offsetBits = br.readBits(8);
                long maxOffset = (long) Math.pow(2, offsetBits) / 2;

                // Read values
                long[] vals = new long[len];

                if (offsetBits == 64) {
                    for (int i = 0; i < len; i++) {
                        vals[i] = br.readBitsLong(64);
                    }
                } else {
                    // Read first value
                    vals[0] = br.readBitsLong(isInt ? 32 : 64);

                    // Read each offset
                    for (int i = 1; i < len; i++) {
                        boolean negative = br.readBits(1) == 1;
                        long val = br.readBitsLong(offsetBits - 1);
                        if (negative) {
                            val -= maxOffset;
                        }
                        vals[i] = vals[i - 1] + val;
                    }
                }

                for (int i = 0; i < len; i++) {
                    Object val;
                    if (dataType == Double.TYPE || dataType == Double.class) {
                        val = vals[i] * 1.0 / Math.pow(10, precision);
                    } else if (isInt) {
                        val = (int) vals[i];
                    } else if (dataType == Long.TYPE || dataType == Long.class) {
                        val = vals[i];
                    } else {
                        throw new EfficientException("Field " + data.field.getName() + " in class " + type.getName() + " is marked as storeByDelta, but is not of type double, long, or int");
                    }

                    data.field.set(Array.get(arr, i), val);
                }
            }

            return arr;
        }

        // Handle other Efficient classes
        if (type.isAnnotationPresent(Efficient.class)) {
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
                ((EfficientSerializable) obj).deserialize(dis);
            } else {
                // Get all fields annotated with @Efficient
                List<FieldData> fields = getEfficientFields(type)
                        .stream()
                        .filter(data -> !data.efficient.storeByDelta())
                        .collect(Collectors.toList());

                // Read field values
                for (FieldData data : fields) {
                    Field field = data.field;
                    field.setAccessible(true);
                    field.set(obj, deserialize(field.getType(), dis));
                    field.setAccessible(false);
                }
            }

            return obj;
        }

        // Throw exception for unrecognized type
        throw new EfficientException("Unrecognized field type: " + type.getName());
    }

    private static List<FieldData> getEfficientFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Efficient.class))
                .sorted(Comparator.comparing(Field::getName))
                .map(field -> {
                    Efficient eff = field.getAnnotation(Efficient.class);
                    return new FieldData(field, eff);
                })
                .collect(Collectors.toList());
    }
}
