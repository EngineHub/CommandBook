package com.sk89q.commandbook.config.typeconversions;

import java.lang.reflect.Type;

/**
 * @author zml2008
 */
public class StringTypeConversion extends TypeConversion {
    @Override
    protected Object cast(Class<?> target, Type[] neededGenerics, Object value) {
        return value.toString();
    }

    @Override
    public boolean isApplicable(Class<?> target, Object value) {
        return String.class.isAssignableFrom(target);
    }

    @Override
    public int getParametersRequired() {
        return 0;
    }
}
