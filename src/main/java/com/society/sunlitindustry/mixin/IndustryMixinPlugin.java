package com.society.sunlitindustry.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public final class IndustryMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith(".RocketFuelMixin")) return modLoaded("gcyr");
        if (mixin.endsWith(".VintageDelightBrewingMixin")) return modLoaded("vintagedelight");
        if (mixin.endsWith(".WorldEditStateIndexMixin")) return modLoaded("worldedit");
        return true;
    }
    private static boolean modLoaded(String modId) {
        var list = FMLLoader.getLoadingModList();
        return list != null && list.getMods().stream().anyMatch(m -> m.getModId().equals(modId));
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
