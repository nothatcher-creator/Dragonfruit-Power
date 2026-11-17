package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Quake: the air itself cracks like glass. Jagged white fractures spreading from the impact point. */
public class AirCrackEffect extends VfxEffect {
    private record Seg(Vec3d a, Vec3d b, double order, double width) {}

    private final Vec3d center;
    private final List<Seg> segs = new ArrayList<>();

    public AirCrackEffect(Vec3d center, Vec3d normal, float size, int life, long seed) {
        super(life);
        this.center = center;
        Random r = new Random(seed);
        Vec3d[] b = Gfx.basis(normal);
        int rays = 9;
        for (int i = 0; i < rays; i++) {
            double angle = Math.PI * 2 * i / rays + (r.nextDouble() - 0.5) * 0.4;
            Vec3d prev = center;
            int steps = 6;
            for (int j = 1; j <= steps; j++) {
                angle += (r.nextDouble() - 0.5) * 0.9;
                double step = size * 0.2 * (0.7 + r.nextDouble() * 0.6);
                Vec3d dir = b[0].multiply(Math.cos(angle)).add(b[1].multiply(Math.sin(angle)));
                Vec3d next = prev.add(dir.multiply(step)).add(normal.multiply((r.nextDouble() - 0.5) * 0.06));
                double width = 0.09 * (1.0 - j / (double) (steps + 2));
                segs.add(new Seg(prev, next, j / (double) steps, width));
                if (r.nextInt(3) == 0 && j < steps - 1) {
                    Vec3d bp = next;
                    double ba = angle + (r.nextBoolean() ? 1 : -1) * (0.6 + r.nextDouble() * 0.6);
                    for (int k = 1; k <= 3; k++) {
                        ba += (r.nextDouble() - 0.5) * 0.7;
                        Vec3d bd = b[0].multiply(Math.cos(ba)).add(b[1].multiply(Math.sin(ba)));
                        Vec3d bn = bp.add(bd.multiply(size * 0.12));
                        segs.add(new Seg(bp, bn, (j + k * 0.5) / steps, width * 0.6));
                        bp = bn;
                    }
                }
                prev = next;
            }
        }
        // concentric fracture rings
        for (int ring = 1; ring <= 2; ring++) {
            double rad = size * 0.35 * ring;
            int n = 10 + ring * 4;
            Vec3d prev = null;
            for (int i = 0; i <= n; i++) {
                double a = Math.PI * 2 * i / n;
                double jitter = 1 + (r.nextDouble() - 0.5) * 0.25;
                Vec3d p = center.add(b[0].multiply(Math.cos(a) * rad * jitter)).add(b[1].multiply(Math.sin(a) * rad * jitter));
                if (prev != null && r.nextInt(4) != 0) segs.add(new Seg(prev, p, 0.4 + ring * 0.25, 0.035));
                prev = p;
            }
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double t = age + td;
        double spread = Gfx.clamp01(t / 4.0);
        double fade = t < life * 0.6 ? 1.0 : Gfx.clamp01(1 - (t - life * 0.6) / (life * 0.4));
        double flicker = 0.85 + 0.15 * Math.sin(t * 3.1);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        for (Seg s : segs) {
            if (s.order() > spread * 1.05) continue;
            Gfx.ribbon(vc, m, s.a(), s.b(), s.width() * 3.5, Gfx.argb(0xCFE8FF, (float) (0.18 * fade)));
            Gfx.ribbon(vc, m, s.a(), s.b(), s.width(), Gfx.argb(0xFFFFFF, (float) (0.95 * fade * flicker)));
        }
        Gfx.sphere(vc, m, center, 0.18 + 0.1 * Math.sin(t), Gfx.argb(0xFFFFFF, (float) (0.8 * fade)), 6, 8);
    }
}
