package com.nothatcher.dragonfruit.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

/**
 * Server-side senders for every visual effect. The client decodes these in client/vfx/VfxDecoder.
 * Keep the field order in each method identical to the decoder.
 */
public final class Vfx {
    public static final int ARM = 1, GATLING = 2, BEAM = 3, RING = 4, ORB = 5, SPIKES = 6, ICE_COFFIN = 7,
            AIR_CRACK = 8, PROJECTILE = 9, FIREFLIES = 10, SHAKE = 11, FLASH = 12, AURA = 13, BURST = 14, FOV = 15,
            BOLT = 16, METEOR = 17, IMPACT = 18, PRESS = 19, SPEEDLINES = 20, SLASH = 21;

    // burst kinds (one-shot particle explosions)
    public static final int B_RUBBER_HIT = 0, B_FIRE_BIG = 1, B_FIRE_SMALL = 2, B_FLAME_PILLAR = 3, B_FROST = 4,
            B_ICE_SHATTER = 5, B_LIGHT = 6, B_LIGHT_SMALL = 7, B_QUAKE_LINE = 8, B_GROUND_QUAKE = 9, B_STEAM = 10,
            B_AWAKEN = 11, B_BLOCK_HIT = 12, B_THUNDER = 13, B_MAGMA = 14, B_ERUPTION = 15, B_GRAVITY = 16,
            B_ARMOR = 17, B_KNOCKOUT = 18, B_CANNON = 19, B_SPLASH = 20, B_SLASH_HIT = 21,
            B_BREATH = 22, B_HEAL = 23, B_TRANSFORM = 24;

    // aura kinds (particles following an entity)
    public static final int A_STEAM = 0, A_FLAME = 1, A_FROST = 2, A_LIGHT = 3, A_THUNDER = 4, A_SENSE = 5, A_ARMOR = 6;

    // particle styles for beams / rings / orbs
    public static final int S_NONE = 0, S_DUST = 1, S_FLAME = 2, S_FROST = 3, S_LIGHT = 4, S_QUAKE = 5,
            S_THUNDER = 6, S_GRAVITY = 7;

    // projectile kinds
    public static final int P_FIRE_FIST = 0, P_ICE_SPEAR = 1, P_MAGMA = 2, P_CANNON = 3;

    // orb attach modes
    public static final int ATTACH_NONE = 0, ATTACH_CENTER = 1, ATTACH_HAND = 2;

    public static final Vec3d UP = new Vec3d(0, 1, 0);
    private static final double RANGE = 128;

    private Vfx() {}

    private static void broadcast(ServerWorld world, Vec3d at, int type, Consumer<PacketByteBuf> writer) {
        for (ServerPlayerEntity p : PlayerLookup.around(world, at, RANGE)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeByte(type);
            writer.accept(buf);
            ServerPlayNetworking.send(p, Net.VFX, buf);
        }
    }

    private static void vec(PacketByteBuf b, Vec3d v) {
        b.writeDouble(v.x);
        b.writeDouble(v.y);
        b.writeDouble(v.z);
    }

    private static ServerWorld world(Entity e) {
        return (ServerWorld) e.getWorld();
    }

    /** A stretching rubber arm from the owner's shoulder (side 1 = right, -1 = left) to target. */
    public static void arm(ServerPlayerEntity owner, int side, Vec3d target, int extend, int hold, int retract, float thickness) {
        broadcast(world(owner), owner.getPos(), ARM, b -> {
            b.writeVarInt(owner.getId());
            b.writeByte(side);
            vec(b, target);
            b.writeByte(extend);
            b.writeByte(hold);
            b.writeByte(retract);
            b.writeFloat(thickness);
        });
    }

    public static void gatling(ServerPlayerEntity owner, int duration, long seed, float range) {
        broadcast(world(owner), owner.getPos(), GATLING, b -> {
            b.writeVarInt(owner.getId());
            b.writeVarInt(duration);
            b.writeLong(seed);
            b.writeFloat(range);
        });
    }

    public static void beam(ServerWorld w, Vec3d from, Vec3d to, int color, float width, int life, int style) {
        broadcast(w, from, BEAM, b -> {
            vec(b, from);
            vec(b, to);
            b.writeInt(color);
            b.writeFloat(width);
            b.writeVarInt(life);
            b.writeByte(style);
        });
    }

    public static void ring(ServerWorld w, Vec3d center, Vec3d normal, float radius, int color, int life, int style) {
        broadcast(w, center, RING, b -> {
            vec(b, center);
            vec(b, normal);
            b.writeFloat(radius);
            b.writeInt(color);
            b.writeVarInt(life);
            b.writeByte(style);
        });
    }

    public static void orb(ServerWorld w, Vec3d center, float radius, int color, int life, int attachEntity, int attachMode, boolean charging, int style) {
        broadcast(w, center, ORB, b -> {
            vec(b, center);
            b.writeFloat(radius);
            b.writeInt(color);
            b.writeVarInt(life);
            b.writeVarInt(attachEntity);
            b.writeByte(attachMode);
            b.writeBoolean(charging);
            b.writeByte(style);
        });
    }

    public static void spikes(ServerWorld w, Vec3d center, float radius, int count, int life, long seed) {
        broadcast(w, center, SPIKES, b -> {
            vec(b, center);
            b.writeFloat(radius);
            b.writeVarInt(count);
            b.writeVarInt(life);
            b.writeLong(seed);
        });
    }

