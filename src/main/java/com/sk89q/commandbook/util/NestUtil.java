package com.sk89q.commandbook.util;

import java.util.*;

public class NestUtil {
    public static <T, K, V> Map<K, V> getNestedMap(Map<T, Map<K, V>> source, T key) {
        Map<K,V> value = source.get(key);
        if (value == null) {
            value = new HashMap<K, V>();
            source.put(key, value);
        }
        return value;
    }

    public static <T, V> Set<V> getNestedSet(Map<T, Set<V>> source, T key) {
        Set<V> value = source.get(key);
        if (value == null) {
            value = new HashSet<V>();
            source.put(key, value);
        }
        return value;
    }

    public static <T, V> List<V> getNestedList(Map<T, List<V>> source, T key) {
        List<V> value = source.get(key);
        if (value == null) {
            value = new ArrayList<V>();
            source.put(key, value);
        }
        return value;
    }
}
