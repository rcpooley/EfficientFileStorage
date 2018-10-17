package com.rcpooley.effstorage.core;

import com.rcpooley.effstorage.bitio.BitReader;
import com.rcpooley.effstorage.bitio.BitWriter;

import static com.rcpooley.effstorage.core.EfficientDeltaValue.Values;

import java.io.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

        EfficientDeltaValue edv = new EfficientDeltaValue() {
            @Override
            public int getNumInitialBits() {
                return 32;
            }

            @Override
            public Values getValues(Object[] values) {
                long[] vals = new long[values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = (int) values[i];
                return new Values(vals);
            }

            @Override
            public Object[] convertValues(Values v) {
                Object[] vals = new Integer[v.values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = (int) v.values[i];
                return vals;
            }
        };
        deltaValues.put(Integer.class, edv);
        deltaValues.put(Integer.TYPE, edv);

        edv = new EfficientDeltaValue() {
            @Override
            public int getNumInitialBits() {
                return 64;
            }

            @Override
            public Values getValues(Object[] values) {
                long[] vals = new long[values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = (long) values[i];
                return new Values(vals);
            }

            @Override
            public Object[] convertValues(Values v) {
                Object[] vals = new Long[v.values.length];
                for (int i = 0; i < vals.length; i++) vals[i] = v.values[i];
                return vals;
            }
        };
        deltaValues.put(Long.class, edv);
        deltaValues.put(Long.TYPE, edv);

        EfficientDeltaValue bigDecimalEdv = new EfficientDeltaValue() {
            @Override
            public int getNumInitialBits() {
                return 64;
            }

            @Override
            public Values getValues(Object[] vls) {
                BigDecimal[] values = new BigDecimal[vls.length];
                for (int i = 0; i < vls.length; i++) {
                    values[i] = (BigDecimal) vls[i];
                }

                int maxScale = 0;
                for (BigDecimal bd : values) {
                    if (bd.scale() > maxScale) maxScale = bd.scale();
                }

                long[] vals = new long[values.length];
                for (int i = 0; i < values.length; i++) {
                    vals[i] = values[i].setScale(maxScale, RoundingMode.UNNECESSARY).unscaledValue().longValue();
                }

                return new Values(vals, maxScale);
            }

            @Override
            public Object[] convertValues(Values v) {
                Object[] vals = new BigDecimal[v.values.length];

                int scale = v.scale;

                for (int i = 0; i < v.values.length; i++) {
                    vals[i] = BigDecimal.valueOf(v.values[i], scale);
                }

                return vals;
            }

            @Override
            public boolean useScale() {
                return true;
            }
        };
        deltaValues.put(BigDecimal.class, bigDecimalEdv);

        edv = new EfficientDeltaValue() {
            @Override
            public int getNumInitialBits() {
                return 64;
            }

            @Override
            public Values getValues(Object[] values) throws IOException {
                BigDecimal[] vals = new BigDecimal[values.length];
                for (int i = 0; i < values.length; i++) {
                    vals[i] = BigDecimal.valueOf((double)values[i]);
                }
                return bigDecimalEdv.getValues(vals);
            }

            @Override
            public Object[] convertValues(Values v) throws IOException {
                Object[] bd = bigDecimalEdv.convertValues(v);
                Object[] vals = new Double[v.values.length];
                for (int i = 0; i < vals.length; i++) {
                    vals[i] = ((BigDecimal)bd[i]).doubleValue();
                }
                return vals;
            }

            @Override
            public boolean useScale() {
                return true;
            }
        };
        deltaValues.put(Double.class, edv);
        deltaValues.put(Double.TYPE, edv);
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
        if (obj == null) {
            throw new EfficientException("Tried to serialize null object");
        }

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

            if (len == 0) return;

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
                data.field.setAccessible(true);
                Class<?> dataType = data.field.getType();

                EfficientDeltaValue edv = deltaValues.get(dataType);
                if (edv == null) {
                    throw new EfficientException("Field " + data.field.getName() + " in class " + type.getName() + " is marked as storeByDelta, but is not a recognized delta type");
                }

                // Get array of the raw values of this delta field
                Object[] rawVals = new Object[len];
                for (int i = 0; i < len; i++) {
                    rawVals[i] = data.field.get(Array.get(obj, i));
                }

                // Get the unscaled values
                Values v = edv.getValues(rawVals);
                long[] vals = v.values;

                // Calculate the offsets
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
                    bw.writeBits(vals[0], edv.getNumInitialBits());

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

                // Write the scale
                if (edv.useScale()) bw.writeBits(v.scale, 32);

                data.field.setAccessible(false);
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

            if (len == 0) return arr;

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
                data.field.setAccessible(true);

                Class<?> dataType = data.field.getType();

                EfficientDeltaValue edv = deltaValues.get(dataType);
                if (edv == null) {
                    throw new EfficientException("Field " + data.field.getName() + " in class " + type.getName() + " is marked as storeByDelta, but is not a recognized delta type");
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
                    vals[0] = br.readBitsLong(edv.getNumInitialBits());

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

                int scale = edv.useScale() ? br.readBits(32) : 0;

                Object[] rawValues = edv.convertValues(new Values(vals, scale));
                for (int i = 0; i < len; i++) {
                    data.field.set(Array.get(arr, i), rawValues[i]);
                }

                data.field.setAccessible(false);
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
