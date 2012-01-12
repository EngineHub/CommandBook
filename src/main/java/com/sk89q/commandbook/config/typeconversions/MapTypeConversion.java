package com.sk89q.commandbook.config.typeconversions;

import com.sk89q.commandbook.config.ConfigUtil;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zml2008
 */
public class MapTypeConversion extends TypeConversion {
    @Override
    @SuppressWarnings("unchecked")
    protected Object cast(Class<?> target, Type[] neededGenerics, Object value) {
        Map<Object, Object> raw = (Map<Object, Object>) value;
        Map<Object, Object> values = new HashMap<Object, Object>();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            values.put(ConfigUtil.smartCast(neededGenerics[0], entry.getKey()), ConfigUtil.smartCast(neededGenerics[1], entry.getValue()));
        }
        return values;
    }

    @Override
    public boolean isApplicable(Class<?> target, Object value) {
        return target.equals(Map.class) && value instanceof Map;
    }

    @Override
    protected int getParametersRequired() {
        return 2;
    }
}
