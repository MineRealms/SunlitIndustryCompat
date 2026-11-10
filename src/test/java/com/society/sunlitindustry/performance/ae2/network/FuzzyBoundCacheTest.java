package com.society.sunlitindustry.performance.ae2.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FuzzyBoundCacheTest {
    @Test
    void boundsRetainNativeInclusiveAndExclusiveBreakpoints() {
        assertEquals(75, FuzzyBoundCache.lower(100, 75, 75, false));
        assertEquals(-1, FuzzyBoundCache.upper(75, 75, false));
        assertEquals(100, FuzzyBoundCache.lower(100, 76, 75, false));
        assertEquals(75, FuzzyBoundCache.upper(76, 75, false));
        assertEquals(100, FuzzyBoundCache.lower(100, 0, 0, true));
        assertEquals(-1, FuzzyBoundCache.upper(0, 0, true));
    }

    @Test
    void collidingBoundsNeverReturnAnotherDamage() {
        var cache = new FuzzyBoundCache();
        Random random = new Random(12477);
        for (int i = 0; i < 10000; i++) {
            int damage = random.nextInt();
            cache.put(damage, Integer.valueOf(damage));
            assertEquals(damage, cache.get(damage));
            int other = random.nextInt();
            Object result = cache.get(other);
            if (result != null) assertEquals(other, result);
        }
    }

    @Test
    void plannerWorkerAndServerThreadDoNotShareMutableCacheState() throws InterruptedException {
        var server = FuzzyBoundCache.local();
        var worker = new AtomicReference<FuzzyBoundCache>();
        Thread thread = new Thread(() -> worker.set(FuzzyBoundCache.local()));
        thread.start();
        thread.join();
        assertNotSame(server, worker.get());
    }
}
