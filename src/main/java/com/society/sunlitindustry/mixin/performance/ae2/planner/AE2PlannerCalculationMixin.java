package com.society.sunlitindustry.mixin.performance.ae2.planner;

import appeng.crafting.CraftingCalculation;
import com.society.sunlitindustry.performance.ae2.planner.PlannerGraphEpoch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CraftingCalculation.class, remap = false)
public abstract class AE2PlannerCalculationMixin implements PlannerGraphEpoch {
    @Unique private long sunlitindustry$graphGeneration;

    @Override
    public long sunlitindustry$graphEpoch() { return sunlitindustry$graphGeneration; }

    @Override
    public void sunlitindustry$graphChanged() { sunlitindustry$graphGeneration++; }
}
