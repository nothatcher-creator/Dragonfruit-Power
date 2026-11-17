package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Expanding shockwave ring with a soft outer halo and particles riding its edge. */
public class RingEffect extends VfxEffect {
    private final Vec3d center, normal;
    private final float radius;
    private final int color;
    private final int style;

    public RingEffect(Vec3d center, Vec3d normal, float radius, int color, int life, int style) {
        super(life);
        this.center = center;
        this.normal = normal.lengthSquared() < 1.0E-6 ? Gfx.UP : normal.normalize();
        this.radius = radius;
        this.color = color;
        this.style = style;
    }

    private double radiusAt(double p) {
        return radius * Gfx.easeOutCubic(p);
    }

    @Override
    protected void onTick(ClientWorld world) {
        double r = radiusAt((double) age / life);
        Vec3d[] b = Gfx.basis(normal);
        int n = Math.max(6, (int) (r * 2.5));
        for (int i = 0; i < n; i++) {
            double a = world.random.nextDouble() * Math.PI * 2;
            Vec3d at = center.add(b[0].multiply(Math.cos(a) * r)).add(b[1].multiply(Math.sin(a) * r));
            Particles.style(world, style, at, world.random);
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double p = progress(td);
        double r = radiusAt(p);
        double fade = Math.pow(1 - p, 1.5);
        double thick = 0.25 + radius * 0.06 * (1 - p);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        int segs = Math.max(24, Math.min(96, (int) (r * 8)));
        Gfx.ring(vc, m, center, normal, r, thick * 3.0, Gfx.argb(color, (float) (0.18 * fade)), segs);
        Gfx.ring(vc, m, center, normal, r, thick, Gfx.argb(color, (float) (0.7 * fade)), segs);
        Gfx.ring(vc, m, center, normal, r, thick * 0.3, Gfx.argb(0xFFFFFF, (float) (0.8 * fade)), segs);
    }
}
