package com.sk89q.commandbook.config.typeconversions;

import java.lang.reflect.Type;

/**
 * @author zml2008
 */
public class SameTypeConversion extends TypeConversion {
    
    @Override
    protected Object cast(Class<?> target, Type[] neededGenerics, Object value) {
        return value;
    }

    @Override
    public boolean isApplicable(Class<?> target, Object value) {
        return target.isInstance(value);
    }

    @Override
    public int getParametersRequired() {
        return 0;
    }
}
