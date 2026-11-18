package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Gravity: a column of rings that keeps slamming down onto the ground, with a glowing cylinder. */
public class PressEffect extends VfxEffect {
    private final Vec3d center;
    private final float radius;
    private final int color;

    public PressEffect(Vec3d center, float radius, int color, int life) {
        super(life);
        this.center = center;
        this.radius = radius;
        this.color = color;
    }

    @Override
    protected void onTick(ClientWorld w) {
        for (int i = 0; i < 6; i++) {
            double a = w.random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(w.random.nextDouble()) * radius;
            Vec3d p = center.add(Math.cos(a) * r, 6 + w.random.nextDouble() * 4, Math.sin(a) * r);
            Particles.add(ParticleTypes.REVERSE_PORTAL, p, new Vec3d(0, -0.6, 0));
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double t = age + td;
        double p = progress(td);
        double fade = p < 0.8 ? 1 : (1 - p) / 0.2;
        double height = 9;
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        for (int k = 0; k < 5; k++) {
            double phase = ((t / 10.0) + k / 5.0) % 1.0;
            double y = height * (1 - phase * phase);
            double r = radius * (0.85 + 0.15 * phase);
            Gfx.ring(vc, m, center.add(0, y, 0), Gfx.UP, r, 0.25 + phase * 0.4, Gfx.argb(color, (float) (0.55 * phase * fade)), 48);
        }
        // translucent cylinder wall
        int segs = 40;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            Vec3d b0 = center.add(Math.cos(a0) * radius, 0.05, Math.sin(a0) * radius);
            Vec3d b1 = center.add(Math.cos(a1) * radius, 0.05, Math.sin(a1) * radius);
            Gfx.quad(vc, m, b0, b1, b1.add(0, height, 0), b0.add(0, height, 0), Gfx.argb(color, (float) (0.07 * fade)));
        }
        double pulse = 0.5 + 0.5 * Math.sin(t * 0.9);
        Gfx.ring(vc, m, center.add(0, 0.08, 0), Gfx.UP, radius, 0.5, Gfx.argb(color, (float) ((0.4 + 0.3 * pulse) * fade)), 64);
    }
}
