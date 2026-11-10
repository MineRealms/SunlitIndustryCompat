package com.society.sunlitindustry.performance.ae2.crafting;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpochLruCacheTest {
    @Test
    void reusesPositiveValuesButNeverCachesFailures() {
        var cache = new EpochLruCache<String, Object>();
        Object source = new Object();
        Object value = new Object();
        assertSame(value, cache.get("valid", 1, source, 4, () -> value));
        assertSame(value, cache.get("valid", 1, source, 4, () -> { throw new AssertionError(); }));
        var calls = new AtomicInteger();
        assertNull(cache.get("missing", 1, source, 4, () -> { calls.incrementAndGet(); return null; }));
        assertNull(cache.get("missing", 1, source, 4, () -> { calls.incrementAndGet(); return null; }));
        assertEquals(2, calls.get());
        assertEquals(1, cache.size());
    }

    @Test
    void bothReloadEpochAndRecipeManagerReplacementInvalidate() {
        var cache = new EpochLruCache<String, String>();
        Object source = new Object();
        assertEquals("old", cache.get("recipe", 1, source, 4, () -> "old"));
        assertEquals("modified", cache.get("recipe", 2, source, 4, () -> "modified"));
        assertNull(cache.get("recipe", 3, source, 4, () -> null));
        assertEquals("restored", cache.get("recipe", 4, source, 4, () -> "restored"));
        assertEquals("new manager", cache.get("recipe", 4, new Object(), 4, () -> "new manager"));
    }

    @Test
    void boundsStorageAndEvictsLeastRecentlyUsed() {
        var cache = new EpochLruCache<String, String>();
        Object source = new Object();
        cache.get("a", 1, source, 2, () -> "a");
        cache.get("b", 1, source, 2, () -> "b");
        cache.get("a", 1, source, 2, () -> { throw new AssertionError(); });
        cache.get("c", 1, source, 2, () -> "c");
        assertEquals(2, cache.size());
        assertEquals("b reloaded", cache.get("b", 1, source, 2, () -> "b reloaded"));
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void loweringLimitShrinksAnExistingCacheAndLoadExceptionsAreNotStored() {
        var cache = new EpochLruCache<String, String>();
        Object source = new Object();
        cache.get("a", 1, source, 3, () -> "a");
        cache.get("b", 1, source, 3, () -> "b");
        cache.get("c", 1, source, 3, () -> "c");
        cache.get("c", 1, source, 1, () -> { throw new AssertionError(); });
        assertEquals(1, cache.size());
        assertThrows(IllegalStateException.class,
                () -> cache.get("failed", 1, source, 3, () -> { throw new IllegalStateException(); }));
        assertEquals("retry", cache.get("failed", 1, source, 3, () -> "retry"));
    }

    @Test
    void parallelRequestsForOneKeyBuildOnlyOnce() throws Exception {
        var cache = new EpochLruCache<String, Object>();
        var builds = new AtomicInteger();
        Object source = new Object();
        Object value = new Object();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Object>>();
            for (int i = 0; i < 100; i++) tasks.add(() -> cache.get("key", 1, source, 2, () -> {
                builds.incrementAndGet();
                return value;
            }));
            for (var result : executor.invokeAll(tasks)) assertSame(value, result.get());
            assertEquals(1, builds.get());
        } finally {
            executor.shutdownNow();
        }
    }
}
