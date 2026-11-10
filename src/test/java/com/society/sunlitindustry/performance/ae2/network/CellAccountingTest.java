package com.society.sunlitindustry.performance.ae2.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CellAccountingTest {
    @Test
    void matchesNativeLongArithmeticAcrossUpdatesAndOverflow() {
        Random random = new Random(125891);
        var amounts = new Object2LongOpenHashMap<String>();
        for (int trial = 0; trial < 1000; trial++) {
            amounts.put("key" + random.nextInt(63), random.nextLong());
            long expected = 0;
            for (Long value : amounts.values()) expected += value;
            assertEquals(expected, CellAccounting.sum(amounts));
            if (trial % 100 == 0) amounts.clear();
        }
    }
}
