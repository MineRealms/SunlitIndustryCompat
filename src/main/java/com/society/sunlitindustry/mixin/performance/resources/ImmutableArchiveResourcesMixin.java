package com.society.sunlitindustry.mixin.performance.resources;

import com.society.sunlitindustry.performance.resources.ArchiveResourceCache;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.resource.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(PathPackResources.class)
public abstract class ImmutableArchiveResourcesMixin {
    @Shadow(remap = false) protected abstract java.nio.file.Path resolve(String... paths);
    @Unique private volatile ArchiveResourceCache.Binding sunlitindustry$archive;
    @Unique private volatile boolean sunlitindustry$identified;

    @Unique
    private ArchiveResourceCache.Binding sunlitindustry$binding() {
        if (!sunlitindustry$identified) {
            synchronized (this) {
                if (!sunlitindustry$identified) {
                    sunlitindustry$archive = ArchiveResourceCache.bind(((PathPackResources) (Object) this).getSource());
                    sunlitindustry$identified = true;
                }
            }
        }
        return sunlitindustry$archive;
    }

    @Inject(method = "getRootResource", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$sourceBytes(String[] paths, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        if (!ArchiveResourceCache.enabled() || paths.length < 3
                || !(paths[0].equals("data") || paths[0].equals("assets"))) return;
        var binding = sunlitindustry$binding();
        var snapshot = ArchiveResourceCache.snapshot(binding);
        if (snapshot == null) return;
        String path = binding.prefix() + String.join("/", paths);
        if (!snapshot.contains(path)) {
            cir.setReturnValue(null);
        } else if (snapshot.hasBytes(path)) {
            cir.setReturnValue(() -> {
                var current = ArchiveResourceCache.snapshot(binding);
                InputStream bytes = current == null ? null : current.open(path);
                if (bytes != null) return bytes;
                return java.nio.file.Files.newInputStream(resolve(paths));
            });
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void sunlitindustry$resourceTree(PackType type, String namespace, String path,
            PackResources.ResourceOutput output, CallbackInfo ci) {
        if (!ArchiveResourceCache.enabled() || FileUtil.decomposePath(path).result().isEmpty()) return;
        var binding = sunlitindustry$binding();
        var snapshot = ArchiveResourceCache.snapshot(binding);
        if (snapshot == null) return;
        String base = binding.prefix() + type.getDirectory() + "/" + namespace + "/";
        PathPackResources pack = (PathPackResources) (Object) this;
        for (String name : snapshot.listTree(base + path)) {
            ResourceLocation id = ResourceLocation.tryParse(namespace + ":" + name.substring(base.length()));
            if (id == null) continue;
            output.accept(id, () -> {
                var current = ArchiveResourceCache.snapshot(binding);
                InputStream cached = current == null ? null : current.open(name);
                if (cached != null) return cached;
                var original = pack.getResource(type, id);
                if (original == null) throw new java.io.FileNotFoundException(id.toString());
                return original.get();
            });
        }
        ci.cancel();
    }
}
