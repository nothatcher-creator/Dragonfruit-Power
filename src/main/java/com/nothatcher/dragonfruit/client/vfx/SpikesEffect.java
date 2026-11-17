package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Ice Age: a wave of ice crystals erupts out of the ground from the centre outward, then shatters. */
public class SpikesEffect extends VfxEffect {
    private record Spike(Vec3d base, Vec3d axis, double height, double radius, int delay) {}

    private final Vec3d center;
    private final float radius;
    private final int count;
    private final long seed;
    private List<Spike> spikes;

    public SpikesEffect(Vec3d center, float radius, int count, int life, long seed) {
        super(life);
        this.center = center;
        this.radius = radius;
        this.count = count;
        this.seed = seed;
    }

    private void build(ClientWorld w) {
        spikes = new ArrayList<>();
        Random r = new Random(seed);
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double dist = 1.5 + Math.sqrt(r.nextDouble()) * (radius - 1.5);
            Vec3d at = center.add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
            BlockPos g = Particles.ground(w, at, 3, 6);
            if (g == null) continue;
            Vec3d base = new Vec3d(at.x, g.getY() + 1.0, at.z);
            Vec3d outward = new Vec3d(Math.cos(a), 0, Math.sin(a));
            Vec3d axis = Gfx.UP.add(outward.multiply(0.25 + r.nextDouble() * 0.35))
                    .add((r.nextDouble() - 0.5) * 0.3, 0, (r.nextDouble() - 0.5) * 0.3).normalize();
            double h = 1.2 + r.nextDouble() * 2.6 * (1.1 - dist / radius * 0.5);
            spikes.add(new Spike(base, axis, h, 0.22 + r.nextDouble() * 0.25, (int) (dist / radius * 8)));
        }
        // a big central cluster
        BlockPos g = Particles.ground(w, center, 2, 4);
        if (g != null) {
            Vec3d base = new Vec3d(center.x, g.getY() + 1.0, center.z);
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2 * i / 5;
                Vec3d axis = Gfx.UP.add(Math.cos(a) * 0.45, 0, Math.sin(a) * 0.45).normalize();
                spikes.add(new Spike(base.add(Math.cos(a) * 1.0, 0, Math.sin(a) * 1.0), axis, 3.5 + r.nextDouble(), 0.5, 0));
            }
        }
    }

    private double grow(Spike s, double t) {
        double local = t - s.delay();
        if (local <= 0) return 0;
        double up = Gfx.easeOutCubic(local / 3.0);
        double sinkStart = life - 10;
        if (t > sinkStart) up *= Gfx.clamp01(1 - (t - sinkStart) / 10.0);
        return up;
    }

    @Override
    protected void onTick(ClientWorld world) {
        if (spikes == null) build(world);
        BlockStateParticleEffect ice = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState());
        for (Spike s : spikes) {
            if (age == s.delay()) {
                for (int i = 0; i < 5; i++) Particles.add(ParticleTypes.SNOWFLAKE, s.base(), Particles.randDir(world.random).multiply(0.15).add(0, 0.15, 0));
                Particles.add(ice, s.base(), new Vec3d(0, 0.3, 0));
            }
            if (age == life - 10) {
                Vec3d mid = s.base().add(s.axis().multiply(s.height() * 0.5));
                for (int i = 0; i < 6; i++) Particles.add(ice, mid.add(Particles.randDir(world.random).multiply(0.3)), Particles.randDir(world.random).multiply(0.2));
            }
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        if (spikes == null) return;
        double t = age + td;
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer solid = vcp.getBuffer(RenderLayer.getDebugQuads());
        for (Spike s : spikes) {
            double g = grow(s, t);
            if (g <= 0.01) continue;
            Gfx.crystal(solid, m, s.base(), s.axis(), s.height() * g, s.radius(), Gfx.argb(0xA8E6FF, 0.62f));
        }
        VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
        for (Spike s : spikes) {
            double g = grow(s, t);
            if (g <= 0.01) continue;
            Gfx.crystal(glow, m, s.base(), s.axis(), s.height() * g * 1.04, s.radius() * 1.15, Gfx.argb(0x7FD8FF, 0.22f));
        }
    }
}
