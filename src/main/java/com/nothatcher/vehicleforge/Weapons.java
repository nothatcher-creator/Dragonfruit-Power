package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleType.Category;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Horns, sirens, the fire hose and everything that goes boom. Server side. */
public final class Weapons {
    private static final Map<UUID, Integer> NEXT_FIRE = new HashMap<>();

    private Weapons() {}

    public static void action(VehicleEntity v, ServerPlayerEntity driver, int action) {
        VehicleType t = v.type;
        if (action == 1) {
            if (t.siren) {
                v.setSiren(!v.sirenOn());
                driver.sendMessage(Text.translatable(v.sirenOn() ? "msg.vehicleforge.siren_on" : "msg.vehicleforge.siren_off").formatted(Formatting.YELLOW), true);
            } else {
                horn(v);
            }
            return;
        }
        if (t.weapon == VehicleType.Weapon.NONE) {
            horn(v);
            return;
        }
        if (t.weapon == VehicleType.Weapon.WATER_CANNON) {
            v.setSpray(!v.sprayOn());
            driver.sendMessage(Text.translatable(v.sprayOn() ? "msg.vehicleforge.spray_on" : "msg.vehicleforge.spray_off").formatted(Formatting.AQUA), true);
            return;
        }
        Integer next = NEXT_FIRE.get(v.getUuid());
        if (next != null && v.age < next) {
            driver.sendMessage(Text.translatable("msg.vehicleforge.reloading").formatted(Formatting.GRAY), true);
            return;
        }
        ServerWorld w = (ServerWorld) v.getWorld();
        switch (t.weapon) {
            case TANK_CANNON -> {
                NEXT_FIRE.put(v.getUuid(), v.age + 40);
                tankShot(w, v, driver);
            }
            case ROCKET_SALVO -> {
                NEXT_FIRE.put(v.getUuid(), v.age + 200);
                salvo(w, v, driver);
            }
            case MACHINE_GUN -> {
                NEXT_FIRE.put(v.getUuid(), v.age + 14);
                machineGun(w, v, driver);
            }
            case AIR_ROCKETS -> {
                NEXT_FIRE.put(v.getUuid(), v.age + 30);
                airRockets(w, v, driver);
            }
            default -> { }
        }
    }

    private static void horn(VehicleEntity v) {
        String s = switch (v.type.horn) {
            case CAR -> "horn_car";
            case TRUCK -> "horn_truck";
            case BIKE_BELL -> "bike_bell";
            case TRAIN -> "horn_train";
            case BOAT -> "horn_boat";
        };
        v.serverWorldSound(s, v.type.horn == VehicleType.Horn.TRAIN || v.type.horn == VehicleType.Horn.BOAT ? 6f : 2.5f, 1f);
    }

    /** Where the turret/gunner is aiming: the driver's view, limited to sensible angles. */
    private static Vec3d aim(PlayerEntity p, float minPitch, float maxPitch) {
        float pitch = MathHelper.clamp(p.getPitch(), minPitch, maxPitch);
        return Vec3d.fromPolar(pitch, p.getHeadYaw());
    }

    // ----------------------------------------------------------- tank

    /** Tank turret pivot and barrel length (must match the tank model). */
    public static final double TURRET_Y = 1.95, TURRET_Z = -0.2, BARREL_LEN = 3.4;

