package com.society.sunlitindustry.mixin;

import appeng.api.stacks.AEItemKey;
import com.society.sunlitindustry.AE2DefaultItemKeyHolder;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class AE2ItemDefaultKeyMixin implements AE2DefaultItemKeyHolder {
    @Unique
    private volatile AEItemKey sunlitindustry$defaultAEItemKey;

    @Override
    public AEItemKey sunlitindustry$getDefaultAEItemKey() {
        return sunlitindustry$defaultAEItemKey;
    }

    @Override
    public void sunlitindustry$setDefaultAEItemKey(AEItemKey key) {
        sunlitindustry$defaultAEItemKey = key;
    }
}
