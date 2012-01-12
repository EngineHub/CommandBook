package com.sk89q.commandbook.config.typeconversions;

import java.lang.reflect.Type;

/**
 * @author zml2008
 */
public abstract class TypeConversion implements Comparable<TypeConversion> {
    public Object handle(Class<?> target, Type[] neededGenerics, Object value) {
        if (isApplicable(target, value) && neededGenerics.length == getParametersRequired()) {
            return cast(target, neededGenerics, value);
        } else {
            return null;
        }
    }
    
    protected abstract Object cast(Class<?> target, Type[] neededGenerics, Object value);
    public abstract boolean isApplicable(Class<?> target, Object value);
    
    protected abstract int getParametersRequired();
    
    public int compareTo(TypeConversion other) {
        return Integer.valueOf(getParametersRequired()).compareTo(other.getParametersRequired());
    }
}
