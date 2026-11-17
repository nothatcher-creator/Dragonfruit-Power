package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Firefly Swarm: glowing embers circle you, then each dives into a target. */
public class FireflyEffect extends VfxEffect {
    private final int ownerId;
    private final int[] targets;
    private final int orbit, fly;
    private final int count;
    private Vec3d[] released;

    public FireflyEffect(int ownerId, int[] targets, int orbit, int fly) {
        super(orbit + fly + 1);
        this.ownerId = ownerId;
        this.targets = targets;
        this.orbit = orbit;
        this.fly = fly;
        this.count = Math.max(10, targets.length);
    }

    private Vec3d orbitPos(Entity owner, int i, double t, float td) {
        double a = Math.PI * 2 * i / count + t * 0.35;
        double rad = 1.3 + 0.2 * Math.sin(t * 0.4 + i);
        double h = owner.getHeight() * 0.6 + Math.sin(t * 0.3 + i * 1.7) * 0.35 + t * 0.02;
        return owner.getLerpedPos(td).add(Math.cos(a) * rad, h, Math.sin(a) * rad);
    }

    private Vec3d targetPos(Entity owner, int i, float td) {
        if (targets.length > 0 && mc().world != null) {
            Entity t = mc().world.getEntityById(targets[i % targets.length]);
            if (t != null) return t.getLerpedPos(td).add(0, t.getHeight() * 0.5, 0);
        }
        Vec3d look = owner.getRotationVec(td);
        double spread = (i - count / 2.0) * 0.12;
        return owner.getCameraPosVec(td).add(look.rotateY((float) spread).multiply(18));
    }

    private Vec3d pos(Entity owner, int i, double t, float td) {
        if (t < orbit || released == null) return orbitPos(owner, i, t, td);
        double f = Gfx.clamp01((t - orbit) / fly);
        Vec3d from = released[i];
        Vec3d to = targetPos(owner, i, td);
        double ease = f * f;
        return from.lerp(to, ease).add(0, Math.sin(Math.PI * f) * 1.6, 0);
    }

    @Override
    protected void onTick(ClientWorld world) {
        Entity owner = world.getEntityById(ownerId);
        if (owner == null) return;
        if (age == orbit) {
            released = new Vec3d[count];
            for (int i = 0; i < count; i++) released[i] = orbitPos(owner, i, age, 1f);
        }
        for (int i = 0; i < count; i++) {
            Vec3d p = pos(owner, i, age, 1f);
            Particles.add(ParticleTypes.FLAME, p, Vec3d.ZERO);
            if (age > orbit) Particles.add(ParticleTypes.SMALL_FLAME, p, Vec3d.ZERO);
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        if (mc().world == null) return;
        Entity owner = mc().world.getEntityById(ownerId);
        if (owner == null) return;
        double t = age + td;
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        for (int i = 0; i < count; i++) {
            Vec3d p = pos(owner, i, t, td);
            double flick = 1 + 0.25 * Math.sin(t * 2.7 + i * 1.3);
            Gfx.sphere(vc, m, p, 0.26 * flick, Gfx.argb(0xFF7A1A, 0.3f), 5, 8);
            Gfx.sphere(vc, m, p, 0.12 * flick, Gfx.argb(0xFFE59A, 0.9f), 4, 6);
        }
    }
}
