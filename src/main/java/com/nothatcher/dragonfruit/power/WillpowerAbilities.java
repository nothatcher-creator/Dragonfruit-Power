package com.nothatcher.dragonfruit.power;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Sense, Armor, Dominance. Usable by everyone, even without a fruit. */
public final class WillpowerAbilities {
    private WillpowerAbilities() {}

    public static boolean run(ServerPlayerEntity p, int index) {
        return switch (index) {
            case 0 -> sense(p);
            case 1 -> armor(p);
            case 2 -> dominance(p);
            default -> false;
        };
    }

    /** See every living thing nearby through walls and dodge 40% of melee hits for 15 seconds. */
    private static boolean sense(ServerPlayerEntity p) {
        ServerWorld w = Abilities.world(p);
        int duration = 300;
        PowerManager.setSense(p, duration);
        for (LivingEntity e : Abilities.inRadius(w, p.getPos(), 40, p)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, duration, 0, false, false, false));
        }
        Vfx.ring(w, p.getPos().add(0, 1, 0), Vfx.UP, 40f, 0xCFE8FF, 26, Vfx.S_NONE);
        Vfx.aura(w, p, Vfx.A_SENSE, duration);
        Vfx.fov(p, -0.08f, 20);
        Abilities.sound(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 1.6f);
        Abilities.sound(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
        return true;
    }

    /** Black, hardened fists: more melee damage, stronger rubber punches, damage resistance for 20 seconds. */
    private static boolean armor(ServerPlayerEntity p) {
        ServerWorld w = Abilities.world(p);
        int duration = 400;
        PowerManager.setArmor(p, duration);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 1, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0, false, false, true));
        Vfx.aura(w, p, Vfx.A_ARMOR, duration);
        Vfx.burst(w, p.getPos().add(0, 1.2, 0), Vec3d.ZERO, Vfx.B_ARMOR, 1f);
        Vfx.impact(w, p.getEyePos().add(p.getRotationVec(1f).multiply(1.2)), 1.4f, 0x8A8AA0, 6);
        Abilities.sound(p, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 1.5f, 0.7f);
        Abilities.sound(p, SoundEvents.BLOCK_ANVIL_PLACE, 0.6f, 1.6f);
        return true;
    }

    /** A wave of pure will: weak mobs collapse, strong ones stagger. Red-black lightning around you. */
    private static boolean dominance(ServerPlayerEntity p) {
        ServerWorld w = Abilities.world(p);
        Vec3d c = p.getPos();
        Random r = p.getRandom();
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10 + r.nextDouble() * 0.4;
            Vec3d from = c.add(0, 1.2, 0);
            Vec3d to = c.add(Math.cos(a) * (6 + r.nextDouble() * 8), 0.2 + r.nextDouble() * 3, Math.sin(a) * (6 + r.nextDouble() * 8));
            Vfx.bolt(w, from, to, 0xC0102A, 0.45f, 14, r.nextLong());
        }
        Vfx.ring(w, c.add(0, 0.2, 0), Vfx.UP, 26f, 0xB0001A, 26, Vfx.S_DUST);
        Vfx.ring(w, c.add(0, 1.2, 0), Vfx.UP, 20f, 0x6A0A8A, 20, Vfx.S_NONE);
        Vfx.shake(w, c, 1.2f, 30, 40);
        Vfx.speedLines(p, 0.7f, 20);
        Abilities.sound(p, SoundEvents.ENTITY_WARDEN_ROAR, 1.5f, 0.6f);
        Abilities.sound(p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);
        for (LivingEntity e : Abilities.inRadius(w, c, 24, p)) {
            if (e instanceof ServerPlayerEntity other) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                continue;
            }
            if (e.getMaxHealth() <= 40) {
                // knocked out
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 7));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 4));
                if (e instanceof MobEntity mob) mob.setTarget(null);
                Abilities.hurt(p, e, 6f, true);
                Vfx.burst(w, e.getPos().add(0, e.getHeight(), 0), Vec3d.ZERO, Vfx.B_KNOCKOUT, 1f);
            } else {
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
            }
        }
        return true;
    }
}
