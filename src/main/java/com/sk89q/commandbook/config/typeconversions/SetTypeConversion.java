package com.sk89q.commandbook.config.typeconversions;

import com.sk89q.commandbook.config.ConfigUtil;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zml2008
 */
public class SetTypeConversion extends TypeConversion {
    @Override
    protected Object cast(Class<?> target, Type[] neededGenerics, Object value) {
        Set<Object> values = new HashSet<Object>();
        Collection raw = (Collection) value;
        for (Object obj : raw) {
            values.add(ConfigUtil.smartCast(neededGenerics[0], obj));
        }
        return values;
    }

    @Override
    public boolean isApplicable(Class<?> target, Object value) {
        return target.equals(Set.class) && value instanceof Collection;
    }

    @Override
    public int getParametersRequired() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
