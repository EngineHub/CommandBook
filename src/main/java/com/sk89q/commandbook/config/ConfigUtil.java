/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.config;

import com.sk89q.util.yaml.YAMLNode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author zml2008
 */
public class ConfigUtil {
    public static YAMLNode getNode(YAMLNode parent, String path) {
        YAMLNode ret = parent.getNode(path);
        if (ret == null) {
            ret = parent.addNode(path);
        }
        return ret;
    }

    public static Object smartCast(Type genericType, Object value) {
        if (value == null) {
            return null;
        }
        Type[] neededGenerics;
        Class target = null;
        if (genericType != null && genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            Type raw = type.getRawType();
            if (raw instanceof Class) {
                target = (Class)raw;
            }
            neededGenerics = type.getActualTypeArguments();
        } else {
            if (genericType instanceof Class) {
                target = (Class)genericType;
            }
            neededGenerics = new Type[0];
        }

        if (target == null) return null;

        Object ret = null;
        // Is it already the correct type?
        if (neededGenerics.length == 0) {
            if (target.isInstance(value)) {
                ret = value;
            } else if (String.class.isAssignableFrom(target)) {
                ret = value.toString();
            } else if (boolean.class.isAssignableFrom(target) || Boolean.class.isAssignableFrom(target)) {
                if (value instanceof Boolean) {
                    ret = value;
                } else {
                    ret = Boolean.parseBoolean(value.toString());
                }
            } else if (target.isPrimitive() || Number.class.isAssignableFrom(target)) {
                ret = getNumber(target, (Number)value);
            }
        } else if (neededGenerics.length == 1) {
            if (target.equals(List.class) && value instanceof Collection) {
                List<Object> values = new ArrayList<Object>();
                Collection raw = (Collection) value;
                for (Object obj : raw) {
                    values.add(smartCast(neededGenerics[0], obj));
                }
                ret = values;
            } else if (target.equals(Set.class) && value instanceof Collection) {
                Set<Object> values = new HashSet<Object>();
                Collection raw = (Collection) value;
                for (Object obj : raw) {
                    values.add(smartCast(neededGenerics[0], obj));
                }
                ret = values;
            }
        } else if (neededGenerics.length == 2) {
            if (target.equals(Map.class) && value instanceof Map) {
                Map<Object, Object> raw = (Map<Object, Object>) value;
                Map<Object, Object> values = new HashMap<Object, Object>();
                for (Map.Entry<Object, Object> entry : raw.entrySet()) {
                    values.put(smartCast(neededGenerics[0], entry.getKey()), smartCast(neededGenerics[1], entry.getValue()));
                }
                ret = values;
            }
        }
        return ret;
    }

    public static Number getNumber(Class<?> target, Number value) {
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
    
    public static Object prepareSerialization(Object obj) {
        if (obj instanceof Collection) {
            obj = new ArrayList((Collection)obj);
        }
        return obj;
    }
}
