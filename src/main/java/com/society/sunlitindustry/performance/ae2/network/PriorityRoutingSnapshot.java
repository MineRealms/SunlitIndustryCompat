package com.society.sunlitindustry.performance.ae2.network;

import java.util.List;
import java.util.NavigableMap;

/** A topology snapshot; routing decisions and inventory contents are never cached. */
public final class PriorityRoutingSnapshot<T> {
    private final Object[][] groups;

    private PriorityRoutingSnapshot(Object[][] groups) {
        this.groups = groups;
    }

    public static <T> PriorityRoutingSnapshot<T> copyOf(NavigableMap<Integer, ? extends List<T>> mounts) {
        Object[][] groups = new Object[mounts.size()][];
        int index = 0;
        for (List<T> inventories : mounts.values()) {
            groups[index++] = inventories.toArray();
        }
        return new PriorityRoutingSnapshot<>(groups);
    }

    public int groupCount() { return groups.length; }
    public int groupSize(int group) { return groups[group].length; }

    @SuppressWarnings("unchecked")
    public T get(int group, int inventory) { return (T) groups[group][inventory]; }
}
