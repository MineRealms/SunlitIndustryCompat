package com.society.sunlitindustry;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OrderedStateIndexTest {
    @Test void everyStateAndNeighbourMatchesOriginalLookup() {
        Map<Map<String, Object>, Integer> source = new LinkedHashMap<>();
        for (int i = 0; i < 4096; i++) {
            Map<String, Object> state = new LinkedHashMap<>();
            for (int bit = 0; bit < 12; bit++) state.put("p" + bit, (i & (1 << bit)) != 0);
            source.put(Map.copyOf(state), i);
        }
        var index = new OrderedStateIndex<>(source);
        assertEquals(source.size(), index.size());
        for (var entry : source.entrySet()) {
            assertEquals(entry.getValue(), index.get(entry.getKey()));
            for (int bit = 0; bit < 12; bit++) {
                Map<String, Object> neighbour = new LinkedHashMap<>(entry.getKey());
                neighbour.put("p" + bit, !(Boolean) neighbour.get("p" + bit));
                assertEquals(source.get(neighbour), index.get(neighbour));
                assertEquals(source.get(neighbour), index.with(entry.getKey(), "p" + bit, neighbour.get("p" + bit)));
            }
        }
        assertEquals(source.entrySet(), index.entrySet());
        assertNull(index.get(Map.of("unknown", true)));
        assertNull(index.get("not a map"));
    }

    @Test void keyInsertionOrderDoesNotAffectLookup() {
        var source = Map.of(Map.of("north", true, "south", false), "a",
                Map.of("north", false, "south", true), "b");
        var index = new OrderedStateIndex<>(source);
        var key = new LinkedHashMap<String, Object>();
        key.put("south", false); key.put("north", true);
        assertEquals("a", index.get(key));
    }

    @Test void rejectsMixedPropertyDomains() {
        assertThrows(IllegalArgumentException.class, () -> new OrderedStateIndex<>(
                Map.of(Map.of("a", 1), "a", Map.of("b", 1), "b")));
    }

    @Test void iterationAndIdentityMatchSourceWithoutRetainingTheSourceMap() {
        Map<Map<String, Object>, Object> source = new LinkedHashMap<>();
        var first = Map.<String, Object>of("north", true, "south", false);
        var second = Map.<String, Object>of("north", false, "south", true);
        Object a = new Object();
        Object b = new Object();
        source.put(first, a);
        source.put(second, b);
        var index = new OrderedStateIndex<>(source);
        assertEquals(new ArrayList<>(source.entrySet()), new ArrayList<>(index.entrySet()));
        assertEquals(List.of(a, b), new ArrayList<>(index.values()));
        assertSame(first, index.entrySet().iterator().next().getKey());
        assertSame(a, index.get(first));
        source.clear();
        assertEquals(2, index.size());
        assertSame(a, index.get(first));
        assertSame(b, index.get(second));
        assertThrows(UnsupportedOperationException.class, () -> index.entrySet().iterator().next().setValue(b));
        assertThrows(UnsupportedOperationException.class, () -> index.values().clear());
    }

    @Test void sparseDomainsInvalidReplacementsAndNullValuesPreserveLookupSemantics() {
        Map<Map<String, Object>, String> source = new LinkedHashMap<>();
        source.put(Map.of("a", true, "b", false), "first");
        source.put(Map.of("a", false, "b", true), null);
        var index = new OrderedStateIndex<>(source);
        assertNull(index.with(Map.of("a", true, "b", false), "a", false));
        assertNull(index.with(Map.of("a", true, "b", false), "missing", true));
        assertNull(index.with(Map.of("a", true, "b", false), "a", "invalid"));
        assertNull(index.get(Map.of("a", true)));
        assertNull(index.get(Map.of("a", true, "b", false, "extra", true)));
        assertTrue(index.containsKey(Map.of("a", false, "b", true)));
        assertNull(index.get(Map.of("a", false, "b", true)));
        assertEquals(source, index);
        assertEquals(source.hashCode(), index.hashCode());
    }

    @Test void handlesHashCollisionsAndPropertylessStates() {
        assertEquals("Aa".hashCode(), "BB".hashCode());
        var a = Map.of("value", "Aa");
        var b = Map.of("value", "BB");
        var collision = new OrderedStateIndex<>(Map.of(a, "a", b, "b"));
        assertEquals("a", collision.get(a));
        assertEquals("b", collision.get(b));
        assertEquals("b", collision.with(a, "value", "BB"));
        var empty = new OrderedStateIndex<>(Map.of());
        assertTrue(empty.isEmpty());
        assertNull(empty.get(Map.of()));
        var propertyless = new OrderedStateIndex<>(Map.of(Map.of(), "state"));
        assertEquals("state", propertyless.get(Map.of()));
        assertNull(propertyless.with(Map.of(), "unknown", true));
    }

    @Test void sharesOnlyEqualStateValuesInTheOriginalPropertyOrder() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("north", true);
        original.put("south", false);
        Map<String, Object> candidate = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(original));
        assertSame(candidate, OrderedStateIndex.shareValues(original, candidate));
        assertEquals(List.of("north", "south"), new ArrayList<>(candidate.keySet()));
        Map<String, Object> reversed = new LinkedHashMap<>();
        reversed.put("south", false);
        reversed.put("north", true);
        assertSame(original, OrderedStateIndex.shareValues(original, reversed));
        assertSame(original, OrderedStateIndex.shareValues(original, Map.of("north", true)));
        assertSame(original, OrderedStateIndex.shareValues(original, Map.of("north", false, "south", false)));
        assertThrows(UnsupportedOperationException.class, () -> OrderedStateIndex.shareValues(original, candidate).put("north", false));
    }
}
