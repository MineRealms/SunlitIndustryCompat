package com.society.sunlitindustry.performance.gt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GtScratchPoolTest {
    @Test
    void nestedBorrowCannotObtainAnInUseValue() {
        var pool = new GtScratchPool<Object>(2);
        var outer = new Object();
        pool.giveBack(outer);
        assertSame(outer, pool.take());
        assertNull(pool.take());
        var inner = new Object();
        pool.giveBack(inner);
        assertSame(inner, pool.take());
        pool.giveBack(outer);
        assertSame(outer, pool.take());
    }

    @Test
    void idleCapacityIsBoundedAndCanBeReleased() {
        var pool = new GtScratchPool<Object>(2);
        for (int i = 0; i < 100; i++) pool.giveBack(new Object());
        assertEquals(2, pool.size());
        pool.clear();
        assertNull(pool.take());
        assertEquals(0, pool.size());
        assertThrows(IllegalArgumentException.class, () -> new GtScratchPool<>(-1));
    }
}
