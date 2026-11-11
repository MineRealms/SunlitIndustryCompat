package com.society.sunlitindustry.performance.gt;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GtDeferredMeFlushTest {
    @Test
    void oneFailingTargetDoesNotInterruptTheBatch() {
        var visited = new ArrayList<String>();
        GtDeferredFlushTarget first = () -> visited.add("first");
        GtDeferredFlushTarget broken = () -> {
            visited.add("broken");
            throw new IllegalStateException("test failure");
        };
        GtDeferredFlushTarget last = () -> visited.add("last");
        GtDeferredMeFlush.flushTargets(List.of(first, broken, last));
        assertEquals(List.of("first", "broken", "last"), visited);
    }
}
