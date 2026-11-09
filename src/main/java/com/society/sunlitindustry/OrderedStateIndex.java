package com.society.sunlitindustry;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Indexes a fixed state-property domain using ordered values instead of Map.hashCode's unordered sum.
 * Swapping boolean/integer values between properties otherwise creates large collision groups.
 * Keys and values are kept in source iteration order. The lookup table stores integer positions,
 * avoiding a second map, a list and an object array for every state.
 */
public final class OrderedStateIndex<V> extends AbstractMap<Map<?, ?>, V> {
    private final List<?> properties;
    private final Map<?, ?>[] keys;
    private final Object[] values;
    private final int[] slots;
    private final Set<Entry<Map<?, ?>, V>> entries = new AbstractSet<>() {
        @Override public int size() { return keys.length; }
        @Override public Iterator<Entry<Map<?, ?>, V>> iterator() {
            return new Iterator<>() {
                private int next;
                @Override public boolean hasNext() { return next < keys.length; }
                @Override public Entry<Map<?, ?>, V> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    int position = next++;
                    return new SimpleImmutableEntry<>(keys[position], valueAt(position));
                }
            };
        }
    };

    public OrderedStateIndex(Map<? extends Map<?, ?>, V> source) {
        properties = source.isEmpty() ? List.of() : new ArrayList<>(source.keySet().iterator().next().keySet());
        keys = new Map<?, ?>[source.size()];
        values = new Object[source.size()];
        int capacity = 1;
        while (capacity < Math.max(2L, source.size() * 2L)) {
            if (capacity == (1 << 30)) throw new IllegalArgumentException("State domain is too large");
            capacity <<= 1;
        }
        slots = new int[capacity];
        int position = 0;
        for (var entry : source.entrySet()) {
            Map<?, ?> key = entry.getKey();
            if (!matchesDomain(key)) throw new IllegalArgumentException("Inconsistent state property domain");
            keys[position] = key;
            values[position] = entry.getValue();
            int slot = hash(key, -1, null) & (slots.length - 1);
            while (slots[slot] != 0) slot = (slot + 1) & (slots.length - 1);
            slots[slot] = ++position;
        }
    }

    private boolean matchesDomain(Map<?, ?> state) {
        if (state.size() != properties.size()) return false;
        for (Object property : properties) {
            if (!state.containsKey(property)) return false;
        }
        return true;
    }

    private int hash(Map<?, ?> state, int changed, Object replacement) {
        int hash = 1;
        for (int i = 0; i < properties.size(); i++) {
            Object value = i == changed ? replacement : state.get(properties.get(i));
            hash = 31 * hash + Objects.hashCode(value);
        }
        return hash ^ (hash >>> 16);
    }

    private int find(Map<?, ?> state, int changed, Object replacement) {
        if (!matchesDomain(state)) return -1;
        int slot = hash(state, changed, replacement) & (slots.length - 1);
        while (slots[slot] != 0) {
            int position = slots[slot] - 1;
            Map<?, ?> candidate = keys[position];
            boolean matches = true;
            for (int i = 0; i < properties.size(); i++) {
                Object property = properties.get(i);
                Object requested = i == changed ? replacement : state.get(property);
                if (!Objects.equals(candidate.get(property), requested)) {
                    matches = false;
                    break;
                }
            }
            if (matches) return position;
            slot = (slot + 1) & (slots.length - 1);
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int position) {
        return (V) values[position];
    }

    public V with(Map<?, ?> state, Object property, Object replacement) {
        int position = properties.indexOf(property);
        if (position < 0) return null;
        int found = find(state, position, replacement);
        return found < 0 ? null : valueAt(found);
    }

    @Override
    public V get(Object key) {
        if (!(key instanceof Map<?, ?> state)) return null;
        int found = find(state, -1, null);
        return found < 0 ? null : valueAt(found);
    }

    @Override public boolean containsKey(Object key) {
        return key instanceof Map<?, ?> state && find(state, -1, null) >= 0;
    }

    @Override public int size() { return keys.length; }

    @Override
    public Set<Entry<Map<?, ?>, V>> entrySet() {
        return entries;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Collection<V> values() {
        return (Collection) Collections.unmodifiableList(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> shareValues(Map<K, V> original, Map<?, ?> shared) {
        if (original.size() != shared.size()) return original;
        var left = original.entrySet().iterator();
        var right = shared.entrySet().iterator();
        while (left.hasNext()) {
            var a = left.next();
            var b = right.next();
            if (!Objects.equals(a.getKey(), b.getKey()) || !Objects.equals(a.getValue(), b.getValue())) return original;
        }
        return (Map<K, V>) shared;
    }

    public interface ValuesOwner {
        void sunlitindustry$shareValues(Map<?, ?> values, OrderedStateIndex<?> index);
    }
}
