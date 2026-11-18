package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** A shaded rock wrapped in fire, accelerating down from the sky with a long burning trail. */
public class MeteorEffect extends VfxEffect {
    private final Vec3d start, end;
    private final float radius;

    public MeteorEffect(Vec3d start, Vec3d end, int fallTicks, float radius) {
        super(fallTicks);
        this.start = start;
        this.end = end;
        this.radius = radius;
    }

    private Vec3d pos(double t) {
        double f = Gfx.clamp01(t / life);
        return start.lerp(end, Math.pow(f, 1.6));
    }

    @Override
    protected void onTick(ClientWorld w) {
        Vec3d p = pos(age);
        Vec3d dir = end.subtract(start).normalize();
        for (int i = 0; i < 30; i++) {
            Vec3d at = p.add(Particles.randDir(w.random).multiply(radius * (0.8 + w.random.nextDouble() * 0.5)));
            Particles.add(ParticleTypes.FLAME, at, dir.multiply(-0.3).add(Particles.randDir(w.random).multiply(0.05)));
        }
        for (int i = 0; i < 6; i++) {
            Particles.add(ParticleTypes.LARGE_SMOKE, p.subtract(dir.multiply(radius * 1.5)).add(Particles.randDir(w.random).multiply(radius)), dir.multiply(-0.1));
        }
        if (w.random.nextInt(2) == 0) Particles.add(ParticleTypes.LAVA, p, Vec3d.ZERO);
        Particles.add(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.subtract(dir.multiply(radius * 2)), new Vec3d(0, 0.02, 0));
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double t = age + td;
        Vec3d p = pos(t);
        Vec3d dir = end.subtract(start).normalize();
        Matrix4f m = ms.peek().getPositionMatrix();
        double spin = t * 0.15;
        VertexConsumer solid = vcp.getBuffer(RenderLayer.getDebugQuads());
        Gfx.shadedSphere(solid, m, p, radius, 0x4A3326, 1f, 10, 14, new Vec3d(Math.cos(spin), 0.8, Math.sin(spin)));
        VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
        double pulse = 1 + 0.05 * Math.sin(t * 2.1);
        Gfx.sphere(glow, m, p, radius * 1.15 * pulse, Gfx.argb(0xFF6A1A, 0.45f), 8, 12);
        Gfx.sphere(glow, m, p, radius * 1.6 * pulse, Gfx.argb(0xFF3A00, 0.18f), 8, 12);
        Vec3d tail = p.subtract(dir.multiply(radius * 9));
        Gfx.ribbon(glow, m, p, tail, radius * 2.4, Gfx.argb(0xFF5A10, 0.3f));
        Gfx.ribbon(glow, m, p, p.subtract(dir.multiply(radius * 5)), radius * 1.2, Gfx.argb(0xFFD27A, 0.5f));
    }
}
