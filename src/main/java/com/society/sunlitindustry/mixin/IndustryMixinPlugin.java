package com.society.sunlitindustry.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public final class IndustryMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        Boolean performanceRule = PerformanceMixinRules.applies(mixin);
        if (performanceRule != null) return performanceRule;
        if (mixin.endsWith(".RocketFuelMixin")) return modLoaded("gcyr");
        if (mixin.endsWith(".VintageDelightBrewingMixin")) return modLoaded("vintagedelight");
        if (mixin.endsWith(".WorldEditStateIndexMixin")) return modLoaded("worldedit");
        if (mixin.endsWith(".FTBBackupsPreviewMixin")) return modLoaded("ftbbackups2");
        if (mixin.endsWith(".KubeJSRecipeCacheMixin")) return modLoaded("kubejs");
        if (mixin.endsWith(".GTResearchDataCopyMixin")) return modLoaded("gtceu");
        if (mixin.endsWith(".GTVoidMekaSuitAssemblyMixin")) return modLoaded("gtceu") && modLoaded("zmod");
        if (mixin.endsWith(".WanxiangFormationMixin")) return modLoaded("zmod") && modLoaded("gtceu");
        if (mixin.endsWith(".AliClientTooltipCacheMixin")) return FMLLoader.getDist().isClient() && modLoaded("ali");
        if (mixin.endsWith(".MekanismFusionBalanceMixin")
                || mixin.endsWith(".MekanismFissionBalanceMixin")) {
            return modLoaded("mekanismgenerators");
        }
        return true;
    }
    private static boolean modLoaded(String modId) {
        var list = FMLLoader.getLoadingModList();
        return list != null && list.getMods().stream().anyMatch(m -> m.getModId().equals(modId));
    }

    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (mixin.endsWith(".AliClientTooltipCacheMixin")) {
            patchAliTooltipCache(node);
            return;
        }
        if (!mixin.endsWith(".ForgeTagConcurrencyMixin")) return;
        int patched = 0;
        for (var method : node.methods) {
            if ((method.name.equals("getTag") && method.desc.equals("(Lnet/minecraft/tags/TagKey;)Lnet/minecraftforge/registries/tags/ITag;"))
                    || (method.name.equals("bind") && method.desc.equals("(Ljava/util/Map;Ljava/util/Set;)V"))) {
                // Parallel recipe decoding must not capture a tag handle lost by another writer.
                method.access |= Opcodes.ACC_SYNCHRONIZED;
                patched++;
            }
        }
        if (patched != 2) throw new IllegalStateException("Unsupported Forge tag manager: expected getTag and bind");
    }

    private static void patchAliTooltipCache(ClassNode node) {
        int patched = 0;
        for (var method : node.methods) {
            if (!method.name.equals("empty") || !method.desc.equals("()Lcom/yanny/aci/tooltip/TooltipNode;")) continue;
            for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof FieldInsnNode field)
                        || field.getOpcode() != Opcodes.GETFIELD
                        || !field.owner.equals("com/yanny/aci/manager/CorePluginManager")
                        || !field.name.equals("serverRegistry")
                        || !field.desc.equals("Lcom/yanny/aci/manager/CoreServerRegistry;")) continue;
                var call = field.getNext();
                if (!(call instanceof MethodInsnNode invoke)
                        || invoke.getOpcode() != Opcodes.INVOKEVIRTUAL
                        || !invoke.owner.equals("com/yanny/aci/manager/CoreServerRegistry")
                        || !invoke.name.equals("getTooltipCache")
                        || !invoke.desc.equals("()Lcom/yanny/aci/tooltip/TooltipNodePalette;")) continue;
                var replacement = new InsnList();
                replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, "com/yanny/aci/manager/CorePluginManager",
                        "INSTANCE", "Lcom/yanny/aci/manager/CorePluginManager;"));
                replacement.add(new FieldInsnNode(Opcodes.GETFIELD, "com/yanny/aci/manager/CorePluginManager",
                        "clientRegistry", "Lcom/yanny/aci/manager/CoreClientRegistry;"));
                replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/society/sunlitindustry/AliTooltipCache",
                        "palette", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
                replacement.add(new TypeInsnNode(Opcodes.CHECKCAST, "com/yanny/aci/tooltip/TooltipNodePalette"));
                method.instructions.insertBefore(call, replacement);
                method.instructions.remove(call);
                patched++;
            }
        }
        if (patched != 1) {
            throw new IllegalStateException("Unsupported ALI TooltipNode.empty: expected one server cache lookup");
        }
    }
}
