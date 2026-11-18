package com.nothatcher.dragonfruit.power;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** Flying slashes and the spinning tornado. Three-sword versions when carrying all three katanas. */
public final class SwordAbilities {
    private SwordAbilities() {}

    public static void slash(ServerPlayerEntity p, boolean three, int color) {
        ServerWorld w = Abilities.world(p);
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        float[] rolls = three ? new float[]{-35f, 0f, 35f} : new float[]{(p.getRandom().nextBoolean() ? 1 : -1) * (10f + p.getRandom().nextFloat() * 25f)};
        float[] yaws = three ? new float[]{-5f, 0f, 5f} : new float[]{0f};
        int[] colors = three ? new int[]{0xE8F4FF, 0xFF2A3A, 0x8A4CFF} : new int[]{color};
        for (int i = 0; i < rolls.length; i++) {
            final Vec3d dir = look.rotateY((float) Math.toRadians(yaws[i])).normalize();
            final Vec3d start = eye.add(dir.multiply(1.0)).add(0, -0.25, 0);
            final float width = three ? 3.0f : 2.6f;
            final float damage = three ? 9f : 10f;
            final int c = colors[i];
            final float roll = rolls[i];
            Scheduler.after(i * 2, () -> {
                if (!Abilities.valid(p)) return;
                Vfx.slash(w, start, dir, 1.8f, 16, width, roll, c);
                Abilities.simulate(p, w, start, dir, 1.8f, 16, 1.3f, hit -> {
                    for (LivingEntity e : Abilities.inRadius(w, hit, 2.2, p)) {
                        Abilities.hurt(p, e, damage, true);
                        Abilities.push(e, dir.multiply(0.6).add(0, 0.2, 0));
                    }
                    Vfx.burst(w, hit, dir, Vfx.B_SLASH_HIT, 1f);
                    Vfx.impact(w, hit, 1.6f, c, 5);
                    Abilities.sound(w, hit, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
                });
            });
            Abilities.sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f + i * 0.15f);
        }
        if (three) Vfx.speedLines(p, 0.5f, 10);
        Vfx.fov(p, 0.08f, 6);
    }

    public static void tornado(ServerPlayerEntity p, boolean three, int color) {
        ServerWorld w = Abilities.world(p);
        Vec3d c = p.getPos().add(0, 1, 0);
        double radius = three ? 7 : 5;
        int n = three ? 12 : 8;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3d dir = new Vec3d(Math.cos(a), 0.05, Math.sin(a));
            int col = three ? new int[]{0xE8F4FF, 0xFF2A3A, 0x8A4CFF}[i % 3] : color;
            Vfx.slash(w, c.add(dir.multiply(0.8)), dir, 0.9f, 9, three ? 2.4f : 2.0f, 0f, col);
        }
        Vfx.ring(w, c.add(0, -0.9, 0), Vfx.UP, (float) radius + 1, color, 14, Vfx.S_DUST);
        Vfx.shake(w, c, 0.5f, 10, 14);
        for (LivingEntity e : Abilities.inRadius(w, c, radius, p)) {
            Abilities.hurt(p, e, three ? 12f : 8f, true);
            Vec3d out = e.getPos().subtract(p.getPos()).multiply(1, 0, 1).normalize();
            Abilities.push(e, out.multiply(0.7).add(0, 1.0, 0));
        }
        Abilities.launch(p, p.getVelocity().add(0, 0.45, 0));
        Abilities.sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
        Abilities.sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.9f);
        Abilities.sound(p, SoundEvents.ENTITY_PHANTOM_FLAP, 1.5f, 0.6f);
    }
}
