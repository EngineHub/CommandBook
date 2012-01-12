package com.sk89q.commandbook.config.typeconversions;

import java.lang.reflect.Type;

/**
 * @author zml2008
 */
public class BooleanTypeConversion extends TypeConversion {
    @Override
    protected Object cast(Class<?> target, Type[] neededGenerics, Object value) {
        if (value instanceof Boolean) {
            return value;
        } else {
            return Boolean.parseBoolean(value.toString());
        }
    }

    @Override
    public boolean isApplicable(Class<?> target, Object value) {
        return boolean.class.isAssignableFrom(target) || Boolean.class.isAssignableFrom(target);
    }

    @Override
    protected int getParametersRequired() {
        return 0;
    }
}