    private static void tankShot(ServerWorld w, VehicleEntity v, PlayerEntity driver) {
        Vec3d dir = aim(driver, -25, 10);
        Vec3d pivot = v.toWorld(0, TURRET_Y, TURRET_Z);
        Vec3d muzzle = pivot.add(dir.multiply(BARREL_LEN));
        w.spawnParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 2, 0.2, 0.2, 0.2, 0);
        w.spawnParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 20, 0.4, 0.4, 0.4, 0.05);
        w.spawnParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 15, 0.2, 0.2, 0.2, 0.1);
        v.serverWorldSound("cannon", 6f, 0.9f);
        shell(w, driver, v, muzzle, dir.multiply(3.6), 0.025, 70, 3.8f, ParticleTypes.SMOKE);
    }

    // ------------------------------------------------------- artillery

    private static void salvo(ServerWorld w, VehicleEntity v, PlayerEntity driver) {
        Vec3d eye = driver.getEyePos();
        Vec3d look = driver.getRotationVec(1f);
        BlockHitResult hit = w.raycast(new RaycastContext(eye, eye.add(look.multiply(220)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, driver));
        Vec3d target = hit.getType() == HitResult.Type.MISS ? eye.add(look.multiply(140)) : hit.getPos();
        driver.sendMessage(Text.translatable("msg.vehicleforge.salvo", (int) target.distanceTo(eye)).formatted(Formatting.RED), true);
        Random r = w.getRandom();
        for (int i = 0; i < 8; i++) {
            final Vec3d spread = target.add((r.nextDouble() - 0.5) * 9, 0, (r.nextDouble() - 0.5) * 9);
            final int col = i % 4, row = i / 4;
            Scheduler.after(i * 4, () -> {
                if (v.isRemoved()) return;
                Vec3d start = v.toWorld(-0.6 + col * 0.4, 2.9 + row * 0.35, -1.6);
                double dist = start.distanceTo(spread);
                int duration = (int) (24 + dist * 0.25);
                double apex = 14 + dist * 0.22;
                v.serverWorldSound("rocket_launch", 4f, 0.9f + r.nextFloat() * 0.2f);
                w.spawnParticles(ParticleTypes.LARGE_SMOKE, start.x, start.y, start.z, 12, 0.3, 0.3, 0.3, 0.05);
                Scheduler.loop(0, age -> {
                    double f = Math.min(1.0, age / (double) duration);
                    Vec3d p = start.lerp(spread, f).add(0, apex * 4 * f * (1 - f), 0);
                    w.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 4, 0.1, 0.1, 0.1, 0.01);
                    w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.01);
                    if (f >= 1.0) {
                        w.createExplosion(driver, p.x, p.y, p.z, 3.4f, World.ExplosionSourceType.MOB);
                        return false;
                    }
                    return true;
                });
            });
        }
    }

    // ---------------------------------------------------- machine guns

    private static void machineGun(ServerWorld w, VehicleEntity v, PlayerEntity driver) {
        boolean plane = v.type.category == Category.PLANE;
        Random r = w.getRandom();
        for (int i = 0; i < 6; i++) {
            final int shot = i;
            Scheduler.after(i * 2, () -> {
                if (v.isRemoved()) return;
                Vec3d dir;
                Vec3d muzzle;
                if (plane) {
                    dir = Vec3d.fromPolar(v.getPitch(), v.getYaw());
                    muzzle = v.toWorld(shot % 2 == 0 ? 1.6 : -1.6, 0.95, 1.6);
                } else {
                    dir = aim(driver, -40, 30);
                    muzzle = v.toWorld(0, 2.55, -1.0).add(dir.multiply(1.7));
                }
                dir = dir.add(r.nextGaussian() * 0.015, r.nextGaussian() * 0.015, r.nextGaussian() * 0.015).normalize();
                bullet(w, v, driver, muzzle, dir, 90, 5f);
                w.spawnParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.01);
                v.serverWorldSound("gunshot", 2.5f, 0.9f + r.nextFloat() * 0.2f);
            });
        }
    }

    private static void bullet(ServerWorld w, VehicleEntity v, PlayerEntity owner, Vec3d from, Vec3d dir, double range, float dmg) {
        Vec3d to = from.add(dir.multiply(range));
        BlockHitResult bh = w.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
        Vec3d end = bh.getType() == HitResult.Type.MISS ? to : bh.getPos();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(from, end).expand(1), e -> e != owner && e.getVehicle() != v && e.isAlive())) {
            var hit = e.getBoundingBox().expand(0.2).raycast(from, end);
            if (hit.isPresent() && hit.get().distanceTo(from) < bestD) {
                bestD = hit.get().distanceTo(from);
                best = e;
            }
        }
        if (best != null) {
            end = from.add(dir.multiply(bestD));
            best.timeUntilRegen = 0;
            best.damage(w.getDamageSources().playerAttack(owner), dmg);
        } else if (bh.getType() != HitResult.Type.MISS) {
            w.spawnParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 6, 0.1, 0.1, 0.1, 0.2);
        }
        double len = end.distanceTo(from);
        for (double d = 1; d < len; d += 2.5) {
            Vec3d p = from.add(dir.multiply(d));
            w.spawnParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ----------------------------------------------------- air rockets

    private static void airRockets(ServerWorld w, VehicleEntity v, PlayerEntity driver) {
        boolean heli = v.type.category == Category.HELICOPTER;
        for (int s = -1; s <= 1; s += 2) {
            final int side = s;
            Scheduler.after(s < 0 ? 0 : 4, () -> {
                if (v.isRemoved()) return;
                Vec3d dir = Vec3d.fromPolar(heli ? MathHelper.clamp(driver.getPitch(), -20, 45) : v.getPitch(), heli ? driver.getHeadYaw() : v.getYaw());
                Vec3d start = heli ? v.toWorld(side * 1.15, 0.85, 0.6) : v.toWorld(side * 1.6, 0.55, 0.2);
                v.serverWorldSound("rocket_launch", 3f, 1.2f);
                shell(w, driver, v, start.add(dir.multiply(1.5)), dir.multiply(2.8), 0, 60, 2.8f, ParticleTypes.FLAME);
            });
        }
    }

    // ------------------------------------------------------ projectile

    private static void shell(ServerWorld w, Entity owner, VehicleEntity v, Vec3d start, Vec3d velocity, double gravity,
                              int life, float power, ParticleEffect trail) {
        final Vec3d[] state = {start, velocity};
        Scheduler.loop(0, age -> {
            Vec3d cur = state[0];
            Vec3d next = cur.add(state[1]);
            BlockHitResult bh = w.raycast(new RaycastContext(cur, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
            Vec3d end = bh.getType() == HitResult.Type.MISS ? next : bh.getPos();
            List<LivingEntity> hits = w.getEntitiesByClass(LivingEntity.class, new Box(cur, end).expand(0.6),
                    e -> e != owner && e.getVehicle() != v && e.isAlive());
            int steps = Math.max(1, (int) (cur.distanceTo(end) / 0.7));
            for (int i = 0; i < steps; i++) {
                Vec3d p = cur.lerp(end, i / (double) steps);
                w.spawnParticles(trail, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            }
            if (!hits.isEmpty() || bh.getType() != HitResult.Type.MISS || age >= life) {
                Vec3d at = hits.isEmpty() ? end : hits.get(0).getBoundingBox().getCenter();
                w.createExplosion(owner, at.x, at.y, at.z, power, World.ExplosionSourceType.MOB);
                return false;
            }
            state[0] = next;
            state[1] = state[1].add(0, -gravity, 0);
            return true;
        });
    }

    // ------------------------------------------------------- fire hose

    public static final double NOZZLE_Y = 2.95, NOZZLE_Z = 0.6;

    public static void sprayTick(VehicleEntity v) {
        LivingEntity driver = v.getControllingPassenger();
        if (driver == null) {
            v.setSpray(false);
            return;
        }
        ServerWorld w = (ServerWorld) v.getWorld();
        Vec3d dir = Vec3d.fromPolar(MathHelper.clamp(driver.getPitch(), -45, 30), driver.getHeadYaw());
        Vec3d nozzle = v.toWorld(0, NOZZLE_Y, NOZZLE_Z).add(dir.multiply(1.5));
        for (double d = 0; d < 18; d += 1.5) {
            Vec3d p = nozzle.add(dir.multiply(d)).add(0, -0.012 * d * d, 0);
            w.spawnParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 5, 0.15 + d * 0.02, 0.15, 0.15 + d * 0.02, 0.05);
            if (v.age % 2 == 0) w.spawnParticles(ParticleTypes.FALLING_WATER, p.x, p.y, p.z, 2, 0.2, 0.2, 0.2, 0);
            if (v.age % 3 != 0) continue;
            BlockPos c = BlockPos.ofFloored(p);
            for (BlockPos bp : BlockPos.iterate(c.add(-1, -2, -1), c.add(1, 1, 1))) {
                if (w.getBlockState(bp).isIn(BlockTags.FIRE)) w.removeBlock(bp, false);
            }
            for (Entity e : w.getOtherEntities(v, new Box(p, p).expand(1.2))) {
                if (e == driver || e.getVehicle() == v) continue;
                e.extinguish();
                e.addVelocity(dir.x * 0.12, 0.02, dir.z * 0.12);
                e.velocityModified = true;
            }
        }
        if (v.age % 12 == 0) v.serverWorldSound("water_spray", 2f, 1f);
        Vec3d end = nozzle.add(dir.multiply(18)).add(0, -3.5, 0);
        w.spawnParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 3, 0.6, 0.3, 0.6, 0.02);
    }
}
