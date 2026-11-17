package com.nothatcher.dragonfruit.power;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * All ability logic. Every ability returns true if it fired (cooldown starts) or false if it fizzled.
 * Damage is timed with Scheduler so hits land when the visual reaches the target.
 */
public final class Abilities {
    private Abilities() {}

    public static boolean run(ServerPlayerEntity p, FruitType fruit, int index) {
        return switch (fruit) {
            case RUBBER -> switch (index) {
                case 0 -> rubberPistol(p);
                case 1 -> rubberRocket(p);
                case 2 -> rubberGatling(p);
                case 3 -> secondGear(p);
                default -> false;
            };
            case FLAME -> switch (index) {
                case 0 -> fireFist(p);
                case 1 -> flamePillar(p);
                case 2 -> fireflySwarm(p);
                case 3 -> flameBody(p);
                default -> false;
            };
            case ICE -> switch (index) {
                case 0 -> iceSpears(p);
                case 1 -> iceAge(p);
                case 2 -> iceCoffin(p);
                default -> false;
            };
            case LIGHT -> switch (index) {
                case 0 -> lightFlash(p);
                case 1 -> lightBarrage(p);
                case 2 -> lightCannon(p);
                default -> false;
            };
            case QUAKE -> switch (index) {
                case 0 -> quakePunch(p);
                case 1 -> groundQuake(p);
                case 2 -> quakeBubble(p);
                default -> false;
            };
            default -> false;
        };
    }

    // =================================================================== RUBBER

    private static boolean rubberPistol(ServerPlayerEntity p) {
        boolean gear = PowerManager.buffActive(p);
        double range = gear ? 34 : 26;
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        BlockHitResult bh = rayBlock(p, eye, look, range);
        double blockDist = bh.getType() == HitResult.Type.MISS ? range : bh.getPos().distanceTo(eye);
        EntityHitResult eh = rayEntity(p, eye, look, blockDist);
        Vec3d target = eh != null ? eh.getPos() : eye.add(look.multiply(blockDist));
        double dist = target.distanceTo(eye);
        int extend = Math.max(2, (int) Math.ceil(dist / (gear ? 9.0 : 5.0)));

        Vfx.arm(p, 1, target, extend, gear ? 1 : 2, extend + 3, gear ? 1.15f : 1f);
        Vfx.fov(p, gear ? 0.2f : 0.12f, extend + 4);
        sound(p, SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1f, 0.55f);
        sound(p, SoundEvents.ENTITY_SLIME_SQUISH, 0.8f, 1.5f);
        if (gear) sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.6f);

