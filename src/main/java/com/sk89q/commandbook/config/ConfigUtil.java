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

import com.sk89q.commandbook.config.typeconversions.*;
import com.sk89q.util.yaml.YAMLNode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author zml2008
 */
public class ConfigUtil {
    private static final List<TypeConversion> typeConversions = new ArrayList<TypeConversion>(
            Arrays.asList(new SameTypeConversion(),
            new StringTypeConversion(),
            new BooleanTypeConversion(),
            new NumberTypeConversion(),
            new SetTypeConversion(),
            new ListTypeConversion(),
            new MapTypeConversion()
    ));

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
        
        for (TypeConversion conversion : typeConversions) {
            if ((ret = conversion.handle(target, neededGenerics, value)) != null) {
                break;
            }
        }
        
        return ret;
    }
    
    public static void registerTypeConversion(TypeConversion conversion) {
        typeConversions.add(conversion);
    }

    @SuppressWarnings("unchecked")
    public static Object prepareSerialization(Object obj) {
        if (obj instanceof Collection) {
            obj = new ArrayList((Collection)obj);
        }
        return obj;
    }

    public static YAMLNode getNode(YAMLNode parent, String path) {
        YAMLNode ret = parent.getNode(path);
        if (ret == null) {
            ret = parent.addNode(path);
        }
        return ret;
    }
}
