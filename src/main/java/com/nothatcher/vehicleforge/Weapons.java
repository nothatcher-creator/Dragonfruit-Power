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
        switch (t.weapon) {
            case TIME_JUMP -> {
                timeJump(v, driver);
                return;
            }
            case SHRINK -> {
                boolean shrink = v.getScaleTarget() > 0.9f;
                v.setScaleTarget(shrink ? 0.3f : 1f);
                v.serverWorldSound("shrink", 1.5f, shrink ? 1.4f : 0.7f);
                ((ServerWorld) v.getWorld()).spawnParticles(ParticleTypes.END_ROD, v.getX(), v.getY() + 0.8, v.getZ(), 40, 1.0, 0.6, 1.0, 0.05);
                if (!shrink) {
                    for (Entity p : v.getPassengerList()) if (p instanceof LivingEntity le) le.removeStatusEffect(ModEffects.SHRUNK);
                }
                driver.sendMessage(Text.translatable(shrink ? "msg.vehicleforge.shrink" : "msg.vehicleforge.grow").formatted(Formatting.AQUA), true);
                return;
            }
            case TRACTOR_BEAM, MOWER, FORKS -> {
                v.setAbility(!v.abilityOn());
                String key = t.weapon == VehicleType.Weapon.TRACTOR_BEAM ? "beam" : t.weapon == VehicleType.Weapon.MOWER ? "blades" : "forks";
                driver.sendMessage(Text.translatable("msg.vehicleforge." + key + (v.abilityOn() ? "_on" : "_off")).formatted(Formatting.YELLOW), true);
                if (t.weapon == VehicleType.Weapon.TRACTOR_BEAM && v.abilityOn()) v.serverWorldSound("ufo_hum", 2f, 1.6f);
                return;
            }
            case BOOST -> {
                if (v.abilityTicks <= 0) {
                    v.setAbility(true);
                    v.abilityTicks = 40;
                    v.serverWorldSound("rocket_launch", 2.5f, 1.3f);
                }
                return;
            }
            case COMPACTOR -> {
                compact(v, driver);
                return;
            }
            case DIG -> {
                dig(v, driver);
                return;
            }
            case BLADE, PAVE -> {
                v.setAbility(!v.abilityOn());
                String key = t.weapon == VehicleType.Weapon.PAVE ? "paving" : t == VehicleTypes.LOADER ? "bucket" : "blade";
                driver.sendMessage(Text.translatable("msg.vehicleforge." + key + (v.abilityOn() ? "_on" : "_off")).formatted(Formatting.YELLOW), true);
                return;
            }
            case CRANE, TOW -> {
                if (v.carried != null) {
                    release(v);
                    driver.sendMessage(Text.translatable("msg.vehicleforge.released").formatted(Formatting.YELLOW), true);
                } else {
                    grab(v, driver);
                }
                return;
            }
            default -> { }
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
            case OSTRICH -> "ostrich_call";
            case ICE_CREAM -> "ice_cream_jingle";
        };
        boolean loud = v.type.horn == VehicleType.Horn.TRAIN || v.type.horn == VehicleType.Horn.BOAT || v.type.horn == VehicleType.Horn.ICE_CREAM;
        v.serverWorldSound(s, loud ? 6f : 2.5f, v.type.horn == VehicleType.Horn.OSTRICH ? 0.9f + v.getWorld().random.nextFloat() * 0.2f : 1f);
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
                    muzzle = v.toWorld(v.type.turret.x, v.type.turret.y, v.type.turret.z).add(dir.multiply(v.type.barrelLen));
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

    // ------------------------------------------------------ wave 3 specials

    /**
     * Time machine: at speed, jump back to where the car was 30 seconds ago and roll the clock back six hours.
     */
    private static void timeJump(VehicleEntity v, ServerPlayerEntity driver) {
        if (!v.abilityOn()) {
            int need = Math.round(v.type.maxSpeed * 0.75f * 20 * 3.6f);
            driver.sendMessage(Text.translatable("msg.vehicleforge.time_need", need).formatted(Formatting.LIGHT_PURPLE), true);
            return;
        }
        if (v.timeHistory.size() < 4) {
            driver.sendMessage(Text.translatable("msg.vehicleforge.time_short").formatted(Formatting.LIGHT_PURPLE), true);
            return;
        }
        ServerWorld w = (ServerWorld) v.getWorld();
        double[] to = v.timeHistory.peekLast();
        int secondsBack = v.timeHistory.size() / 2;
        // departure: a flash and a ring of sparks
        w.spawnParticles(ParticleTypes.FLASH, v.getX(), v.getY() + 1, v.getZ(), 1, 0, 0, 0, 0);
        w.spawnParticles(ParticleTypes.END_ROD, v.getX(), v.getY() + 0.8, v.getZ(), 80, 1.5, 0.6, 1.5, 0.15);
        w.spawnParticles(ParticleTypes.REVERSE_PORTAL, v.getX(), v.getY() + 0.8, v.getZ(), 120, 1.2, 0.8, 1.2, 0.2);
        v.serverWorldSound("time_warp", 4f, 1f);
        long tod = w.getTimeOfDay();
        long back = tod - 6000;
        if (back < 0) back += 24000;
        w.setTimeOfDay(back);
        v.refreshPositionAndAngles(to[0], to[1], to[2], (float) to[3], 0);
        v.timeHistory.clear();
        driver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket(v));
        w.spawnParticles(ParticleTypes.FLASH, v.getX(), v.getY() + 1, v.getZ(), 1, 0, 0, 0, 0);
        w.spawnParticles(ParticleTypes.REVERSE_PORTAL, v.getX(), v.getY() + 0.8, v.getZ(), 120, 1.2, 0.8, 1.2, 0.2);
        v.serverWorldSound("time_warp", 4f, 0.7f);
        driver.sendMessage(Text.translatable("msg.vehicleforge.time_jump", secondsBack).formatted(Formatting.LIGHT_PURPLE), true);
    }

    /** UFO: lift creatures and dropped items under the saucer up into the beam. */
    public static void beamTick(VehicleEntity v) {
        ServerWorld w = (ServerWorld) v.getWorld();
        Vec3d top = v.getPos().add(0, -0.4, 0);
        Box box = new Box(v.getX() - 2.5, v.getY() - 28, v.getZ() - 2.5, v.getX() + 2.5, v.getY(), v.getZ() + 2.5);
        for (Entity e : w.getOtherEntities(v, box, e -> e.getRootVehicle() != v && (e instanceof LivingEntity || e instanceof net.minecraft.entity.ItemEntity))) {
            Vec3d d = top.subtract(e.getPos());
            Vec3d pull = d.length() > 0.5 ? d.normalize().multiply(Math.min(0.45, d.length() * 0.12)) : d.multiply(0.3);
            e.setVelocity(pull);
            e.velocityModified = true;
            e.fallDistance = 0;
        }
        if (v.age % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double y = v.getY() - w.random.nextDouble() * 20;
                double a = w.random.nextDouble() * Math.PI * 2, r = (v.getY() - y) * 0.09 + 0.4;
                w.spawnParticles(ParticleTypes.END_ROD, v.getX() + Math.cos(a) * r, y, v.getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
            }
        }
        if (v.age % 40 == 0) v.serverWorldSound("ufo_hum", 2f, 1.6f);
    }

    /** Lawn mower: cut grass, ferns and flowers under and beside the deck (crops and saplings are left alone). */
    public static void mowTick(VehicleEntity v) {
        ServerWorld w = (ServerWorld) v.getWorld();
        BlockPos c = BlockPos.ofFloored(v.toWorld(0, 0.2, 0.3));
        int cut = 0;
        for (BlockPos bp : BlockPos.iterate(c.add(-1, -1, -1), c.add(1, 1, 1))) {
            net.minecraft.block.BlockState st = w.getBlockState(bp);
            net.minecraft.block.Block b = st.getBlock();
            boolean plant = (b instanceof net.minecraft.block.PlantBlock || b instanceof net.minecraft.block.TallPlantBlock)
                    && !(b instanceof net.minecraft.block.CropBlock) && !(b instanceof net.minecraft.block.SaplingBlock)
                    && !(b instanceof net.minecraft.block.StemBlock) && !(b instanceof net.minecraft.block.SweetBerryBushBlock);
            if (plant) {
                w.breakBlock(bp, false);
                w.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK, st), bp.getX() + 0.5, bp.getY() + 0.3, bp.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.05);
                cut++;
            }
        }
        if (cut > 0 && v.age % 6 == 0) w.playSound(null, v.getX(), v.getY(), v.getZ(), net.minecraft.sound.SoundEvents.BLOCK_GRASS_BREAK, net.minecraft.sound.SoundCategory.NEUTRAL, 0.8f, 1.2f);
    }

    /** Garbage truck: scoop up dropped items behind the truck. */
    private static void compact(VehicleEntity v, ServerPlayerEntity driver) {
        ServerWorld w = (ServerWorld) v.getWorld();
        Vec3d rear = v.toWorld(0, 0.5, v.type.rearZ);
        int n = 0;
        for (net.minecraft.entity.ItemEntity item : w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, new Box(rear, rear).expand(5, 3, 5), e -> true)) {
            n += item.getStack().getCount();
            w.spawnParticles(ParticleTypes.POOF, item.getX(), item.getY(), item.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
            item.discard();
        }
        v.serverWorldSound("compactor", 2f, 1f);
        driver.sendMessage(Text.translatable("msg.vehicleforge.collected", n).formatted(Formatting.GREEN), true);
    }

    // ---------------------------------------------------- construction

    /** blocks a machine may scoop or push: earth, sand, gravel, snow, plants and trees, never buildings or roads */
    private static boolean earthy(net.minecraft.block.BlockState st) {
        if (st.isAir() || st.hasBlockEntity()) return false;
        net.minecraft.block.Block b = st.getBlock();
        return st.isIn(BlockTags.DIRT) || st.isIn(BlockTags.SAND) || st.isIn(BlockTags.SNOW) || st.isIn(BlockTags.LEAVES)
                || st.isIn(BlockTags.LOGS) || st.isIn(BlockTags.FLOWERS) || st.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || b == net.minecraft.block.Blocks.GRAVEL || b == net.minecraft.block.Blocks.CLAY || b == net.minecraft.block.Blocks.COBBLESTONE
                || b == net.minecraft.block.Blocks.DIRT_PATH || b == net.minecraft.block.Blocks.FARMLAND
                || b instanceof net.minecraft.block.PlantBlock || b instanceof net.minecraft.block.TallPlantBlock;
    }

    private static boolean litter(net.minecraft.block.BlockState st) {
        return st.isIn(BlockTags.LEAVES) || st.getBlock() instanceof net.minecraft.block.PlantBlock
                || st.getBlock() instanceof net.minecraft.block.TallPlantBlock || st.isOf(net.minecraft.block.Blocks.SNOW);
    }

    /** Excavator: scoop out a 2x2 bite of earth or stone wherever the operator is looking (up to 9 blocks away). */
    private static void dig(VehicleEntity v, ServerPlayerEntity driver) {
        if (v.abilityTicks > 0) return;
        ServerWorld w = (ServerWorld) v.getWorld();
        Vec3d eye = driver.getEyePos();
        Vec3d end = eye.add(driver.getRotationVec(1f).multiply(9));
        BlockHitResult hit = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, driver));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos p = hit.getBlockPos();
        net.minecraft.util.math.Direction right = driver.getHorizontalFacing().rotateYClockwise();
        int dug = 0;
        for (BlockPos bp : new BlockPos[]{p, p.up(), p.offset(right), p.offset(right).up()}) {
            net.minecraft.block.BlockState st = w.getBlockState(bp);
            if (!earthy(st) || st.getHardness(w, bp) < 0) continue;
            w.breakBlock(bp, !litter(st), driver);
            dug++;
        }
        if (dug > 0) {
            v.setAbility(true);
            v.abilityTicks = 10;
            w.playSound(null, p, net.minecraft.sound.SoundEvents.BLOCK_GRAVEL_BREAK, net.minecraft.sound.SoundCategory.BLOCKS, 1.5f, 0.7f);
        }
    }

    /** Bulldozer / loader: clear earth, plants and trees in front of the blade while driving. */
    public static void bladeTick(VehicleEntity v) {
        ServerWorld w = (ServerWorld) v.getWorld();
        double hw = v.type.width / 2;
        int y0 = net.minecraft.util.math.MathHelper.floor(v.getY() + 0.1);
        boolean any = false;
        for (double lx = -hw; lx <= hw + 0.01; lx += 0.5) {
            Vec3d at = v.toWorld(lx, 0, v.type.frontZ + 0.4);
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos bp = BlockPos.ofFloored(at.x, y0 + dy, at.z);
                net.minecraft.block.BlockState st = w.getBlockState(bp);
                if (!earthy(st) || st.getHardness(w, bp) < 0 || st.getHardness(w, bp) > 2.5f) continue;
                w.breakBlock(bp, !litter(st) && w.random.nextInt(3) == 0);
                any = true;
            }
        }
        if (any && v.age % 5 == 0) w.playSound(null, v.getX(), v.getY(), v.getZ(), net.minecraft.sound.SoundEvents.BLOCK_ROOTED_DIRT_BREAK,
                net.minecraft.sound.SoundCategory.BLOCKS, 1.2f, 0.6f);
    }

    /** Road roller: lay asphalt on earth, gravel and stone under the drum, and flatten the plants on top. */
    public static void paveTick(VehicleEntity v) {
        ServerWorld w = (ServerWorld) v.getWorld();
        double hw = v.type.width / 2 - 0.1;
        int yFeet = net.minecraft.util.math.MathHelper.floor(v.getY() + 0.05);
        for (double lx = -hw; lx <= hw + 0.01; lx += 0.5) {
            Vec3d at = v.toWorld(lx, 0, v.type.frontZ - 0.9);
            BlockPos feet = BlockPos.ofFloored(at.x, yFeet, at.z);
            BlockPos below = feet.down();
            if (litter(w.getBlockState(feet))) w.breakBlock(feet, false);
            net.minecraft.block.BlockState st = w.getBlockState(below);
            if (earthy(st) && !st.isIn(BlockTags.LEAVES) && !st.isIn(BlockTags.LOGS) && !(st.getBlock() instanceof net.minecraft.block.PlantBlock)) {
                w.setBlockState(below, com.nothatcher.vehicleforge.block.CityBlocks.ASPHALT.getDefaultState());
                if (v.age % 3 == 0) w.spawnParticles(ParticleTypes.SMOKE, below.getX() + 0.5, below.getY() + 1.1, below.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.01);
            }
        }
    }

    /** where the crane hook hangs: the boom swings and lifts with the operator's view */
    private static Vec3d hookPos(VehicleEntity v, LivingEntity driver) {
        float yaw = driver.getHeadYaw();
        float pitch = net.minecraft.util.math.MathHelper.clamp(driver.getPitch(), -60, 5);
        Vec3d pivot = v.toWorld(v.type.turret.x, v.type.turret.y + 0.3, v.type.turret.z);
        return pivot.add(Vec3d.fromPolar(pitch, yaw).multiply(v.type.barrelLen)).add(0, -2.6, 0);
    }

    private static void grab(VehicleEntity v, ServerPlayerEntity driver) {
        ServerWorld w = (ServerWorld) v.getWorld();
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        if (v.type.weapon == VehicleType.Weapon.CRANE) {
            Vec3d hook = hookPos(v, driver);
            for (Entity e : w.getOtherEntities(v, new Box(hook, hook).expand(3.5), e -> !(e instanceof PlayerEntity)
                    && e.getRootVehicle() != v && (e instanceof LivingEntity || e instanceof net.minecraft.entity.ItemEntity
                    || (e instanceof VehicleEntity ve && !ve.hasPassengers())))) {
                double d = e.squaredDistanceTo(hook);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
        } else {
            Vec3d rear = v.toWorld(0, 0.5, v.type.rearZ);
            for (VehicleEntity e : w.getEntitiesByClass(VehicleEntity.class, new Box(rear, rear).expand(6, 3, 6), e -> e != v && !e.hasPassengers())) {
                double d = e.squaredDistanceTo(rear);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
        }
        if (best == null) {
            driver.sendMessage(Text.translatable(v.type.weapon == VehicleType.Weapon.CRANE ? "msg.vehicleforge.nothing_hook" : "msg.vehicleforge.nothing_tow")
                    .formatted(Formatting.GRAY), true);
            return;
        }
        v.carried = best;
        if (best instanceof VehicleEntity ve) ve.carriedBy = v;
        w.playSound(null, best.getX(), best.getY(), best.getZ(), net.minecraft.sound.SoundEvents.BLOCK_CHAIN_PLACE, net.minecraft.sound.SoundCategory.NEUTRAL, 1.5f, 0.8f);
        driver.sendMessage(Text.translatable(v.type.weapon == VehicleType.Weapon.CRANE ? "msg.vehicleforge.hooked" : "msg.vehicleforge.towing",
                best.getDisplayName()).formatted(Formatting.GREEN), true);
    }

    public static void release(VehicleEntity v) {
        if (v.carried instanceof VehicleEntity ve && ve.carriedBy == v) ve.carriedBy = null;
        if (v.carried != null) v.carried.setVelocity(Vec3d.ZERO);
        v.carried = null;
    }

    /** Crane: the load hangs under the hook and follows the boom. */
    public static void craneTick(VehicleEntity v) {
        LivingEntity driver = v.getControllingPassenger();
        Entity c = v.carried;
        if (driver == null || c == null) return;
        Vec3d hook = hookPos(v, driver);
        Vec3d target = new Vec3d(hook.x, hook.y - c.getHeight() - 0.3, hook.z);
        Vec3d now = c.getPos();
        Vec3d next = now.add(target.subtract(now).multiply(0.35));
        if (c instanceof VehicleEntity) c.refreshPositionAndAngles(next.x, next.y, next.z, c.getYaw(), 0);
        else c.setPosition(next.x, next.y, next.z);
        c.setVelocity(Vec3d.ZERO);
        c.velocityModified = true;
        c.fallDistance = 0;
    }

    /** Tow truck: the towed vehicle follows the hitch, nose up to the truck. */
    public static void towTick(VehicleEntity v) {
        if (!(v.carried instanceof VehicleEntity t)) return;
        Vec3d hitch = v.toWorld(0, 0, v.type.rearZ);
        double dx = hitch.x - t.getX(), dz = hitch.z - t.getZ();
        double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        double back = t.type.frontZ + 0.5;
        double nx = hitch.x - dx / len * back, nz = hitch.z - dz / len * back;
        t.refreshPositionAndAngles(nx, v.getY(), nz, yaw, 0);
        t.setVelocity(Vec3d.ZERO);
        t.velocityModified = true;
    }
}
