package com.society.sunlitindustry.performance.gt;

import it.unimi.dsi.fastutil.Hash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GtParallelScratchTest {
    @AfterEach
    void clear() { GtParallelScratch.clearCurrentThread(); }

    @Test
    void scratchIsFreshAfterExceptionAndNestedCallsDoNotClearTheOuterMaps() {
        assertThrows(IllegalStateException.class, () -> {
            try (var outer = GtParallelScratch.open()) {
                var map = GtParallelScratch.<String>plainMap();
                map.put("outer", 9L);
                try (var inner = GtParallelScratch.open()) {
                    var innerMap = GtParallelScratch.<String>plainMap();
                    assertNotSame(map, innerMap);
                    assertTrue(innerMap.isEmpty());
                    innerMap.put("inner", 7L);
                }
                assertEquals(9L, map.getLong("outer"));
                throw new IllegalStateException("handler failure");
            }
        });
        try (var ignored = GtParallelScratch.open()) {
            assertTrue(GtParallelScratch.plainMap().isEmpty());
        }
    }

    @Test
    void strategyReplacementDoesNotReuseThePreviousEqualityRules() {
        Hash.Strategy<String> identity = new Hash.Strategy<>() {
            public int hashCode(String key) { return System.identityHashCode(key); }
            public boolean equals(String left, String right) { return left == right; }
        };
        Hash.Strategy<String> content = new Hash.Strategy<>() {
            public int hashCode(String key) { return key == null ? 0 : key.hashCode(); }
            public boolean equals(String left, String right) { return java.util.Objects.equals(left, right); }
        };
        try (var ignored = GtParallelScratch.open()) {
            var map = GtParallelScratch.customMap(identity);
            map.put(new String("x"), 2L);
            map.addTo(new String("x"), 3L);
            assertEquals(2, map.size());
        }
        try (var ignored = GtParallelScratch.open()) {
            var map = GtParallelScratch.customMap(content);
            assertTrue(map.isEmpty());
            map.put(new String("x"), 2L);
            map.addTo(new String("x"), 3L);
            assertEquals(1, map.size());
            assertEquals(5L, map.getLong("x"));
        }
    }
}
