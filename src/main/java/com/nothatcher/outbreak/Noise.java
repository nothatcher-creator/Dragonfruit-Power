package com.nothatcher.outbreak;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Sound draws the dead. Gunshots and running engines give nearby zombies somewhere to go: they walk to the noise
 * and attack whatever they find there.
 */
public final class Noise {
    public record Target(Vec3d pos, long until) {}

    private static final Map<MobEntity, Target> TARGETS = new WeakHashMap<>();
    private static final Map<UUID, Vec3d> LAST = new HashMap<>();

    private Noise() {}

    public static void emit(ServerWorld w, Vec3d at, double radius) {
        Box box = new Box(at, at).expand(radius, radius * 0.6, radius);
        for (ZombieEntity z : w.getEntitiesByClass(ZombieEntity.class, box, z -> z.getTarget() == null)) {
            if (z.squaredDistanceTo(at) <= radius * radius) TARGETS.put(z, new Target(at, w.getTime() + 400));
        }
    }

    /** send one zombie somewhere, for a while (hordes on the move) */
    public static void send(MobEntity m, Vec3d at, int ticks) {
        TARGETS.put(m, new Target(at, m.getWorld().getTime() + ticks));
    }

    public static Target get(MobEntity m) {
        Target t = TARGETS.get(m);
        if (t != null && t.until < m.getWorld().getTime()) {
            TARGETS.remove(m);
            return null;
        }
        return t;
    }

    public static void clear(MobEntity m) {
        TARGETS.remove(m);
    }

    /** anyone driving (cars, boats, horses...) makes engine noise */
    static void vehicleNoise(ServerWorld w) {
        if (w.getTime() % 20 != 7) return;
        for (ServerPlayerEntity p : w.getPlayers()) {
            Entity v = p.getVehicle();
            Vec3d now = p.getPos();
            Vec3d before = LAST.put(p.getUuid(), now);
            if (v != null && before != null && before.squaredDistanceTo(now) > 9) emit(w, now, 32);
        }
    }
}
