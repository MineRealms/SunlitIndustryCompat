package com.society.sunlitindustry.performance.tiangong.client;

import com.society.sunlitindustry.performance.tiangong.TiangongClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TiangongEffectDistance {
    public static boolean visible(Object controller) {
        if (!TiangongClientConfig.distantEffects() || !(controller instanceof BlockEntity blockEntity)) {
            return true;
        }
        double squared = nearestDistanceSquared(blockEntity);
        double distance = TiangongClientConfig.effectDistance();
        return !Double.isFinite(squared) || squared <= distance * distance;
    }

    public static double nearestDistanceSquared(BlockEntity blockEntity) {
        AABB bounds = blockEntity.getRenderBoundingBox();
        if (bounds == null || !finite(bounds)) return Double.NaN;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        // Native rectangular bounds do not rotate with every industrial machine's facing.
        double horizontalHalf = Math.max(bounds.maxX - bounds.minX, bounds.maxZ - bounds.minZ) * 0.5;
        double centerX = (bounds.minX + bounds.maxX) * 0.5;
        double centerZ = (bounds.minZ + bounds.maxZ) * 0.5;
        double dx = distanceToRange(camera.x, centerX - horizontalHalf, centerX + horizontalHalf);
        double dy = distanceToRange(camera.y, bounds.minY, bounds.maxY);
        double dz = distanceToRange(camera.z, centerZ - horizontalHalf, centerZ + horizontalHalf);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceToRange(double coordinate, double min, double max) {
        return coordinate < min ? min - coordinate : coordinate > max ? coordinate - max : 0;
    }

    private static boolean finite(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }

    private TiangongEffectDistance() {}
}
