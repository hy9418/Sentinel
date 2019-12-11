package com.alibaba.csp.sentinel.support;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/9 17:18
 */
public class PrimitiveWrapper {

    private static final List<Class<?>> wrapperTypes = new ArrayList<>(8);

    static {
        wrapperTypes.add(Boolean.class);
        wrapperTypes.add(Byte.class);
        wrapperTypes.add(Character.class);
        wrapperTypes.add(Double.class);
        wrapperTypes.add(Float.class);
        wrapperTypes.add(Integer.class);
        wrapperTypes.add(Long.class);
        wrapperTypes.add(Short.class);
    }

    public static <T> Object resolveWrapper(T raw) {
        if (raw != null && needResolve(raw.getClass())) {
            if (raw instanceof Boolean) {
                return Boolean.parseBoolean(raw.toString());
            } else if (raw instanceof Byte) {
                return Byte.parseByte(raw.toString());
            } else if (raw instanceof Character) {
                return ((Character) raw).charValue();
            } else if (raw instanceof Double) {
                return Double.parseDouble(raw.toString());
            } else if (raw instanceof Float) {
                return Float.parseFloat(raw.toString());
            } else if (raw instanceof Integer) {
                return Integer.parseInt(raw.toString());
            } else if (raw instanceof Long) {
                return Long.parseLong(raw.toString());
            } else if (raw instanceof Short) {
                return Short.parseShort(raw.toString());
            }
        }
        return raw;
    }

    public static <T> boolean needResolve(Class<T> clazz) {
        return clazz != null && wrapperTypes.contains(clazz);
    }

}
