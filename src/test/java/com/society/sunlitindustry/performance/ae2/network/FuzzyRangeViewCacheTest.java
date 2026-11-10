package com.society.sunlitindustry.performance.ae2.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class FuzzyRangeViewCacheTest {
    @Test
    void liveViewSeesAddsRemovalsClearsAndEntryMutations() {
        var map = new TreeMap<Integer, Long>();
        map.put(10, 1L);
        var cache = new FuzzyRangeViewCache<SortedMap<Integer, Long>>();
        assertNull(cache.get(map, 0, 20));
        var view = map.subMap(0, 20);
        cache.put(map, 0, 20, view);
        map.put(15, 2L);
        assertEquals(2, cache.get(map, 0, 20).size());
        cache.get(map, 0, 20).entrySet().iterator().next().setValue(8L);
        assertEquals(8L, map.get(10));
        map.remove(15);
        assertEquals(1, cache.get(map, 0, 20).size());
        map.clear();
        assertTrue(cache.get(map, 0, 20).isEmpty());
    }

    @Test
    void mapIdentityAndBothBoundsArePartOfCacheKey() {
        var first = new TreeMap<Integer, Long>();
        var second = new TreeMap<Integer, Long>();
        var cache = new FuzzyRangeViewCache<SortedMap<Integer, Long>>();
        var view = first.subMap(0, 20);
        cache.put(first, 0, 20, view);
        assertSame(view, cache.get(first, 0, 20));
        assertNull(cache.get(first, 1, 20));
        assertNull(cache.get(first, 0, 21));
        assertNull(cache.get(second, 0, 20));
        assertNull(cache.get(first, 0, 20));
    }

    @Test
    void evictionDropsViewsWithoutChangingTheirBackingMaps() {
        var map = new TreeMap<Integer, Long>();
        map.put(5, 10L);
        var cache = new FuzzyRangeViewCache<SortedMap<Integer, Long>>();
        for (int range = 0; range < 20; range++) cache.put(map, range, range + 10, map.subMap(range, range + 10));
        assertNull(cache.get(map, 0, 10));
        assertNotNull(cache.get(map, 19, 29));
        assertEquals(10L, map.get(5));
    }
}
