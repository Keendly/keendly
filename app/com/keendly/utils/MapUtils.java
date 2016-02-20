package com.keendly.utils;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * map creating utility methods copied from
 * http://minborgsjavapot.blogspot.com.es/2014/12/java-8-initializing-maps-in-smartest-way.html
 */
public class MapUtils {


    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue());
    }
}
