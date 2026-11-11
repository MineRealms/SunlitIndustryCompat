package com.society.sunlitindustry.performance.tiangong.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class DysonPrimitiveEmitter {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    public static void quad(BufferBuilder buffer, Matrix4f matrix, Vector3f p, Vector3f q,
            Vector3f r, Vector3f s, int red, int green, int blue, int alpha) {
        vertex(buffer, matrix, p.x, p.y, p.z, red, green, blue, alpha);
        vertex(buffer, matrix, q.x, q.y, q.z, red, green, blue, alpha);
        vertex(buffer, matrix, r.x, r.y, r.z, red, green, blue, alpha);
        vertex(buffer, matrix, s.x, s.y, s.z, red, green, blue, alpha);
    }

    public static void diamond(BufferBuilder buffer, Matrix4f matrix, Vector3f p, float radius,
            int red, int green, int blue, int alpha) {
        vertex(buffer, matrix, p.x - radius, p.y + 0.0F, p.z + 0.0F, red, green, blue, alpha);
        vertex(buffer, matrix, p.x + 0.0F, p.y + 0.0F, p.z + radius, red, green, blue, alpha);
        vertex(buffer, matrix, p.x + radius, p.y + 0.0F, p.z + 0.0F, red, green, blue, alpha);
        vertex(buffer, matrix, p.x + 0.0F, p.y + 0.0F, p.z - radius, red, green, blue, alpha);
    }

    public static void line(BufferBuilder buffer, Matrix4f matrix, Vector3f p, Vector3f q,
            float width, int red, int green, int blue, int alpha) {
        Scratch scratch = SCRATCH.get();
        Vector3f side = scratch.side.set(q).sub(p).cross(scratch.normal.set(p).normalize());
        if (side.lengthSquared() < 1.0E-9) return;
        side.normalize(width);
        float x = side.x;
        float y = side.y;
        float z = side.z;
        vertex(buffer, matrix, p.x + x, p.y + y, p.z + z, red, green, blue, alpha);
        vertex(buffer, matrix, q.x + x, q.y + y, q.z + z, red, green, blue, alpha);
        vertex(buffer, matrix, q.x - x, q.y - y, q.z - z, red, green, blue, alpha);
        vertex(buffer, matrix, p.x - x, p.y - y, p.z - z, red, green, blue, alpha);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
            int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static final class Scratch {
        private final Vector3f side = new Vector3f();
        private final Vector3f normal = new Vector3f();
    }

    private DysonPrimitiveEmitter() {}
}