        final Entity hit = eh != null ? eh.getEntity() : null;
        final boolean hitBlock = eh == null && bh.getType() != HitResult.Type.MISS;
        Scheduler.after(extend, () -> {
            if (!valid(p)) return;
            ServerWorld w = world(p);
            if (hit instanceof LivingEntity le && le.isAlive()) {
                hurt(p, le, gear ? 14f : 9f, true);
                push(le, look.multiply(gear ? 2.2 : 1.5).add(0, 0.35, 0));
                Vfx.burst(w, target, look, Vfx.B_RUBBER_HIT, gear ? 1.6f : 1f);
                Vfx.shake(w, target, gear ? 0.5f : 0.3f, 8, 16);
                sound(w, target, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.3f, 0.8f);
                sound(w, target, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.7f);
            } else if (hitBlock) {
                Vfx.burst(w, target, look, Vfx.B_BLOCK_HIT, 1f);
                Vfx.shake(w, target, 0.15f, 5, 10);
                sound(w, target, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.6f);
            }
        });
        return true;
    }

    private static boolean rubberRocket(ServerPlayerEntity p) {
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        BlockHitResult bh = rayBlock(p, eye, look, 40);
        if (bh.getType() == HitResult.Type.MISS) {
            p.sendMessage(Text.translatable("msg.dragonfruit.nothing_to_grab").formatted(Formatting.GRAY), true);
            return false;
        }
        Vec3d target = bh.getPos();
        double dist = target.distanceTo(eye);
        int extend = Math.max(3, (int) Math.ceil(dist / 6.0));
        Vec3d right = rightOf(look);

        // Both arms shoot out and grab, then you slingshot toward the grab point.
        Vfx.arm(p, 1, target.add(right.multiply(0.45)), extend, 7, 4, 1f);
        Vfx.arm(p, -1, target.subtract(right.multiply(0.45)), extend, 7, 4, 1f);
        Vfx.fov(p, 0.1f, extend);
        sound(p, SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1f, 0.5f);
        sound(p, SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1f, 0.7f);

        Scheduler.after(extend, () -> {
            if (!valid(p)) return;
            Vfx.burst(world(p), target, look, Vfx.B_BLOCK_HIT, 0.8f);
            sound(world(p), target, SoundEvents.ENTITY_SLIME_SQUISH, 1f, 0.6f);
        });
        Scheduler.after(extend + 3, () -> {
            if (!valid(p)) return;
            Vec3d dir = target.subtract(p.getPos().add(0, 1.0, 0)).normalize();
            double speed = Math.min(3.4, 1.3 + dist * 0.07);
            launch(p, dir.multiply(speed).add(0, 0.25, 0));
            Vfx.fov(p, 0.4f, 16);
            Vfx.burst(world(p), p.getPos().add(0, 1, 0), dir, Vfx.B_RUBBER_HIT, 0.8f);
            sound(p, SoundEvents.ENTITY_SLIME_JUMP, 1.2f, 0.6f);
            sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.6f);
        });
        return true;
    }

    private static boolean rubberGatling(ServerPlayerEntity p) {
        final boolean gear = PowerManager.buffActive(p);
        final int duration = 44;
        Vfx.gatling(p, duration, p.getRandom().nextLong(), gear ? 11f : 9f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1, false, false, false));
        sound(p, SoundEvents.ENTITY_SLIME_SQUISH, 1f, 0.5f);
        Scheduler.loop(0, age -> {
            if (!valid(p) || age >= duration) return false;
            Vec3d look = p.getRotationVec(1f);
            if (age % 2 == 0) {
                for (LivingEntity e : cone(p, gear ? 11 : 9.5, 0.75)) {
                    hurt(p, e, gear ? 4f : 2.5f, true);
                    push(e, look.multiply(0.18).add(0, 0.06, 0));
                }
            }
            Random r = p.getRandom();
            if (age % 3 == 0) sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 0.8f + r.nextFloat() * 0.6f);
            if (age % 5 == 0) sound(p, SoundEvents.ENTITY_FISHING_BOBBER_THROW, 0.5f, 1.1f + r.nextFloat() * 0.5f);
            return true;
        });
        return true;
    }

    private static boolean secondGear(ServerPlayerEntity p) {
        final int duration = 400;
        ServerWorld w = world(p);
        PowerManager.setBuff(p, duration);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 1, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 1, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, 1, false, false, true));
        Vfx.aura(w, p, Vfx.A_STEAM, duration);
        Vfx.burst(w, p.getPos().add(0, 1, 0), Vec3d.ZERO, Vfx.B_STEAM, 1.5f);
        Vfx.shake(w, p.getPos(), 0.35f, 15, 12);
        Vfx.fov(p, 0.18f, 20);
        sound(p, SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.2f, 0.6f);
        sound(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 2f, 1.4f);
        Scheduler.loop(16, age -> {
            if (!valid(p) || !PowerManager.buffActive(p)) return false;
            if (age % 16 == 0) sound(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.2f, 1.5f);
            if (age % 50 == 0) sound(p, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.4f, 0.8f);
            return true;
        });
        return true;
    }

    // ==================================================================== FLAME

    private static boolean fireFist(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d look = p.getRotationVec(1f);
        Vec3d start = p.getEyePos().add(look.multiply(1.2)).add(0, -0.2, 0);
        float speed = 1.6f;
        int life = 32;
        float size = 1.4f;
        Vfx.projectile(w, p.getId(), Vfx.P_FIRE_FIST, start, look, speed, life, size);
        Vfx.fov(p, 0.15f, 8);
        Vfx.shake(w, p.getPos(), 0.25f, 6, 6);
        sound(p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
        sound(p, SoundEvents.ITEM_FIRECHARGE_USE, 1.2f, 0.7f);
        simulate(p, w, start, look, speed, life, size, hit -> {
            Vfx.burst(w, hit, look, Vfx.B_FIRE_BIG, 1.6f);
            Vfx.ring(w, hit, Vfx.UP, 6f, 0xFF7A1A, 14, Vfx.S_FLAME);
            Vfx.orb(w, hit, 2.5f, 0xFF8A2A, 10, -1, Vfx.ATTACH_NONE, false, Vfx.S_FLAME);
            Vfx.shake(w, hit, 0.8f, 14, 24);
            Vfx.flash(w, hit, 0.3f, 20);
            sound(w, hit, SoundEvents.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
            for (LivingEntity e : inRadius(w, hit, 4.5, p)) {
                hurt(p, e, 12f, true);
                e.setOnFireFor(8);
                push(e, e.getPos().subtract(hit).normalize().multiply(1.2).add(0, 0.5, 0));
            }
            if (griefing(w)) placeFire(w, hit, 2);
        });
        return true;
    }

    private static boolean flamePillar(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d c = p.getPos();
        float radius = 7f;
        Vfx.burst(w, c, Vfx.UP, Vfx.B_FLAME_PILLAR, radius);
        Vfx.ring(w, c.add(0, 0.1, 0), Vfx.UP, radius + 0.5f, 0xFF6A00, 16, Vfx.S_FLAME);
        Vfx.orb(w, c.add(0, 1, 0), 2.2f, 0xFF8A2A, 12, -1, Vfx.ATTACH_NONE, false, Vfx.S_FLAME);
        Vfx.shake(w, c, 0.6f, 16, 20);
        Vfx.flash(w, c, 0.2f, 14);
        sound(p, SoundEvents.ENTITY_BLAZE_SHOOT, 2f, 0.5f);
        sound(p, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
        sound(p, SoundEvents.BLOCK_FIRE_AMBIENT, 2f, 0.6f);
        for (LivingEntity e : inRadius(w, c, radius, p)) {
            hurt(p, e, 10f, true);
            e.setOnFireFor(6);
            Vec3d out = e.getPos().subtract(c).multiply(1, 0, 1).normalize();
            push(e, out.multiply(0.8).add(0, 1.0, 0));
        }
        return true;
    }

    private static boolean fireflySwarm(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        List<LivingEntity> targets = inRadius(w, p.getPos(), 22, p);
        targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
        if (targets.size() > 10) targets = new ArrayList<>(targets.subList(0, 10));
        int[] ids = targets.stream().mapToInt(Entity::getId).toArray();
        final int orbit = 18, fly = 10;
        Vfx.fireflies(p, ids, orbit, fly);
        sound(p, SoundEvents.ENTITY_BLAZE_AMBIENT, 1f, 1.6f);
        final List<LivingEntity> finalTargets = targets;
        Scheduler.after(orbit, () -> {
            if (valid(p)) sound(p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.2f, 1.3f);
        });
        Scheduler.after(orbit + fly, () -> {
            for (LivingEntity e : finalTargets) {
                if (!e.isAlive()) continue;
                Vec3d at = e.getBoundingBox().getCenter();
                hurt(p, e, 7f, true);
                e.setOnFireFor(5);
                Vfx.burst(w, at, Vec3d.ZERO, Vfx.B_FIRE_SMALL, 1f);
                sound(w, at, SoundEvents.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);
            }
        });
        return true;
    }

    private static boolean flameBody(ServerPlayerEntity p) {
        final int duration = 240;
        ServerWorld w = world(p);
        PowerManager.setBuff(p, duration);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, false, false, true));
        Vfx.aura(w, p, Vfx.A_FLAME, duration);
        Vfx.burst(w, p.getPos().add(0, 1, 0), Vec3d.ZERO, Vfx.B_FIRE_BIG, 0.8f);
        Vfx.fov(p, 0.12f, 12);
        sound(p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
        sound(p, SoundEvents.BLOCK_FIRE_AMBIENT, 2f, 0.8f);
        return true;
    }

    // ====================================================================== ICE

    private static boolean iceSpears(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d look = p.getRotationVec(1f);
        Vec3d eye = p.getEyePos();
        sound(p, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 1f, 1.2f);
        sound(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.8f);
        for (int k = -3; k <= 3; k++) {
            float yaw = (float) Math.toRadians(k * 6.0);
            Vec3d dir = look.rotateY(yaw).add(0, (p.getRandom().nextFloat() - 0.5f) * 0.05, 0).normalize();
            Vec3d start = eye.add(dir.multiply(1.0)).add(0, -0.15, 0);
            int delay = Math.abs(k);
            Scheduler.after(delay, () -> {
                if (!valid(p)) return;
                Vfx.projectile(w, p.getId(), Vfx.P_ICE_SPEAR, start, dir, 2.2f, 18, 0.6f);
                simulate(p, w, start, dir, 2.2f, 18, 0.6f, hit -> {
                    Vfx.burst(w, hit, dir, Vfx.B_ICE_SHATTER, 0.7f);
                    sound(w, hit, SoundEvents.BLOCK_GLASS_BREAK, 0.6f, 1.6f);
                    for (LivingEntity e : inRadius(w, hit, 1.6, p)) {
                        hurt(p, e, 6f, true);
                        freeze(e, 220);
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                    }
                });
            });
        }
        return true;
    }

    private static boolean iceAge(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d c = p.getPos();
        int r = 12;
        BlockPos base = p.getBlockPos();
        Random rand = p.getRandom();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int dy = -4; dy <= 2; dy++) {
                    BlockPos pos = base.add(dx, dy, dz);
                    BlockState state = w.getBlockState(pos);
                    if (state.isOf(Blocks.WATER) && state.getFluidState().isStill() && w.isAir(pos.up())) {
                        w.setBlockState(pos, Blocks.FROSTED_ICE.getDefaultState());
                        w.scheduleBlockTick(pos, Blocks.FROSTED_ICE, MathHelper.nextInt(rand, 200, 400));
                    }
                }
            }
        }
        for (LivingEntity e : inRadius(w, c, r, p)) {
            hurt(p, e, 7f, true);
            freeze(e, 340);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 3));
        }
        Vfx.spikes(w, c, r, 46, 70, rand.nextLong());
        Vfx.ring(w, c.add(0, 0.12, 0), Vfx.UP, r + 1, 0xBFF4FF, 18, Vfx.S_FROST);
        Vfx.burst(w, c.add(0, 0.5, 0), Vfx.UP, Vfx.B_FROST, r);
        Vfx.shake(w, c, 0.5f, 18, 24);
        Vfx.flash(w, c, 0.15f, 14);
        sound(p, SoundEvents.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
        sound(p, SoundEvents.BLOCK_POWDER_SNOW_BREAK, 2f, 0.6f);
        sound(p, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.6f);
        return true;
    }

    private static boolean iceCoffin(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        BlockHitResult bh = rayBlock(p, eye, look, 28);
        double max = bh.getType() == HitResult.Type.MISS ? 28 : bh.getPos().distanceTo(eye);
        EntityHitResult eh = rayEntity(p, eye, look, max);
        if (eh == null || !(eh.getEntity() instanceof LivingEntity target)) {
            p.sendMessage(Text.translatable("msg.dragonfruit.no_target").formatted(Formatting.GRAY), true);
            return false;
        }
        final int hold = 60;
        Vfx.iceCoffin(w, target, hold);
        Vfx.beam(w, eye.add(look).add(0, -0.2, 0), target.getBoundingBox().getCenter(), 0x9FE8FF, 0.3f, 8, Vfx.S_FROST);
        sound(p, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.8f);
        sound(w, target.getPos(), SoundEvents.BLOCK_GLASS_PLACE, 1.5f, 0.6f);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, hold, 9));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, hold, 4));
        if (target instanceof MobEntity mob) mob.getNavigation().stop();
        final Vec3d lockPos = target.getPos();
        Scheduler.loop(0, age -> {
            if (!target.isAlive()) return false;
            if (age >= hold) {
                Vec3d at = target.getBoundingBox().getCenter();
                hurt(p, target, 16f, true);
                Vfx.burst(w, at, Vec3d.ZERO, Vfx.B_ICE_SHATTER, 2f);
                Vfx.shake(w, at, 0.3f, 8, 14);
                sound(w, at, SoundEvents.BLOCK_GLASS_BREAK, 2f, 0.7f);
                return false;
            }
            freeze(target, 300);
            target.setVelocity(Vec3d.ZERO);
            if (target instanceof MobEntity mob) {
                mob.getNavigation().stop();
                mob.refreshPositionAndAngles(lockPos.x, lockPos.y, lockPos.z, mob.getYaw(), mob.getPitch());
            }
            target.velocityModified = true;
            return true;
        });
        return true;
    }

    // ==================================================================== LIGHT

    private static boolean lightFlash(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        BlockHitResult bh = rayBlock(p, eye, look, 42);
        Vec3d destEye = bh.getType() == HitResult.Type.MISS ? eye.add(look.multiply(42)) : bh.getPos().subtract(look.multiply(0.8));
        double eyeH = p.getStandingEyeHeight();
        Vec3d feet = destEye.subtract(0, eyeH, 0);

        Vec3d chosen = null;
        outer:
        for (double back = 0; back < 12; back += 1.0) {
            for (int up = 0; up <= 4; up++) {
                Vec3d cand = feet.subtract(look.multiply(back)).add(0, up * 0.5, 0);
                Box moved = p.getBoundingBox().offset(cand.subtract(p.getPos()));
                if (w.isSpaceEmpty(p, moved)) {
                    chosen = cand;
                    break outer;
                }
            }
        }
        if (chosen == null) {
            p.sendMessage(Text.translatable("msg.dragonfruit.blocked").formatted(Formatting.GRAY), true);
            return false;
        }
        Vec3d a = eye;
        Vec3d b = chosen.add(0, eyeH, 0);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(a, b).expand(1.5),
                e -> e != p && e.isAlive() && !e.isSpectator())) {
            if (e.getBoundingBox().expand(0.8).raycast(a, b).isPresent()) {
                hurt(p, e, 9f, true);
                push(e, look.multiply(0.8).add(0, 0.4, 0));
                Vfx.burst(w, e.getBoundingBox().getCenter(), look, Vfx.B_LIGHT_SMALL, 1f);
            }
        }
        p.networkHandler.requestTeleport(chosen.x, chosen.y, chosen.z, p.getYaw(), p.getPitch());
        p.fallDistance = 0;

        Vfx.beam(w, a.add(0, -0.4, 0), b.add(0, -0.4, 0), 0xFFF3A0, 0.55f, 12, Vfx.S_LIGHT);
        Vfx.burst(w, a, look, Vfx.B_LIGHT, 1f);
        Vfx.burst(w, b, look, Vfx.B_LIGHT, 1f);
        Vfx.flash(w, b, 0.45f, 24);
        Vfx.fov(p, 0.3f, 10);
        sound(w, a, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.2f, 1.6f);
        sound(w, b, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.2f, 1.8f);
        sound(w, b, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.8f);
        return true;
    }

    private static boolean lightBarrage(ServerPlayerEntity p) {
        final int duration = 40;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1, false, false, false));
        sound(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 1.8f);
        Scheduler.loop(0, age -> {
            if (!valid(p) || age >= duration) return false;
            ServerWorld w = world(p);
            Vec3d eye = p.getEyePos();
            Vec3d look = p.getRotationVec(1f);
            Vec3d right = rightOf(look);
            Vec3d up = right.crossProduct(look);
            Random r = p.getRandom();
            for (int k = 0; k < 2; k++) {
                Vec3d origin = eye.add(right.multiply((r.nextFloat() - 0.5) * 2.6))
                        .add(up.multiply((r.nextFloat() - 0.3) * 1.4))
                        .subtract(look.multiply(0.3));
                Vec3d dir = look.add(r.nextGaussian() * 0.04, r.nextGaussian() * 0.04, r.nextGaussian() * 0.04).normalize();
                BlockHitResult bh = rayBlock(p, origin, dir, 44);
                double bd = bh.getType() == HitResult.Type.MISS ? 44 : bh.getPos().distanceTo(origin);
                EntityHitResult eh = rayEntity(p, origin, dir, bd);
                Vec3d hit = eh != null ? eh.getPos() : origin.add(dir.multiply(bd));
                if (eh != null && eh.getEntity() instanceof LivingEntity le) hurt(p, le, 2.5f, true);
                Vfx.beam(w, origin, hit, 0xFFE866, 0.14f, 4, Vfx.S_NONE);
                if (eh != null || bh.getType() != HitResult.Type.MISS) {
                    Vfx.burst(w, hit, dir, Vfx.B_LIGHT_SMALL, 0.6f);
                }
            }
            if (age % 2 == 0) sound(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.6f + r.nextFloat() * 0.4f);
            return true;
        });
        return true;
    }

    private static boolean lightCannon(ServerPlayerEntity p) {
        final int charge = 30;
        ServerWorld w = world(p);
        Vfx.orb(w, p.getEyePos(), 0.9f, 0xFFEB7A, charge, p.getId(), Vfx.ATTACH_HAND, true, Vfx.S_LIGHT);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, charge, 3, false, false, false));
        sound(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.5f, 1.6f);
        sound(p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1.6f);
        Scheduler.after(charge, () -> {
            if (!valid(p)) return;
            ServerWorld ww = world(p);
            Vec3d eye = p.getEyePos();
            Vec3d look = p.getRotationVec(1f);
            Vec3d start = eye.add(look.multiply(1.2)).add(0, -0.15, 0);
            BlockHitResult bh = rayBlock(p, start, look, 64);
            Vec3d end = bh.getType() == HitResult.Type.MISS ? start.add(look.multiply(64)) : bh.getPos();
            for (LivingEntity e : ww.getEntitiesByClass(LivingEntity.class, new Box(start, end).expand(2),
                    e -> e != p && e.isAlive() && !e.isSpectator())) {
                if (e.getBoundingBox().expand(1.2).raycast(start, end).isPresent()) {
                    hurt(p, e, 22f, true);
                    e.setOnFireFor(4);
                    push(e, look.multiply(1.5).add(0, 0.3, 0));
                }
            }
            for (LivingEntity e : inRadius(ww, end, 4, p)) hurt(p, e, 10f, true);
            Vfx.beam(ww, start, end, 0xFFF6B0, 1.6f, 18, Vfx.S_LIGHT);
            Vfx.beam(ww, start, end, 0xFFFFFF, 0.6f, 18, Vfx.S_NONE);
            Vfx.burst(ww, end, look, Vfx.B_LIGHT, 2.5f);
            Vfx.ring(ww, end, look, 6f, 0xFFF0A0, 12, Vfx.S_LIGHT);
            Vfx.flash(ww, start, 0.7f, 40);
            Vfx.shake(ww, end, 0.9f, 16, 30);
            Vfx.shake(ww, start, 0.5f, 10, 6);
            Vfx.fov(p, 0.25f, 14);
            launch(p, p.getVelocity().add(look.multiply(-0.6)).add(0, 0.1, 0));
            sound(ww, start, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 2f, 1.3f);
            sound(ww, start, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.8f);
            sound(ww, end, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);
        });
        return true;
    }

    // ==================================================================== QUAKE

    private static boolean quakePunch(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        Vec3d fist = eye.add(look.multiply(1.3)).add(0, -0.1, 0);
        Vfx.arm(p, 1, fist.add(look.multiply(0.3)), 2, 6, 4, 1.1f);
        Vfx.airCrack(w, fist, look, 1.7f, 34, p.getRandom().nextLong());
        Vfx.ring(w, fist, look, 5f, 0xFFFFFF, 10, Vfx.S_QUAKE);
        Vfx.burst(w, fist, look, Vfx.B_QUAKE_LINE, 14f);
        Vfx.shake(w, fist, 1.0f, 22, 36);
        Vfx.fov(p, 0.2f, 10);
        for (LivingEntity e : cone(p, 15, 0.7)) {
            hurt(p, e, 14f, true);
            push(e, look.multiply(2.8).add(0, 0.55, 0));
        }
        sound(p, SoundEvents.ENTITY_GENERIC_EXPLODE, 2f, 0.55f);
        sound(p, SoundEvents.BLOCK_ANVIL_LAND, 1f, 0.5f);
        sound(p, SoundEvents.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
        return true;
    }

    private static boolean groundQuake(ServerPlayerEntity p) {
        ServerWorld w = world(p);
        Vec3d c = p.getPos();
        float r = 12f;
        Vfx.arm(p, 1, c.add(p.getRotationVec(1f).multiply(1, 0, 1).normalize().multiply(0.9)).add(0, 0.2, 0), 2, 6, 4, 1.1f);
        Vfx.ring(w, c.add(0, 0.15, 0), Vfx.UP, r + 1, 0xEDEDE0, 20, Vfx.S_DUST);
        Vfx.airCrack(w, c.add(0, 0.6, 0), Vfx.UP, 2.6f, 40, p.getRandom().nextLong());
        Vfx.burst(w, c, Vfx.UP, Vfx.B_GROUND_QUAKE, r);
        Vfx.shake(w, c, 1.6f, 30, 48);
        Vfx.fov(p, 0.15f, 14);
        for (LivingEntity e : inRadius(w, c, r, p)) {
            hurt(p, e, 10f, true);
            Vec3d out = e.getPos().subtract(c).multiply(1, 0, 1).normalize();
            push(e, out.multiply(0.6).add(0, 1.15, 0));
        }
        sound(p, SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5f, 0.45f);
        sound(p, SoundEvents.BLOCK_ANVIL_LAND, 1.2f, 0.4f);
        sound(p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
        return true;
    }

    private static boolean quakeBubble(ServerPlayerEntity p) {
        final int charge = 26;
        ServerWorld w = world(p);
        Vfx.orb(w, p.getEyePos(), 0.7f, 0xF4F4FF, charge, p.getId(), Vfx.ATTACH_HAND, true, Vfx.S_QUAKE);
        Vfx.shake(w, p.getPos(), 0.25f, charge, 10);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, charge, 3, false, false, false));
        sound(p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1f, 0.6f);
        Scheduler.after(charge, () -> {
            if (!valid(p)) return;
            ServerWorld ww = world(p);
            Vec3d eye = p.getEyePos();
            Vec3d look = p.getRotationVec(1f);
            Vec3d fist = eye.add(look.multiply(1.3)).add(0, -0.1, 0);
            Vfx.arm(p, 1, fist.add(look.multiply(0.4)), 2, 8, 5, 1.25f);
            Vfx.airCrack(ww, fist.add(look), look, 3.2f, 50, p.getRandom().nextLong());
            Vfx.ring(ww, fist, look, 9f, 0xFFFFFF, 14, Vfx.S_QUAKE);
            Vfx.burst(ww, fist, look, Vfx.B_QUAKE_LINE, 28f);
            Vfx.shake(ww, fist, 2.0f, 36, 64);
            Vfx.flash(ww, fist, 0.25f, 20);
            Vfx.fov(p, 0.3f, 16);
            for (LivingEntity e : cone(p, 28, 0.6)) {
                hurt(p, e, 20f, true);
                push(e, look.multiply(3.8).add(0, 0.7, 0));
            }
            sound(ww, fist, SoundEvents.ENTITY_GENERIC_EXPLODE, 3f, 0.4f);
            sound(ww, fist, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 2f, 0.6f);
            sound(ww, fist, SoundEvents.BLOCK_GLASS_BREAK, 1.5f, 0.4f);
        });
        return true;
    }

    // ================================================================== helpers

    static boolean valid(ServerPlayerEntity p) {
        return !p.isRemoved() && p.isAlive();
    }

    static ServerWorld world(ServerPlayerEntity p) {
        return (ServerWorld) p.getWorld();
    }

    static Vec3d rightOf(Vec3d look) {
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 1.0E-4) right = new Vec3d(1, 0, 0);
        return right.normalize();
    }

    static BlockHitResult rayBlock(ServerPlayerEntity p, Vec3d from, Vec3d dir, double dist) {
        return p.getWorld().raycast(new RaycastContext(from, from.add(dir.multiply(dist)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
    }

    static EntityHitResult rayEntity(ServerPlayerEntity p, Vec3d from, Vec3d dir, double dist) {
        Vec3d to = from.add(dir.multiply(dist));
        Box box = new Box(from, to).expand(1.0);
        return ProjectileUtil.raycast(p, from, to, box,
                e -> e != p && !e.isSpectator() && e.canHit() && e instanceof LivingEntity, dist * dist);
    }

    static List<LivingEntity> inRadius(ServerWorld w, Vec3d c, double r, ServerPlayerEntity exclude) {
        return w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(r),
                e -> e != exclude && e.isAlive() && !e.isSpectator() && e.squaredDistanceTo(c) <= r * r);
    }

    static List<LivingEntity> cone(ServerPlayerEntity p, double range, double minDot) {
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        return world(p).getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(range), e -> {
            if (e == p || !e.isAlive() || e.isSpectator()) return false;
            Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range) return false;
            return d < 1.5 || to.multiply(1.0 / d).dotProduct(look) >= minDot;
        });
    }

    static void hurt(ServerPlayerEntity p, LivingEntity target, float amount, boolean ignoreIFrames) {
        if (ignoreIFrames) target.timeUntilRegen = 0;
        target.damage(p.getDamageSources().playerAttack(p), amount);
    }

    static void push(Entity target, Vec3d velocity) {
        target.addVelocity(velocity.x, velocity.y, velocity.z);
        target.velocityModified = true;
    }

    static void launch(ServerPlayerEntity p, Vec3d velocity) {
        p.setVelocity(velocity);
        p.velocityModified = true;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
        p.fallDistance = 0;
    }

    static void freeze(LivingEntity e, int ticks) {
        e.setFrozenTicks(Math.max(e.getFrozenTicks(), ticks));
    }

    static void sound(ServerPlayerEntity p, SoundEvent s, float volume, float pitch) {
        p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundCategory.PLAYERS, volume, pitch);
    }

    static void sound(ServerWorld w, Vec3d at, SoundEvent s, float volume, float pitch) {
        w.playSound(null, at.x, at.y, at.z, s, SoundCategory.PLAYERS, volume, pitch);
    }

    static boolean griefing(ServerWorld w) {
        return w.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
    }

    static void placeFire(ServerWorld w, Vec3d center, int r) {
        BlockPos c = BlockPos.ofFloored(center);
        Random rand = w.getRandom();
        for (BlockPos pos : BlockPos.iterate(c.add(-r, -r, -r), c.add(r, r, r))) {
            if (rand.nextFloat() > 0.35f) continue;
            BlockPos below = pos.down();
            if (w.isAir(pos) && w.getBlockState(below).isSideSolidFullSquare(w, below, Direction.UP)) {
                w.setBlockState(pos, Blocks.FIRE.getDefaultState());
            }
        }
    }

    /** Server-side projectile that mirrors the client visual: straight line, hits first entity or block. */
    static void simulate(ServerPlayerEntity owner, ServerWorld w, Vec3d start, Vec3d dir, float speed, int life,
                         float radius, Consumer<Vec3d> onHit) {
        final Vec3d[] pos = {start};
        Scheduler.loop(0, age -> {
            Vec3d cur = pos[0];
            Vec3d next = cur.add(dir.multiply(speed));
            BlockHitResult bh = w.raycast(new RaycastContext(cur, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, owner));
            Vec3d end = bh.getType() == HitResult.Type.MISS ? next : bh.getPos();
            List<LivingEntity> hits = w.getEntitiesByClass(LivingEntity.class, new Box(cur, end).expand(radius),
                    e -> e != owner && e.isAlive() && !e.isSpectator());
            if (!hits.isEmpty()) {
                hits.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(cur)));
                onHit.accept(hits.get(0).getBoundingBox().getCenter());
                return false;
            }
            if (bh.getType() != HitResult.Type.MISS) {
                onHit.accept(bh.getPos());
                return false;
            }
            pos[0] = next;
            if (age >= life) {
                onHit.accept(next);
                return false;
            }
            return true;
        });
    }
}
