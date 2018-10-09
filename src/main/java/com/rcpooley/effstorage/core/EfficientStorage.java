package com.rcpooley.effstorage.core;

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

    private static TypeData[] types = new TypeData[]{
            new TypeData(Integer.class, Integer.TYPE, "Int"),
            new TypeData(Byte.class, Byte.TYPE, "Byte", Integer.TYPE),
            new TypeData(Short.class, Short.TYPE, "Short", Integer.TYPE),
            new TypeData(Long.class, Long.TYPE, "Long"),
            new TypeData(Character.class, Character.TYPE, "Char", Integer.TYPE),
            new TypeData(Float.class, Float.TYPE, "Float"),
            new TypeData(Double.class, Double.TYPE, "Double"),
            new TypeData(Boolean.class, Boolean.TYPE, "Boolean")
    };

    private static Map<Object, Map<String, Integer>> precisionMap = new HashMap<>();

    private static int getPrecision(Object array, String componentField) throws EfficientException {
        Map<String, Integer> map = precisionMap.getOrDefault(array, new HashMap<>());
        if (!map.containsKey(componentField)) {
            throw new EfficientException("Decimal precision not set for field " + componentField + " in class " + array.getClass().getComponentType().getName());
        }
        int val = map.remove(componentField);
        if (map.size() == 0) precisionMap.remove(array);
        return val;
    }

    public static void setPrecision(Object array, String componentField, int numDecimals) {
        Map<String, Integer> map = precisionMap.getOrDefault(array, new HashMap<>());
        map.put(componentField, numDecimals);
        precisionMap.put(array, map);
    }

    public static byte[] serialize(Object object) throws EfficientException {
        // Create byte stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Serialize the object
        try {
            serialize(object, dos);
        } catch (IOException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new EfficientException(e);
        }
    }

    private static void serialize(Object obj, DataOutputStream dos) throws IOException, IllegalAccessException, EfficientException, NoSuchMethodException, InvocationTargetException {
        Class<?> type = obj.getClass();

        // Handle primitive data types
        for (TypeData check : types) {
            if (type == check.objClass) {
                Method m = dos.getClass().getMethod("write" + check.funcName, check.funcParam);
                m.invoke(dos, obj);
                return;
            }
        }

        // Handle Strings
        if (type == String.class) {
            String s = (String) obj;
            dos.writeInt(s.length());
            dos.write(s.getBytes());
            return;
        }

        // Handle EfficientSerializable interfaces
        if (obj instanceof EfficientSerializable) {
            EfficientSerializable es = (EfficientSerializable) obj;
            dos.write(es.serialize());
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
                if (dataType != Double.TYPE && dataType != Double.class) {
                    throw new EfficientException("Field " + data.field.getName() + " in class " + type.getName() + " is marked as storeByDelta, but is not of type double");
                }

                int precision = getPrecision(obj, data.field.getName());

                // Write number of decimal places
                bw.writeBits(precision, 8);

                int[] vals = new int[len];
                for (int i = 0; i < len; i++) {
                    double d = (double) data.field.get(Array.get(obj, i));
                    vals[i] = (int) (d * Math.pow(10, precision));
                }

                int offsetBits = 1;
                int maxOffset = (int) Math.pow(2, offsetBits) / 2;
                int[] offsets = new int[vals.length - 1];
                for (int i = 0; i < len - 1; i++) {
                    offsets[i] = vals[i + 1] - vals[i];
                    while (offsets[i] >= maxOffset || offsets[i] < -maxOffset) {
                        offsetBits++;
                        maxOffset = (int) Math.pow(2, offsetBits) / 2;
                    }
                }

                // Write number of offset bits
                bw.writeBits(offsetBits, 8);

                // Write the first value
                bw.writeBits(vals[0], 32);

                // Write the offsets
                for (int o : offsets) {
                    if (o < 0) {
                        bw.writeBits(1, 1);
                        bw.writeBits(maxOffset + o, offsetBits - 1);
                    } else {
                        bw.writeBits(0, 1);
                        bw.writeBits(o, offsetBits - 1);
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

    private static Object deserialize(Class<?> type, DataInputStream dis) throws IOException, EfficientException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        // Handle primitive data types
        for (TypeData check : types) {
            if (type == check.primClass) {
                Method m = dis.getClass().getMethod("read" + check.funcName);
                return m.invoke(dis);
            }
        }

        // Handle Strings
        if (type == String.class) {
            int len = dis.readInt();
            byte[] bytes = new byte[len];
            int r = dis.read(bytes);
            if (r != len) {
                throw new RuntimeException("Failed to read " + len + " bytes for string");
            }
            return new String(bytes);
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
                int precision = br.readBits(8);
                int offsetBits = br.readBits(8);
                int maxOffset = (int) Math.pow(2, offsetBits) / 2;
                int[] vals = new int[len];
                vals[0] = br.readBits(32);
                for (int i = 1; i < len; i++) {
                    boolean negative = br.readBits(1) == 1;
                    int val = br.readBits(offsetBits - 1);
                    if (negative) {
                        val -= maxOffset;
                    }
                    vals[i] = vals[i - 1] + val;
                }

                for (int i = 0; i < len; i++) {
                    data.field.set(Array.get(arr, i), vals[i] * 1.0 / Math.pow(10, precision));
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
                ((EfficientSerializable)obj).deserialize(dis);
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
