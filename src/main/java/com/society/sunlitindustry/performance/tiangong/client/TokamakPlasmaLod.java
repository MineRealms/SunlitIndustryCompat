package com.society.sunlitindustry.performance.tiangong.client;

import com.society.sunlitindustry.performance.tiangong.TiangongClientConfig;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zmod.machine.tokamak.TokamakGeometry;
import zmod.machine.tokamak.TokamakPreviewLayout;

public final class TokamakPlasmaLod {
    private static final Logger LOGGER = LoggerFactory.getLogger("SunlitIndustry/TokamakPlasmaLod");
    private static volatile Meshes medium;
    private static volatile Meshes distant;
    private static volatile boolean failed;

    public static TokamakGeometry.Mesh select(TokamakGeometry.Mesh nativeMesh, int layer,
            BlockEntity controller) {
        if (!TiangongClientConfig.tokamakPlasmaLod() || failed) return nativeMesh;
        double squared = TiangongEffectDistance.nearestDistanceSquared(controller);
        if (!Double.isFinite(squared)) return nativeMesh;
        double mid = TiangongClientConfig.tokamakPlasmaMidDistance();
        if (squared < mid * mid) return nativeMesh;
        double far = TiangongClientConfig.tokamakPlasmaFarDistance();
        try {
            Meshes meshes = meshes(squared >= far * far);
            return switch (layer) {
                case 0 -> meshes.core();
                case 1 -> meshes.plasma();
                case 2 -> meshes.halo();
                default -> nativeMesh;
            };
        } catch (RuntimeException error) {
            failed = true;
            LOGGER.warn("Tokamak plasma LOD generation failed; retaining native meshes", error);
            return nativeMesh;
        }
    }

    private static Meshes meshes(boolean far) {
        Meshes result = far ? distant : medium;
        if (result != null) return result;
        synchronized (TokamakPlasmaLod.class) {
            result = far ? distant : medium;
            if (result == null) {
                int ring = far ? 32 : 64;
                int tube = far ? 8 : 12;
                result = new Meshes(
                        TokamakGeometry.mesh(TokamakPreviewLayout.SHAPE, ring, tube, 0.43),
                        TokamakGeometry.mesh(TokamakPreviewLayout.SHAPE, ring, tube, 0.64),
                        TokamakGeometry.mesh(TokamakPreviewLayout.SHAPE, ring, tube, 0.8));
                if (far) distant = result;
                else medium = result;
            }
        }
        return result;
    }

    private record Meshes(TokamakGeometry.Mesh core, TokamakGeometry.Mesh plasma,
            TokamakGeometry.Mesh halo) {}

    private TokamakPlasmaLod() {}
}