    public static void iceCoffin(ServerWorld w, Entity target, int life) {
        broadcast(w, target.getPos(), ICE_COFFIN, b -> {
            b.writeVarInt(target.getId());
            b.writeVarInt(life);
        });
    }

    public static void airCrack(ServerWorld w, Vec3d pos, Vec3d normal, float size, int life, long seed) {
        broadcast(w, pos, AIR_CRACK, b -> {
            vec(b, pos);
            vec(b, normal);
            b.writeFloat(size);
            b.writeVarInt(life);
            b.writeLong(seed);
        });
    }

    public static void projectile(ServerWorld w, int ownerId, int kind, Vec3d start, Vec3d dir, float speed, int life, float size) {
        broadcast(w, start, PROJECTILE, b -> {
            b.writeVarInt(ownerId);
            b.writeByte(kind);
            vec(b, start);
            vec(b, dir);
            b.writeFloat(speed);
            b.writeVarInt(life);
            b.writeFloat(size);
        });
    }

    public static void fireflies(ServerPlayerEntity owner, int[] targets, int orbitTicks, int flyTicks) {
        broadcast(world(owner), owner.getPos(), FIREFLIES, b -> {
            b.writeVarInt(owner.getId());
            b.writeVarInt(targets.length);
            for (int t : targets) b.writeVarInt(t);
            b.writeVarInt(orbitTicks);
            b.writeVarInt(flyTicks);
        });
    }

    public static void shake(ServerWorld w, Vec3d center, float intensity, int duration, float radius) {
        broadcast(w, center, SHAKE, b -> {
            vec(b, center);
            b.writeFloat(intensity);
            b.writeVarInt(duration);
            b.writeFloat(radius);
        });
    }

    public static void flash(ServerWorld w, Vec3d center, float intensity, float radius) {
        broadcast(w, center, FLASH, b -> {
            vec(b, center);
            b.writeFloat(intensity);
            b.writeFloat(radius);
        });
    }

    public static void aura(ServerWorld w, Entity entity, int kind, int duration) {
        broadcast(w, entity.getPos(), AURA, b -> {
            b.writeVarInt(entity.getId());
            b.writeByte(kind);
            b.writeVarInt(duration);
        });
    }

    /** dir doubles as an RGB colour (0..1) for B_AWAKEN. */
    public static void burst(ServerWorld w, Vec3d pos, Vec3d dir, int kind, float scale) {
        broadcast(w, pos, BURST, b -> {
            vec(b, pos);
            vec(b, dir);
            b.writeByte(kind);
            b.writeFloat(scale);
        });
    }

    /** FOV punch only for the player who used the ability. */
    public static void fov(ServerPlayerEntity player, float amount, int duration) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(FOV);
        buf.writeFloat(amount);
        buf.writeVarInt(duration);
        ServerPlayNetworking.send(player, Net.VFX, buf);
    }

    /** Jagged lightning bolt between two points (re-jags every couple of ticks). */
    public static void bolt(ServerWorld w, Vec3d from, Vec3d to, int color, float width, int life, long seed) {
        broadcast(w, to, BOLT, b -> {
            vec(b, from);
            vec(b, to);
            b.writeInt(color);
            b.writeFloat(width);
            b.writeVarInt(life);
            b.writeLong(seed);
        });
    }

    /** Burning meteor falling from start to end over fallTicks. */
    public static void meteor(ServerWorld w, Vec3d start, Vec3d end, int fallTicks, float radius) {
        broadcast(w, end, METEOR, b -> {
            vec(b, start);
            vec(b, end);
            b.writeVarInt(fallTicks);
            b.writeFloat(radius);
        });
    }

    /** Anime impact star (camera-facing spikes + flash). */
    public static void impact(ServerWorld w, Vec3d pos, float size, int color, int life) {
        broadcast(w, pos, IMPACT, b -> {
            vec(b, pos);
            b.writeFloat(size);
            b.writeInt(color);
            b.writeVarInt(life);
        });
    }

    /** Gravity column pressing down on an area. */
    public static void press(ServerWorld w, Vec3d center, float radius, int color, int life) {
        broadcast(w, center, PRESS, b -> {
            vec(b, center);
            b.writeFloat(radius);
            b.writeInt(color);
            b.writeVarInt(life);
        });
    }

    /** Full-screen anime speed lines, only for the player who used the move. */
    public static void speedLines(ServerPlayerEntity player, float intensity, int duration) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(SPEEDLINES);
        buf.writeFloat(intensity);
        buf.writeVarInt(duration);
        ServerPlayNetworking.send(player, Net.VFX, buf);
    }

    /** Flying sword slash: a glowing crescent travelling along dir. roll tilts the crescent (degrees). */
    public static void slash(ServerWorld w, Vec3d start, Vec3d dir, float speed, int life, float width, float roll, int color) {
        broadcast(w, start, SLASH, b -> {
            vec(b, start);
            vec(b, dir);
            b.writeFloat(speed);
            b.writeVarInt(life);
            b.writeFloat(width);
            b.writeFloat(roll);
            b.writeInt(color);
        });
    }

    public static Vec3d rgb(int color) {
        return new Vec3d(((color >> 16) & 0xFF) / 255.0, ((color >> 8) & 0xFF) / 255.0, (color & 0xFF) / 255.0);
    }
}
