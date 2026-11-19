package com.nothatcher.outbreak;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Shove (B): push whatever's in front of you back and stagger it for a moment. Look down at a staggered zombie and
 * shove again to stomp it.
 */
public final class Shove {
    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final Map<LivingEntity, Long> STAGGERED = new WeakHashMap<>();

    private Shove() {}

    public static boolean staggered(LivingEntity e) {
        Long t = STAGGERED.get(e);
        return t != null && t > e.getWorld().getTime();
    }

    public static void shove(ServerPlayerEntity p) {
        if (p.hasVehicle() || p.isSpectator() || p.isDead()) return;
        ServerWorld w = p.getServerWorld();
        long now = w.getTime();
        Long last = LAST.get(p.getUuid());
        if (last != null && now - last < 12) return;
        LAST.put(p.getUuid(), now);
        p.swingHand(Hand.MAIN_HAND, true);
        p.addExhaustion(0.3f);
        Vec3d look = p.getRotationVec(1f);
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        flat = flat.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : flat.normalize();
        boolean down = p.getPitch() > 50;
        boolean any = false;
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(2.4, 1.0, 2.4),
                e -> e != p && e.isAlive() && !(e instanceof PlayerEntity) && !(e instanceof TameableEntity t && t.isTamed()) && !e.hasVehicle())) {
            Vec3d to = e.getPos().subtract(p.getPos());
            Vec3d toFlat = new Vec3d(to.x, 0, to.z);
            double d = toFlat.length();
            if (d > 2.6) continue;
            if (d > 0.7 && toFlat.normalize().dotProduct(flat) < 0.45) continue;
            any = true;
            if (down && staggered(e)) {
                e.timeUntilRegen = 0;
                e.damage(w.getDamageSources().playerAttack(p), 6f);
                w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 0.6f);
                w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 0.5, e.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
                continue;
            }
            e.takeKnockback(0.85, -flat.x, -flat.z);
            e.velocityModified = true;
            if (e instanceof MobEntity m) {
                m.getNavigation().stop();
                m.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 35, 5, false, false));
                m.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 35, 4, false, false));
                STAGGERED.put(e, now + 35);
            }
            w.spawnParticles(ParticleTypes.CRIT, e.getX(), e.getBodyY(0.6), e.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
        }
        w.playSound(null, p.getX(), p.getY(), p.getZ(), any ? SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK : SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE,
                SoundCategory.PLAYERS, 0.9f, 0.9f + w.random.nextFloat() * 0.2f);
    }
}
