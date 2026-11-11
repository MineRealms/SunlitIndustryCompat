package com.society.sunlitindustry.performance.ae2.network;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

public final class CellAccounting {
    public static <K> long sum(Object2LongMap<K> amounts) {
        long total = 0;
        var values = amounts.values().iterator();
        while (values.hasNext()) total += values.nextLong();
        return total;
    }

    private CellAccounting() {}
}
