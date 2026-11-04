package com.society.sunlitindustry;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indexes a fixed state-property domain using ordered values instead of Map.hashCode's unordered sum.
 * Swapping boolean/integer values between properties otherwise creates large collision groups.
 * The external keys and entrySet remain unchanged; this only accelerates lookup during construction.
 */
public final class OrderedStateIndex<V> extends AbstractMap<Map<?, ?>, V> {
    private final Map<? extends Map<?, ?>, V> source;
    private final List<?> properties;
    private final Map<List<Object>, V> index = new HashMap<>();

    public OrderedStateIndex(Map<? extends Map<?, ?>, V> source) {
        this.source = source;
        properties = source.isEmpty() ? List.of() : new ArrayList<>(source.keySet().iterator().next().keySet());
        for (var entry : source.entrySet()) {
            List<Object> key = key(entry.getKey());
            if (key == null) throw new IllegalArgumentException("Inconsistent state property domain");
            index.put(key, entry.getValue());
        }
    }

    private List<Object> key(Map<?, ?> state) {
        if (state.size() != properties.size()) return null;
        List<Object> values = new ArrayList<>(properties.size());
        for (Object property : properties) {
            if (!state.containsKey(property)) return null;
            values.add(state.get(property));
        }
        return values;
    }

    public V with(Map<?, ?> state, Object property, Object replacement) {
        int position = properties.indexOf(property);
        if (position < 0) return null;
        List<Object> values = key(state);
        if (values == null) return null;
        values.set(position, replacement);
        return index.get(values);
    }

    @Override
    public V get(Object key) {
        if (!(key instanceof Map<?, ?> state)) return null;
        List<Object> values = key(state);
        return values == null ? null : index.get(values);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Set<Entry<Map<?, ?>, V>> entrySet() {
        return (Set) source.entrySet();
    }
}
