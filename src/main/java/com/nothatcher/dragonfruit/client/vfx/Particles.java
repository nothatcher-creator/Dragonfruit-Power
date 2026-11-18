package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/** One-shot particle explosions ("bursts") and helpers. Uses the particle manager directly so nothing is culled. */
public final class Particles {
    private Particles() {}

    public static void add(ParticleEffect effect, Vec3d pos, Vec3d vel) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.particleManager == null) return;
        mc.particleManager.addParticle(effect, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
    }

    static Vec3d randDir(Random r) {
        Vec3d v = new Vec3d(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
        return v.lengthSquared() < 1.0E-6 ? Gfx.UP : v.normalize();
    }

    /** Finds the top solid block at or below pos (within depth). Returns null if none. */
    static BlockPos ground(ClientWorld w, Vec3d pos, int up, int depth) {
        BlockPos.Mutable m = BlockPos.ofFloored(pos).up(up).mutableCopy();
        for (int i = 0; i < up + depth; i++) {
            BlockState s = w.getBlockState(m);
            if (!s.isAir() && !s.getCollisionShape(w, m).isEmpty() && w.getBlockState(m.up()).getCollisionShape(w, m.up()).isEmpty()) {
                return m.toImmutable();
            }
            m.move(0, -1, 0);
        }
        return null;
    }

    static void blockDust(ClientWorld w, Vec3d at, int count, double upSpeed, Random r) {
        BlockPos g = ground(w, at, 2, 6);
        if (g == null) return;
        BlockState state = w.getBlockState(g);
        BlockStateParticleEffect fx = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
        Vec3d top = new Vec3d(at.x, g.getY() + 1.05, at.z);
        for (int i = 0; i < count; i++) {
            add(fx, top.add((r.nextDouble() - 0.5) * 0.8, 0, (r.nextDouble() - 0.5) * 0.8),
                    new Vec3d((r.nextDouble() - 0.5) * 0.3, upSpeed * (0.5 + r.nextDouble()), (r.nextDouble() - 0.5) * 0.3));
        }
    }

    public static void burst(ClientWorld w, Vec3d pos, Vec3d dir, int kind, float s) {
        Random r = w.random;
        switch (kind) {
            case Vfx.B_RUBBER_HIT -> {
                for (int i = 0; i < 14 * s; i++) add(ParticleTypes.POOF, pos, randDir(r).multiply(0.25 * s));
                for (int i = 0; i < 18 * s; i++) add(ParticleTypes.CRIT, pos, randDir(r).multiply(0.6));
                add(ParticleTypes.SWEEP_ATTACK, pos, Vec3d.ZERO);
                shockRing(pos, dir, ParticleTypes.CLOUD, 18, 0.35 * s);
            }
            case Vfx.B_BLOCK_HIT -> {
                BlockPos bp = BlockPos.ofFloored(pos.add(dir.multiply(0.2)));
                BlockState st = w.getBlockState(bp);
                if (!st.isAir()) {
                    BlockStateParticleEffect fx = new BlockStateParticleEffect(ParticleTypes.BLOCK, st);
                    for (int i = 0; i < 28 * s; i++) add(fx, pos, randDir(r).multiply(0.3).subtract(dir.multiply(0.2)));
                }
                for (int i = 0; i < 6; i++) add(ParticleTypes.POOF, pos, randDir(r).multiply(0.1));
            }
            case Vfx.B_FIRE_BIG -> {
                for (int i = 0; i < 90 * s; i++) add(ParticleTypes.FLAME, pos, randDir(r).multiply(0.12 + r.nextDouble() * 0.25 * s));
                for (int i = 0; i < 14 * s; i++) add(ParticleTypes.LAVA, pos, Vec3d.ZERO);
                for (int i = 0; i < 20 * s; i++) add(ParticleTypes.LARGE_SMOKE, pos, randDir(r).multiply(0.1).add(0, 0.05, 0));
                add(s >= 1.5f ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, pos, Vec3d.ZERO);
                add(ParticleTypes.FLASH, pos, Vec3d.ZERO);
            }
            case Vfx.B_FIRE_SMALL -> {
                for (int i = 0; i < 26; i++) add(ParticleTypes.FLAME, pos, randDir(r).multiply(0.12));
                for (int i = 0; i < 8; i++) add(ParticleTypes.SMOKE, pos, randDir(r).multiply(0.05));
                for (int i = 0; i < 3; i++) add(ParticleTypes.LAVA, pos, Vec3d.ZERO);
            }
            case Vfx.B_FLAME_PILLAR -> {
                int columns = 28;
                for (int c = 0; c < columns; c++) {
                    double a = Math.PI * 2 * c / columns + r.nextDouble() * 0.2;
                    double rad = s * (0.55 + r.nextDouble() * 0.45);
                    Vec3d base = pos.add(Math.cos(a) * rad, 0.1, Math.sin(a) * rad);
                    for (int i = 0; i < 14; i++) {
                        add(ParticleTypes.FLAME, base.add((r.nextDouble() - 0.5) * 0.5, 0, (r.nextDouble() - 0.5) * 0.5),
                                new Vec3d(0, 0.35 + r.nextDouble() * 0.55, 0));
                    }
                    if (r.nextInt(3) == 0) add(ParticleTypes.LAVA, base, Vec3d.ZERO);
                    add(ParticleTypes.LARGE_SMOKE, base.add(0, 1, 0), new Vec3d(0, 0.2, 0));
                }
                for (int i = 0; i < 50; i++) {
                    add(ParticleTypes.FLAME, pos.add((r.nextDouble() - 0.5) * 0.8, 0.2, (r.nextDouble() - 0.5) * 0.8),
                            new Vec3d(0, 0.6 + r.nextDouble() * 0.6, 0));
                }
            }
            case Vfx.B_FROST -> {
                for (int i = 0; i < 90; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double sp = 0.3 + r.nextDouble() * 0.5 * (s / 10.0);
                    add(ParticleTypes.SNOWFLAKE, pos, new Vec3d(Math.cos(a) * sp, 0.05 + r.nextDouble() * 0.15, Math.sin(a) * sp));
                }
                for (int i = 0; i < 30; i++) add(ParticleTypes.ITEM_SNOWBALL, pos, randDir(r).multiply(0.4));
                for (int i = 0; i < 24; i++) add(ParticleTypes.CLOUD, pos, randDir(r).multiply(0.3).multiply(1, 0.2, 1));
            }
            case Vfx.B_ICE_SHATTER -> {
                BlockStateParticleEffect ice = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState());
                BlockStateParticleEffect packed = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState());
                for (int i = 0; i < 40 * s; i++) add(i % 2 == 0 ? ice : packed, pos.add(randDir(r).multiply(0.4 * s)), randDir(r).multiply(0.35));
                for (int i = 0; i < 20 * s; i++) add(ParticleTypes.SNOWFLAKE, pos, randDir(r).multiply(0.2));
                for (int i = 0; i < 8 * s; i++) add(ParticleTypes.ITEM_SNOWBALL, pos, randDir(r).multiply(0.3));
            }
            case Vfx.B_LIGHT -> {
                for (int i = 0; i < 60 * s; i++) add(ParticleTypes.END_ROD, pos, randDir(r).multiply(0.1 + r.nextDouble() * 0.3 * s));
                for (int i = 0; i < 25 * s; i++) add(ParticleTypes.FIREWORK, pos, randDir(r).multiply(0.3));
                for (int i = 0; i < 20 * s; i++) add(ParticleTypes.ELECTRIC_SPARK, pos, randDir(r).multiply(0.6));
                add(ParticleTypes.FLASH, pos, Vec3d.ZERO);
            }
            case Vfx.B_LIGHT_SMALL -> {
                for (int i = 0; i < 6; i++) add(ParticleTypes.END_ROD, pos, randDir(r).multiply(0.12));
                for (int i = 0; i < 4; i++) add(ParticleTypes.ELECTRIC_SPARK, pos, randDir(r).multiply(0.4));
            }
            case Vfx.B_QUAKE_LINE -> {
                Vec3d d = dir.lengthSquared() < 1.0E-6 ? Gfx.UP : dir.normalize();
                for (double l = 0; l < s; l += 0.8) {
                    Vec3d p = pos.add(d.multiply(l));
                    if (((int) (l / 0.8)) % 4 == 0) add(ParticleTypes.SONIC_BOOM, p, Vec3d.ZERO);
                    for (int i = 0; i < 3; i++) add(ParticleTypes.CLOUD, p, randDir(r).multiply(0.15).add(d.multiply(0.4)));
                    blockDust(w, p, 4, 0.5, r);
                }
                for (int i = 0; i < 3; i++) add(ParticleTypes.EXPLOSION, pos.add(randDir(r).multiply(0.6)), Vec3d.ZERO);
            }
            case Vfx.B_GROUND_QUAKE -> {
                for (int i = 0; i < 70; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rad = Math.sqrt(r.nextDouble()) * s;
                    blockDust(w, pos.add(Math.cos(a) * rad, 0.5, Math.sin(a) * rad), 5, 0.55, r);
                }
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI * 2 * i / 10;
                    add(ParticleTypes.SONIC_BOOM, pos.add(Math.cos(a) * s * 0.5, 0.6, Math.sin(a) * s * 0.5), Vec3d.ZERO);
                }
                for (int i = 0; i < 5; i++) add(ParticleTypes.EXPLOSION, pos.add(randDir(r).multiply(1.2)).add(0, 0.5, 0), Vec3d.ZERO);
                shockRing(pos.add(0, 0.3, 0), Gfx.UP, ParticleTypes.CLOUD, 40, 0.8);
            }
            case Vfx.B_STEAM -> {
                for (int i = 0; i < 45 * s; i++) add(ParticleTypes.CLOUD, pos.add(randDir(r).multiply(0.6)), randDir(r).multiply(0.15).add(0, 0.12, 0));
                for (int i = 0; i < 20 * s; i++) add(ParticleTypes.POOF, pos, randDir(r).multiply(0.3));
                DustParticleEffect pink = new DustParticleEffect(new Vector3f(1f, 0.45f, 0.55f), 1.4f);
                for (int i = 0; i < 20; i++) add(pink, pos.add(randDir(r).multiply(0.8)), Vec3d.ZERO);
            }
            case Vfx.B_AWAKEN -> {
                DustParticleEffect col = new DustParticleEffect(new Vector3f((float) dir.x, (float) dir.y, (float) dir.z), 1.6f);
                for (int i = 0; i < 120; i++) {
                    double a = i * 0.35;
                    double rad = 1.4 - i / 120.0;
                    Vec3d p = pos.add(Math.cos(a) * rad, -1 + i / 40.0, Math.sin(a) * rad);
                    add(col, p, Vec3d.ZERO);
                    if (i % 2 == 0) add(ParticleTypes.TOTEM_OF_UNDYING, p, new Vec3d(Math.cos(a) * 0.3, 0.25, Math.sin(a) * 0.3));
                }
                for (int i = 0; i < 25; i++) add(ParticleTypes.END_ROD, pos, randDir(r).multiply(0.25));
                add(ParticleTypes.FLASH, pos, Vec3d.ZERO);
            }
            case Vfx.B_THUNDER -> {
                for (int i = 0; i < 40 * s; i++) add(ParticleTypes.ELECTRIC_SPARK, pos, randDir(r).multiply(0.4 + r.nextDouble() * 0.6));
                for (int i = 0; i < 12 * s; i++) add(ParticleTypes.END_ROD, pos, randDir(r).multiply(0.15));
                for (int i = 0; i < 8 * s; i++) add(ParticleTypes.CLOUD, pos, randDir(r).multiply(0.2).multiply(1, 0.3, 1));
                add(ParticleTypes.FLASH, pos, Vec3d.ZERO);
                blockDust(w, pos, (int) (10 * s), 0.5, r);
            }
            case Vfx.B_MAGMA -> {
                for (int i = 0; i < 60 * s; i++) add(ParticleTypes.FLAME, pos, randDir(r).multiply(0.1 + r.nextDouble() * 0.25 * s));
                for (int i = 0; i < 20 * s; i++) add(ParticleTypes.LAVA, pos, Vec3d.ZERO);
                for (int i = 0; i < 25 * s; i++) add(ParticleTypes.LARGE_SMOKE, pos.add(randDir(r).multiply(s)), new Vec3d(0, 0.08, 0));
                for (int i = 0; i < 12 * s; i++) add(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.add(randDir(r).multiply(s)), new Vec3d(0, 0.05, 0));
                add(ParticleTypes.EXPLOSION, pos, Vec3d.ZERO);
                blockDust(w, pos, (int) (16 * s), 0.6, r);
            }
            case Vfx.B_ERUPTION -> {
                int columns = (int) (6 * s);
                for (int c = 0; c < columns; c++) {
                    double a = Math.PI * 2 * c / columns + r.nextDouble() * 0.2;
                    Vec3d base = pos.add(Math.cos(a) * s, 0.1, Math.sin(a) * s);
                    BlockPos g = ground(w, base, 2, 4);
                    if (g != null) base = new Vec3d(base.x, g.getY() + 1.02, base.z);
                    for (int i = 0; i < 10; i++) {
                        add(ParticleTypes.FLAME, base.add((r.nextDouble() - 0.5) * 0.6, 0, (r.nextDouble() - 0.5) * 0.6),
                                new Vec3d(0, 0.45 + r.nextDouble() * 0.6, 0));
                    }
                    for (int i = 0; i < 3; i++) add(ParticleTypes.LAVA, base, Vec3d.ZERO);
                    add(ParticleTypes.LARGE_SMOKE, base.add(0, 1.5, 0), new Vec3d(0, 0.15, 0));
                    blockDust(w, base, 3, 0.6, r);
                }
            }
            case Vfx.B_GRAVITY -> {
                for (int i = 0; i < 50; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rad = Math.sqrt(r.nextDouble()) * s;
                    add(ParticleTypes.REVERSE_PORTAL, pos.add(Math.cos(a) * rad, 0.3, Math.sin(a) * rad), new Vec3d(0, -0.2, 0));
                }
                for (int i = 0; i < 20; i++) add(ParticleTypes.PORTAL, pos.add(0, 0.5, 0), randDir(r).multiply(0.8));
                shockRing(pos.add(0, 0.3, 0), Gfx.UP, ParticleTypes.CLOUD, 30, 0.6);
            }
            default -> { }
        }
    }

    static void shockRing(Vec3d c, Vec3d normal, ParticleEffect fx, int count, double speed) {
        Vec3d[] b = Gfx.basis(normal.lengthSquared() < 1.0E-6 ? Gfx.UP : normal);
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Vec3d d = b[0].multiply(Math.cos(a)).add(b[1].multiply(Math.sin(a)));
            add(fx, c, d.multiply(speed));
        }
    }

    /** Ring-edge particles used by RingEffect styles. */
    static void style(ClientWorld w, int style, Vec3d at, Random r) {
        switch (style) {
            case Vfx.S_DUST -> blockDust(w, at, 2, 0.35, r);
            case Vfx.S_FLAME -> add(ParticleTypes.FLAME, at, new Vec3d(0, 0.08, 0));
            case Vfx.S_FROST -> add(ParticleTypes.SNOWFLAKE, at, new Vec3d(0, 0.05, 0));
            case Vfx.S_LIGHT -> add(ParticleTypes.END_ROD, at, Vec3d.ZERO);
            case Vfx.S_QUAKE -> add(ParticleTypes.CLOUD, at, Vec3d.ZERO);
            case Vfx.S_THUNDER -> add(ParticleTypes.ELECTRIC_SPARK, at, new Vec3d(0, 0.1, 0));
            case Vfx.S_GRAVITY -> add(ParticleTypes.REVERSE_PORTAL, at, Vec3d.ZERO);
            default -> { }
        }
    }
}
