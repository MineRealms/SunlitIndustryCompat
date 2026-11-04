package com.society.sunlitindustry;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
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
}
