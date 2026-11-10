package com.society.sunlitindustry;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroup;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroupColor;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroupDistinctness;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;

import java.util.HashMap;
import java.util.List;

/** Caches grouping structure only; inventories, recipe results and chance rolls are always read live. */
public final class GTHandlerGroups {
    public static final int ITEM_INPUT = 4;
    public static final int FLUID_INPUT = 5;
    private final Slot[] slots = new Slot[6];

    public synchronized HashMap<RecipeHandlerGroup, List<RecipeHandlerList>> get(
            int key, List<RecipeHandlerList> handlers, RecipeCapability<?> filter) {
        if (!ServerPerformanceConfig.groups() || handlers.size() > ServerPerformanceConfig.maxHandlers()) {
            slots[key] = null;
            ServerPerformanceStats.record(ServerPerformanceStats.Event.GROUP_FALLBACK);
            return new HashMap<>();
        }
        // Third-party mutable group keys have no reliable change signature. Keep their native path.
        for (var handler : handlers) {
            var group = handler.getGroup();
            if (!(group instanceof RecipeHandlerGroupColor || group instanceof RecipeHandlerGroupDistinctness)) {
                slots[key] = null;
                ServerPerformanceStats.record(ServerPerformanceStats.Event.GROUP_FALLBACK);
                return new HashMap<>();
            }
        }
        Slot slot = slots[key];
        if (slot != null && slot.matches(handlers, filter)) {
            ServerPerformanceStats.record(ServerPerformanceStats.Event.GROUP_HIT);
            return slot.groups;
        }
        slot = new Slot(handlers, filter);
        slots[key] = slot;
        ServerPerformanceStats.record(ServerPerformanceStats.Event.GROUP_BUILD);
        return slot.groups;
    }

    public static int key(IO io) { return io.ordinal(); }

    public static void append(RecipeHandlerGroup group, RecipeHandlerList handler,
                              java.util.Map<RecipeHandlerGroup, List<RecipeHandlerList>> groups) {
        if (!(groups instanceof ReadyMap)) RecipeHelper.addToRecipeHandlerMap(group, handler, groups);
    }

    private static final class ReadyMap extends HashMap<RecipeHandlerGroup, List<RecipeHandlerList>> {}

    private static final class Slot {
        private final RecipeHandlerList[] handlers;
        private final RecipeHandlerGroup[] groupKeys;
        private final boolean[] bypass;
        private final boolean[] included;
        private final ReadyMap groups = new ReadyMap();

        Slot(List<RecipeHandlerList> source, RecipeCapability<?> filter) {
            handlers = source.toArray(RecipeHandlerList[]::new);
            groupKeys = new RecipeHandlerGroup[handlers.length];
            bypass = new boolean[handlers.length];
            included = new boolean[handlers.length];
            for (int i = 0; i < handlers.length; i++) {
                var handler = handlers[i];
                groupKeys[i] = handler.getGroup();
                bypass[i] = handler.doesCapabilityBypassDistinct();
                included[i] = filter == null || handler.hasCapability(filter);
                if (included[i]) RecipeHelper.addToRecipeHandlerMap(groupKeys[i], handler, groups);
            }
        }

        boolean matches(List<RecipeHandlerList> source, RecipeCapability<?> filter) {
            if (source.size() != handlers.length) return false;
            for (int i = 0; i < handlers.length; i++) {
                var handler = source.get(i);
                if (handler != handlers[i] || !groupKeys[i].equals(handler.getGroup())
                        || bypass[i] != handler.doesCapabilityBypassDistinct()
                        || included[i] != (filter == null || handler.hasCapability(filter))) return false;
            }
            return true;
        }
    }
}
