package com.society.sunlitindustry;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod("sunlitindustry")
public final class SunlitIndustryCompat {
    public SunlitIndustryCompat() {
        NuclearBalanceConfig.register();
        MekItemEnergyConfig.register();
        com.society.sunlitindustry.performance.PerformanceBootstrap.register();
        if (ModList.get().isLoaded("gtceu") && ModList.get().isLoaded("mekanism")) {
            MekEuItemCapabilities.register();
        }
    }
}
