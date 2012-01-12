package com.sk89q.commandbook.config.typeconversions;

import java.lang.reflect.Type;

/**
 * @author zml2008
 */
public class NumberTypeConversion extends TypeConversion {
    @Override
    protected Object cast(Class<?> target, Type[] neededGenerics, Object rawVal) {
        Number value = (Number)rawVal;
        // Wrapper classes are evil!
        if (target.equals(Number.class)) {
            return value;
        } else if (target.equals(int.class) || target.equals(Integer.class)) {
            if (value instanceof Integer) {
                return value;
            } else {
                return value.intValue();
            }
        } else if (target.equals(byte.class) || target.equals(Byte.class)) {
            if (value instanceof Byte) {
                return value;
            } else {
                return value.byteValue();
            }
        } else if (target.equals(long.class) || target.equals(Long.class)) {
            if (value instanceof Long) {
                return value;
            } else {
                return value.longValue();
            }
        } else if (target.equals(double.class) || target.equals(Double.class)) {
            if (value instanceof Double) {
                return value;
            } else {
                return value.doubleValue();
            }
        } else if (target.equals(float.class) || target.equals(Float.class)) {
            if (value instanceof Float) {
                return value;
            } else {
                return value.floatValue();
            }
        } else if (target.equals(short.class) || target.equals(Short.class)) {
            if (value instanceof Short) {
                return value;
            } else {
                return value.shortValue();
            }
        }
        return null;
    }

    @Override
    public boolean isApplicable(Class<?> target, Object value) {
        return (target.isPrimitive() || Number.class.isAssignableFrom(target)) && !boolean.class.isAssignableFrom(target);
    }

    @Override
    public int getParametersRequired() {
        return 0;
    }
}
