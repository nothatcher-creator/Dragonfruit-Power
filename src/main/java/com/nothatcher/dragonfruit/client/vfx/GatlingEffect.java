package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Spawns a storm of short punching arms (solid + translucent after-images) in front of the owner. */
public class GatlingEffect extends VfxEffect {
    final int ownerId;
    private final Random random;
    private final float range;

    public GatlingEffect(int ownerId, int duration, long seed, float range) {
        super(duration);
        this.ownerId = ownerId;
        this.random = new Random(seed);
        this.range = range;
    }

    @Override
    protected void onTick(ClientWorld world) {
        Entity e = world.getEntityById(ownerId);
        if (e == null) return;
        Vec3d eye = Poses.eye(e, 1f);
        Vec3d look = Poses.look(e, 1f);
        Vec3d right = Poses.rightOf(look);
        Vec3d up = right.crossProduct(look);
        boolean fp = Poses.isFirstPersonOwner(e);

        for (int k = 0; k < 3; k++) {
            int side = (age + k) % 2 == 0 ? 1 : -1;
            Vec3d dir = look.add(right.multiply((random.nextDouble() - 0.5) * 0.7))
                    .add(up.multiply((random.nextDouble() - 0.5) * 0.5)).normalize();
            double dist = range * (0.55 + random.nextDouble() * 0.45);
            Vec3d target = eye.add(dir.multiply(dist));
            boolean ghost = k > 0;
            double jx = (random.nextDouble() - 0.5) * (fp ? 0.5 : 0.6);
            double jy = (random.nextDouble() - 0.5) * (fp ? 0.3 : 0.4);
            VfxManager.add(new ArmEffect(ownerId, side, target, 2, 0, 2, ghost ? 0.95f : 1.05f, ghost ? 0.42f : 1f, jx, jy));
            if (random.nextInt(3) == 0) Particles.add(ParticleTypes.CRIT, target, dir.multiply(0.2));
        }
        if (age % 3 == 0) {
            Vec3d front = eye.add(look.multiply(range * 0.6));
            Particles.add(ParticleTypes.SWEEP_ATTACK, front.add(right.multiply((random.nextDouble() - 0.5) * 2)), Vec3d.ZERO);
            Particles.add(ParticleTypes.CLOUD, front, look.multiply(0.3));
        }
    }
}
