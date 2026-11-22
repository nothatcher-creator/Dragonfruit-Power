package com.nothatcher.outbreak;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Things that happen to you. Every so often a horde of a dozen or more comes shambling down the streets towards
 * wherever you are. And every few days a helicopter thunders low over your head, and every zombie for a long way
 * round hears it and comes to see. Bloaters burst into a sickening cloud.
 */
public final class Events {
    private static final Map<UUID, Long> NEXT_HORDE = new HashMap<>();
    private static long nextHeli = -1, heliUntil = -1;
    private static UUID heliTarget;
    private static Vec3d heliFrom, heliTo;

    private Events() {}

    static void tick(ServerWorld w) {
        if (w.getRegistryKey() != World.OVERWORLD || w.getTime() % 20 != 3) return;
        if (!w.getGameRules().getBoolean(Outbreak.ZOMBIES) || !w.getGameRules().getBoolean(Outbreak.EVENTS) || w.getDifficulty() == Difficulty.PEACEFUL) return;
        long now = w.getTime();
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.isSpectator() || p.isCreative()) continue;
            long next = NEXT_HORDE.computeIfAbsent(p.getUuid(), u -> now + 12000 + w.random.nextInt(18000));
            if (now >= next) {
                NEXT_HORDE.put(p.getUuid(), now + (w.isNight() ? 9000 : 15000) + w.random.nextInt(18000));
                horde(w, p);
            }
        }
        helicopter(w, now);
    }

    // ------------------------------------------------------------------ hordes

    private static void horde(ServerWorld w, ServerPlayerEntity p) {
        Random r = w.random;
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, dist = 60 + r.nextDouble() * 25;
            int x = MathHelper.floor(p.getX() + Math.cos(a) * dist), z = MathHelper.floor(p.getZ() + Math.sin(a) * dist);
            if (!w.isChunkLoaded(new BlockPos(x, 0, z))) continue;
            BlockPos spot = ZombieSpawner.findSpot(w, x, p.getBlockY(), z);
            if (spot == null || Airport.at(w, spot)) continue;
            int n = 10 + r.nextInt(9);
            for (int i = 0; i < n; i++) {
                ZombieEntity zed = EntityType.ZOMBIE.create(w);
                if (zed == null) continue;
                zed.refreshPositionAndAngles(spot.getX() + 0.5 + r.nextGaussian() * 2.5, spot.getY(), spot.getZ() + 0.5 + r.nextGaussian() * 2.5, r.nextFloat() * 360, 0);
                if (!w.isSpaceEmpty(zed)) zed.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, r.nextFloat() * 360, 0);
                zed.initialize(w, w.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
                ZombieSpawner.dress(zed, r, w);
                zed.addCommandTag("outbreak_horde");
                w.spawnEntityAndPassengers(zed);
                Noise.send(zed, p.getPos().add(r.nextGaussian() * 4, 0, r.nextGaussian() * 4), 2400);
            }
            w.playSound(null, spot, SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 6f, 0.7f);
            p.sendMessage(Text.translatable("msg.outbreak.horde").formatted(Formatting.DARK_RED), true);
            return;
        }
    }

    // ------------------------------------------------------------------ the helicopter

    private static void helicopter(ServerWorld w, long now) {
        if (nextHeli < 0) nextHeli = now + 24000L * (2 + w.random.nextInt(2)) + w.random.nextInt(12000);
        if (heliUntil < 0) {
            if (now < nextHeli || w.getPlayers().isEmpty()) return;
            ServerPlayerEntity p = w.getPlayers().get(w.random.nextInt(w.getPlayers().size()));
            if (p.isSpectator() || p.isCreative()) {
                nextHeli = now + 6000;
                return;
            }
            heliTarget = p.getUuid();
            double a = w.random.nextDouble() * Math.PI * 2;
            Vec3d c = p.getPos();
            heliFrom = c.add(Math.cos(a) * 140, 30, Math.sin(a) * 140);
            heliTo = c.add(-Math.cos(a) * 140, 30, -Math.sin(a) * 140);
            heliUntil = now + 1200;
            for (ServerPlayerEntity q : w.getPlayers()) if (q.squaredDistanceTo(p) < 300 * 300)
                q.sendMessage(Text.translatable("msg.outbreak.helicopter").formatted(Formatting.GOLD), true);
            return;
        }
        if (now >= heliUntil) {
            heliUntil = -1;
            nextHeli = now + 24000L * (3 + w.random.nextInt(3)) + w.random.nextInt(12000);
            return;
        }
        ServerPlayerEntity p = w.getServer().getPlayerManager().getPlayer(heliTarget);
        if (p == null || p.getWorld() != w) {
            heliUntil = now;
            return;
        }
        // it sweeps past, then circles over you for a while, then heads off: loud the whole time
        double t = 1.0 - (heliUntil - now) / 1200.0;
        Vec3d at;
        if (t < 0.3) at = heliFrom.lerp(p.getPos().add(0, 30, 0), t / 0.3);
        else if (t < 0.7) {
            double ang = (t - 0.3) * 20;
            at = p.getPos().add(Math.cos(ang) * 18, 30, Math.sin(ang) * 18);
        } else at = p.getPos().add(0, 30, 0).lerp(heliTo, (t - 0.7) / 0.3);
        Identifier rotor = new Identifier("vehicleforge", "engine_heli");
        SoundEvent snd = Registries.SOUND_EVENT.containsId(rotor) ? Registries.SOUND_EVENT.get(rotor) : SoundEvents.ENTITY_PHANTOM_FLAP;
        w.playSound(null, at.x, at.y, at.z, snd, SoundCategory.AMBIENT, 12f, 0.9f);
        if (now % 40 == 3) Noise.emit(w, new Vec3d(at.x, p.getY(), at.z), 72);
    }

    // ------------------------------------------------------------------ bloaters

    static void burst(ServerWorld w, ZombieEntity z) {
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(w, z.getX(), z.getY() + 0.3, z.getZ());
        cloud.setRadius(3.5f);
        cloud.setRadiusOnUse(-0.2f);
        cloud.setWaitTime(5);
        cloud.setDuration(160);
        cloud.setRadiusGrowth(-3.5f / 160f);
        cloud.setColor(0x6B8E23);
        cloud.addEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 0));
        w.spawnEntity(cloud);
        w.spawnParticles(ParticleTypes.SNEEZE, z.getX(), z.getY() + 1, z.getZ(), 40, 0.6, 0.5, 0.6, 0.05);
        w.playSound(null, z.getX(), z.getY(), z.getZ(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.5f, 0.5f);
        Blood.splash(w, z.getBlockPos(), OutbreakBlocks.BLOOD_SPRAY.getDefaultState(), 2);
    }
}
