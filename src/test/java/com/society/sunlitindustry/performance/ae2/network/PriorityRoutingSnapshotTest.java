package com.society.sunlitindustry.performance.ae2.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class PriorityRoutingSnapshotTest {
    @Test
    void retainsPriorityGroupsSamePriorityOrderAndDuplicateMounts() {
        var mounts = new TreeMap<Integer, List<String>>(Comparator.reverseOrder());
        mounts.put(100, List.of("first", "second", "first"));
        mounts.put(-100, List.of("last"));
        var routes = PriorityRoutingSnapshot.copyOf(mounts);
        assertEquals(List.of("first", "second", "first", "last"), forward(routes));
        assertEquals(List.of("last", "first", "second", "first"), extraction(routes));
    }

    @Test
    void snapshotDoesNotChangeDuringQueuedMountOrUnmount() {
        var mounts = new TreeMap<Integer, List<Object>>(Comparator.reverseOrder());
        Object before = new Object(), after = new Object();
        mounts.put(0, new ArrayList<>(List.of(before)));
        var routes = PriorityRoutingSnapshot.copyOf(mounts);
        mounts.get(0).clear();
        mounts.get(0).add(after);
        assertSame(before, routes.get(0, 0));
        assertSame(after, PriorityRoutingSnapshot.copyOf(mounts).get(0, 0));
    }

    @Test
    void randomizedTraversalsMatchNativeMapIteration() {
        Random random = new Random(49023);
        for (int trial = 0; trial < 1000; trial++) {
            var mounts = new TreeMap<Integer, List<Integer>>(Comparator.reverseOrder());
            for (int item = 0; item < random.nextInt(100); item++) {
                mounts.computeIfAbsent(random.nextInt(25) - 12, ignored -> new ArrayList<>()).add(item);
            }
            var routes = PriorityRoutingSnapshot.copyOf(mounts);
            assertEquals(mounts.values().stream().flatMap(List::stream).toList(), forward(routes));
            assertEquals(mounts.descendingMap().values().stream().flatMap(List::stream).toList(), extraction(routes));
        }
    }

    private static <T> List<T> forward(PriorityRoutingSnapshot<T> routes) {
        List<T> result = new ArrayList<>();
        for (int group = 0; group < routes.groupCount(); group++) {
            for (int slot = 0; slot < routes.groupSize(group); slot++) result.add(routes.get(group, slot));
        }
        return result;
    }

    private static <T> List<T> extraction(PriorityRoutingSnapshot<T> routes) {
        List<T> result = new ArrayList<>();
        for (int group = routes.groupCount() - 1; group >= 0; group--) {
            for (int slot = 0; slot < routes.groupSize(group); slot++) result.add(routes.get(group, slot));
        }
        return result;
    }
}
