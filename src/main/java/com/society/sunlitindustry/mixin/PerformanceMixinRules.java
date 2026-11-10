package com.society.sunlitindustry.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import java.util.Set;

/** Loading-time gates use only FML metadata; optional mod classes must not be resolved here. */
final class PerformanceMixinRules {
    private static final Set<String> GT = Set.of("GTMachineGroupCacheMixin", "GTWorkableGroupLifecycleMixin",
            "GTRecipeFastPathMixin", "GTRecipeRunnerPerformanceMixin", "GTItemParallelGroupsMixin", "GTFluidParallelGroupsMixin");
    private static final Set<String> AE2 = Set.of("AE2ItemDefaultKeyMixin", "AE2DefaultItemKeyCacheMixin",
            "AE2FluidDefaultKeyMixin", "AE2DefaultFluidKeyCacheMixin");

    static Boolean applies(String mixin) {
        String name = mixin.substring(mixin.lastIndexOf('.') + 1);
        if (GT.contains(name)) return runtime() && version("gtceu", "7.5.3");
        if (AE2.contains(name)) return runtime() && version("gtceu", "7.5.3") && version("ae2", "15.4.10");
        if (mixin.endsWith(".performance.ae2.network.AE2CompactCellNbtMixin")) {
            // Existing compact cells must remain readable after disabling the writer or optimizations.
            return version("ae2", "15.4.10") && !conflict();
        }
        if (mixin.contains(".mixin.performance.resources.")) {
            return !disabled() && !conflict()
                    && (FMLLoader.getDist().isDedicatedServer()
                    || (!loaded("modernfix") && !loaded("fusion")));
        }
        if (mixin.contains(".mixin.performance.ae2.")) {
            return runtime() && version("ae2", "15.4.10");
        }
        if (mixin.contains(".mixin.performance.gt.")) {
            if ((mixin.endsWith(".GtDeferredMeBusMixin") || mixin.endsWith(".GtDeferredMeHatchMixin"))
                    && !version("ae2", "15.4.10")) return false;
            return runtime() && version("gtceu", "7.5.3");
        }
        return null;
    }

    static boolean runtime() { return FMLLoader.getDist().isDedicatedServer() && !disabled() && !conflict(); }
    private static boolean disabled() { return Boolean.getBoolean("sunlitindustry.disableServerOptimizations"); }
    private static boolean conflict() { return loaded("gtocore") || loaded("gtolib") || loaded("warpload"); }
    private static boolean loaded(String id) {
        var list = FMLLoader.getLoadingModList();
        return list != null && list.getMods().stream().anyMatch(mod -> mod.getModId().equals(id));
    }
    private static boolean version(String id, String version) {
        var list = FMLLoader.getLoadingModList();
        return list != null && list.getMods().stream().anyMatch(mod -> mod.getModId().equals(id)
                && mod.getVersion().toString().equals(version));
    }
    private PerformanceMixinRules() {}
}
