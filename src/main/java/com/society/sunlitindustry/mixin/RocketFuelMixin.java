package com.society.sunlitindustry.mixin;

import argent_matter.gcyr.api.space.planet.Planet;
import argent_matter.gcyr.common.data.GCYRRecipeTypes;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.society.sunlitindustry.FuelMeter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;

/** GCYR 0.2.9: per-rocket fuel validation and prepaid fractional consumption. */
@Pseudo
@Mixin(targets = "argent_matter.gcyr.common.entity.RocketEntity", remap = false)
public abstract class RocketFuelMixin extends Entity {
    @Shadow @Final private CustomFluidTank fuelTank;
    @Shadow private int motorTier;
    @Shadow private GTRecipe selectedFuelRecipe;
    @Shadow public abstract int getThrusterCount();
    @Shadow public abstract int getFuelCapacity();
    @Shadow public abstract void setRecipeDuration(int value);
    @Shadow public abstract Planet getDestination();
    @Unique private double sunlit$fuelCredit;
    @Unique private String sunlit$creditFluid = "";

    protected RocketFuelMixin(EntityType<?> type, Level level) { super(type, level); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sunlit$install(CallbackInfo ci) {
        // Leave GCYR's contents callback in place for entity-data synchronization.
        // Consumption below revalidates its selection before using its duration.
        fuelTank.setValidator(stack -> level().isClientSide || sunlit$find(stack) != null);
    }

    @Unique private boolean sunlit$matches(GTRecipe recipe, FluidStack stack) {
        if (stack.isEmpty() || recipe.duration <= 0 || motorTier < 1
                || RecipeHelper.getRecipeEUtTier(recipe) > motorTier) return false;
        var inputs = recipe.inputs.get(FluidRecipeCapability.CAP);
        if (inputs == null || inputs.isEmpty()) return false;
        return java.util.Arrays.stream(FluidRecipeCapability.CAP.of(inputs.get(0).content).getStacks())
                .anyMatch(fluid -> fluid.isFluidEqual(stack));
    }

    @Unique private GTRecipe sunlit$find(FluidStack stack) {
        if (getServer() == null || stack.isEmpty()) return null;
        return getServer().getRecipeManager().getAllRecipesFor(GCYRRecipeTypes.ROCKET_FUEL_RECIPES).stream()
                .filter(recipe -> sunlit$matches(recipe, stack))
                .min(Comparator.comparing(recipe -> recipe.id.toString())).orElse(null);
    }

    @Unique private FluidStack sunlit$payableFuel() {
        FluidStack stack = fuelTank.getFluid();
        if (!stack.isEmpty() || sunlit$fuelCredit <= 0 || sunlit$creditFluid.isEmpty()) return stack;
        var id = net.minecraft.resources.ResourceLocation.tryParse(sunlit$creditFluid);
        return id == null ? FluidStack.EMPTY : new FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id), 1);
    }

    @Inject(method = "lambda$new$9", at = @At("TAIL"))
    private void sunlit$contentsChanged(CallbackInfo ci) {
        if (!level().isClientSide) sunlit$refresh();
    }

    @Unique private GTRecipe sunlit$refresh() {
        FluidStack stack = sunlit$payableFuel();
        // Check object identity against the live recipe manager, not only the saved recipe id.
        GTRecipe current = selectedFuelRecipe;
        if (getServer() == null || current == null || !sunlit$matches(current, stack)
                || getServer().getRecipeManager().byKey(current.id).orElse(null) != current) {
            current = sunlit$find(stack);
        }
        selectedFuelRecipe = current;
        setRecipeDuration(current == null ? 0 : current.duration);
        return current;
    }

    @Inject(method = "consumeFuel", at = @At("HEAD"), cancellable = true)
    private void sunlit$consume(CallbackInfoReturnable<Boolean> cir) {
        if (level().isClientSide) return; // GCYR owns visual movement; the server owns payment.
        GTRecipe recipe = sunlit$refresh();
        if (recipe == null) { cir.setReturnValue(false); return; }
        String fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(sunlit$payableFuel().getFluid()).toString();
        if (!fluid.equals(sunlit$creditFluid)) { sunlit$fuelCredit = 0; sunlit$creditFluid = fluid; }
        double demand = FuelMeter.demand(getThrusterCount(), getDestination() == null ? 1 : getDestination().rocketTier(), recipe.duration);
        var draw = FuelMeter.quote(sunlit$fuelCredit, demand, fuelTank.getFluidAmount());
        if (!draw.allowed()) { cir.setReturnValue(false); return; }
        if (draw.millibuckets() > 0) {
            int simulated = fuelTank.drain(draw.millibuckets(), IFluidHandler.FluidAction.SIMULATE).getAmount();
            if (simulated != draw.millibuckets()) { cir.setReturnValue(false); return; }
            fuelTank.drain(draw.millibuckets(), IFluidHandler.FluidAction.EXECUTE);
        }
        sunlit$fuelCredit = draw.credit();
        cir.setReturnValue(true);
    }

    @Inject(method = {"addAdditionalSaveData", "m_7380_"}, at = @At("TAIL"))
    private void sunlit$save(CompoundTag tag, CallbackInfo ci) {
        tag.putDouble("sunlitindustry.fuelCredit", sunlit$fuelCredit);
        tag.putString("sunlitindustry.creditFluid", sunlit$creditFluid);
    }

    @Inject(method = {"readAdditionalSaveData", "m_7378_"}, at = @At("TAIL"))
    private void sunlit$load(CompoundTag tag, CallbackInfo ci) {
        sunlit$fuelCredit = FuelMeter.validCredit(tag.getDouble("sunlitindustry.fuelCredit"));
        sunlit$creditFluid = tag.getString("sunlitindustry.creditFluid");
        selectedFuelRecipe = null; // Resolve against current recipes/tier on first use, not stale NBT.
        setRecipeDuration(0);
        fuelTank.setCapacity(getFuelCapacity());
    }
}
