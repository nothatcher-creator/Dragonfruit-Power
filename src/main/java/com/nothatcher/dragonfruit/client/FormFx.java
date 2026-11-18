package com.nothatcher.dragonfruit.client;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.client.vfx.Particles;
import com.nothatcher.dragonfruit.client.vfx.Poses;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Particles streaming off every player in an elemental or beast form. */
public final class FormFx {
    private FormFx() {}

    public static void tick(MinecraftClient mc) {
        if (mc.world == null) return;
        Random r = mc.world.random;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (!ClientPowerData.inForm(p) || p.isInvisible()) continue;
            FruitType f = ClientPowerData.fruitOf(p);
            boolean fp = Poses.isFirstPersonOwner(p);
            int n = fp ? 1 : 4;
            for (int i = 0; i < n; i++) {
                double h = fp ? r.nextDouble() * 0.6 : r.nextDouble() * p.getHeight();
                Vec3d at = p.getPos().add((r.nextDouble() - 0.5) * 0.8, h, (r.nextDouble() - 0.5) * 0.8);
                switch (f) {
                    case FLAME -> Particles.add(ParticleTypes.FLAME, at, new Vec3d(0, 0.05 + r.nextDouble() * 0.06, 0));
                    case MAGMA -> {
                        Particles.add(r.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, at, new Vec3d(0, 0.05, 0));
                        if (r.nextInt(12) == 0) Particles.add(ParticleTypes.LAVA, at, Vec3d.ZERO);
                    }
                    case ICE -> Particles.add(ParticleTypes.SNOWFLAKE, at, new Vec3d(0, -0.02, 0));
                    case THUNDER -> Particles.add(ParticleTypes.ELECTRIC_SPARK, at, Particles.randDirPublic(r).multiply(0.2));
                    case LIGHT -> Particles.add(ParticleTypes.END_ROD, at, new Vec3d(0, 0.02, 0));
                    case DRAGON -> {
                        if (!p.isOnGround() && r.nextInt(4) == 0) Particles.add(ParticleTypes.CLOUD, at.add(0, -0.5, 0), new Vec3d(0, -0.1, 0));
                    }
                    default -> { }
                }
            }
        }
    }
}
