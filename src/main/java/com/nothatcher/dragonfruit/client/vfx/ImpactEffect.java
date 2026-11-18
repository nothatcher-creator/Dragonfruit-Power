package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Random;

/** Anime impact frame: a burst of sharp spikes that always faces the camera, with a hot white centre. */
public class ImpactEffect extends VfxEffect {
    private final Vec3d pos;
    private final float size;
    private final int color;
    private final double[] angles, lengths;

    public ImpactEffect(Vec3d pos, float size, int color, int life) {
        super(life);
        this.pos = pos;
        this.size = size;
        this.color = color;
        Random r = new Random(Double.doubleToLongBits(pos.x * 31 + pos.z));
        int n = 16;
        angles = new double[n];
        lengths = new double[n];
        for (int i = 0; i < n; i++) {
            angles[i] = Math.PI * 2 * i / n + (r.nextDouble() - 0.5) * 0.3;
            lengths[i] = 0.55 + r.nextDouble() * 0.6;
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double p = progress(td);
        double grow = Gfx.easeOutCubic(Math.min(1, p * 3));
        double fade = p < 0.4 ? 1 : 1 - (p - 0.4) / 0.6;
        Vec3d normal = Gfx.cam.subtract(pos);
        if (normal.lengthSquared() < 1.0E-6) return;
        Vec3d[] b = Gfx.basis(normal);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        Vec3d c = pos.add(normal.normalize().multiply(0.3));
        for (int i = 0; i < angles.length; i++) {
            double a = angles[i];
            double half = 0.07 * (1 - p * 0.6);
            Vec3d d = b[0].multiply(Math.cos(a)).add(b[1].multiply(Math.sin(a)));
            Vec3d side = b[0].multiply(Math.cos(a + Math.PI / 2)).add(b[1].multiply(Math.sin(a + Math.PI / 2)));
            double inner = size * 0.18 * grow;
            double outer = size * lengths[i] * grow;
            Vec3d base = c.add(d.multiply(inner));
            Vec3d tip = c.add(d.multiply(outer));
            double w = size * half;
            Gfx.tri(vc, m, base.add(side.multiply(w)), base.subtract(side.multiply(w)), tip, Gfx.argb(color, (float) (0.9 * fade)));
            Gfx.tri(vc, m, base.add(side.multiply(w * 2.2)), base.subtract(side.multiply(w * 2.2)), c.add(d.multiply(outer * 0.8)),
                    Gfx.argb(color, (float) (0.25 * fade)));
        }
        Gfx.sphere(vc, m, c, size * 0.22 * (1 - p * 0.5), Gfx.argb(0xFFFFFF, (float) (0.9 * fade)), 6, 10);
        Gfx.ring(vc, m, c, normal, size * (0.3 + 0.9 * p), size * 0.06, Gfx.argb(color, (float) (0.6 * fade)), 32);
    }
}
