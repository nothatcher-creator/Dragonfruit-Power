package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleType.Category;
import com.nothatcher.vehicleforge.interior.InteriorManager;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * One entity class drives every vehicle; behaviour comes from its VehicleType.
 * Physics run on the driver's client (smooth, like vanilla boats) and on the server when nobody drives.
 */
public class VehicleEntity extends Entity {
    private static final TrackedData<Integer> PAINT = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> HEALTH = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> SIREN = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SPRAY = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CARRIAGES = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> WOBBLE = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> FUEL = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.FLOAT);

    /** Client hook (engine sounds etc.), set by the client mod. */
    public static Consumer<VehicleEntity> clientTickHook = e -> { };
    /** Client hook: the driver's client reports crashes to the server (set by the client mod). */
    public static BiConsumer<VehicleEntity, Float> crashHook = (e, impact) -> { };

    public final VehicleType type;

    // driving state (on the logical side)
    public float speed, throttle;
    private float yawDelta;
    private Direction railDir;
    private float prevVy;

    // visual state (client)
    public float steerVisual, prevSteerVisual, roll, prevRoll;
    public float wheelSpin, prevWheelSpin, rotorSpin, prevRotorSpin;
    public float clientSpeed, prevClientSpeed, accelVis, slipVis;
    public boolean thrusting, braking, lightsOn;
    public Object engineSound;
    /** gearbox (visual + sound): -1 = reverse; rpm 0..1 */
    public int gear = 1;
    public float rpm, prevRpm;
    /** suspension / terrain body motion (degrees) */
    public float bodyPitch, prevBodyPitch, bodyRoll, prevBodyRoll;
    private float pitchVel, rollVel;
    private float steerInput;
    private boolean wasOnGround = true;
    private double lastFallVy;
    private Vec3d lastEnd;
    /** horizontal speed measured from real motion, smoothed (all sides; used for impacts on the server) */
    public float measuredSpeed;
    private Vec3d motionDir = Vec3d.ZERO;

    // remote interpolation
    private int lerpTicks;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw, lerpPitch;

    /** train: positions the locomotive passed through, newest first (both sides) */
    private final ArrayDeque<Vec3d> path = new ArrayDeque<>();

    public VehicleEntity(EntityType<? extends VehicleEntity> entityType, World world) {
        super(entityType, world);
        this.type = ModRegistry.TYPE_OF.get(entityType);
        this.setStepHeight(type.stepHeight);
        this.intersectionChecked = true;
    }

    // ---------------------------------------------------------------- data

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(PAINT, -1);
        dataTracker.startTracking(HEALTH, -1f);
        dataTracker.startTracking(SIREN, false);
        dataTracker.startTracking(SPRAY, false);
        dataTracker.startTracking(CARRIAGES, -1);
        dataTracker.startTracking(WOBBLE, 0);
        dataTracker.startTracking(FUEL, -1f);
    }

    /** 0..1 */
    public float getFuel() {
        float f = dataTracker.get(FUEL);
        return f < 0 ? 1f : f;
    }

    public void setFuel(float f) {
        dataTracker.set(FUEL, MathHelper.clamp(f, 0f, 1f));
    }

    public boolean hasFuel() {
        return !type.usesFuel() || getFuel() > 0;
    }

    /** someone is in the driver's seat and the tank isn't dry */
    public boolean engineRunning() {
        return getControllingPassenger() != null && hasFuel();
    }

    public int getPaint() {
        int p = dataTracker.get(PAINT);
        return p < 0 ? type.defaultPaint : p;
    }

    public void setPaint(int rgb) {
        dataTracker.set(PAINT, rgb & 0xFFFFFF);
    }

    public float getHealth() {
        float h = dataTracker.get(HEALTH);
        return h < 0 ? type.health : h;
    }

    public void setHealth(float h) {
        dataTracker.set(HEALTH, Math.max(0, h));
    }

    public boolean sirenOn() {
        return dataTracker.get(SIREN);
    }

    public void setSiren(boolean on) {
        dataTracker.set(SIREN, on);
    }

    public boolean sprayOn() {
        return dataTracker.get(SPRAY);
    }

    public void setSpray(boolean on) {
        dataTracker.set(SPRAY, on);
    }

    public int getCarriages() {
        int c = dataTracker.get(CARRIAGES);
        return c < 0 ? type.carriages : c;
    }

    public int getWobble() {
        return dataTracker.get(WOBBLE);
    }

    public int interiorId = -1;

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("Paint")) dataTracker.set(PAINT, nbt.getInt("Paint"));
        if (nbt.contains("Health")) dataTracker.set(HEALTH, nbt.getFloat("Health"));
        if (nbt.contains("Carriages")) dataTracker.set(CARRIAGES, nbt.getInt("Carriages"));
        if (nbt.contains("Fuel")) dataTracker.set(FUEL, nbt.getFloat("Fuel"));
        interiorId = nbt.contains("Interior") ? nbt.getInt("Interior") : -1;
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Paint", dataTracker.get(PAINT));
        nbt.putFloat("Health", dataTracker.get(HEALTH));
        nbt.putInt("Carriages", dataTracker.get(CARRIAGES));
        nbt.putFloat("Fuel", dataTracker.get(FUEL));
        nbt.putInt("Interior", interiorId);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    // ------------------------------------------------------------- basics

    @Override
    public boolean canHit() {
        return !isRemoved();
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getPickBlockStack() {
        return new ItemStack(ModRegistry.ITEMS.get(type.id));
    }

    @Override
    public Box getVisibilityBoundingBox() {
        return getBoundingBox().expand(type.category == Category.TRAIN ? 30 : 8, 8, type.category == Category.TRAIN ? 30 : 8);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof PlayerEntity p ? p : null;
    }

    /** unit vector to the vehicle's left */
    public Vec3d leftVec() {
        double yaw = Math.toRadians(getYaw());
        return new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
    }

    public Vec3d forward() {
        double yaw = Math.toRadians(getYaw());
        return new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** local (x = left, y = up, z = forward) to world, including pitch for aircraft */
    public Vec3d toWorld(double x, double y, double z) {
        return getPos().add(rotate(x, y, z, getYaw(), getPitch()));
    }

    public static Vec3d rotate(double x, double y, double z, float yawDeg, float pitchDeg) {
        double p = Math.toRadians(pitchDeg);
        double cp = Math.cos(p), sp = Math.sin(p);
        double y2 = y * cp - z * sp;
        double z2 = y * sp + z * cp;
        double yaw = Math.toRadians(yawDeg);
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new Vec3d(x * c - z2 * s, y2, x * s + z2 * c);
    }

    // ------------------------------------------------------------- seats

    public int maxSeats() {
        return type.seats.size() + (type.category == Category.TRAIN ? getCarriages() * 4 : 0);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengerList().size() < maxSeats();
    }

    public Vec3d seatPos(int index) {
        if (index < type.seats.size()) {
            Vec3d s = type.seats.get(index);
            float pitch = type.flies() ? getPitch() : 0;
            return getPos().add(rotate(s.x, s.y, s.z, getYaw(), pitch));
        }
        int k = index - type.seats.size();
        int car = k / 4 + 1, slot = k % 4;
        CarriagePose pose = carriagePose(car);
        double sx = (slot % 2 == 0) ? 0.35 : -0.35;
        double sz = slot < 2 ? 1.3 : -1.3;
        return pose.pos.add(rotate(sx, 0.95, sz, pose.yaw, 0));
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        if (!hasPassenger(passenger)) return;
        int i = getPassengerList().indexOf(passenger);
        Vec3d p = seatPos(i);
        positionUpdater.accept(passenger, p.x, p.y + passenger.getHeightOffset(), p.z);
        if (yawDelta != 0 && i < type.seats.size()) {
            passenger.setYaw(passenger.getYaw() + yawDelta);
            passenger.setHeadYaw(passenger.getHeadYaw() + yawDelta);
        }
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        int i = Math.max(0, getPassengerList().indexOf(passenger));
        Vec3d seat = seatPos(i);
        Vec3d left = rotate(1, 0, 0, getYaw(), 0);
        double out = type.width / 2 + 0.8;
        Vec3d[] tries = {
                seat.add(left.multiply(out)), seat.subtract(left.multiply(out)),
                getPos().add(forward().multiply(-(type.width + 1.5))), getPos().add(0, type.height + 0.2, 0)
        };
        for (Vec3d t : tries) {
            Vec3d ground = new Vec3d(t.x, Math.max(getY(), t.y - 1.5), t.z);
            Box box = passenger.getBoundingBox().offset(ground.subtract(passenger.getPos()));
            if (getWorld().isSpaceEmpty(passenger, box)) return ground;
        }
        return super.updatePassengerForDismount(passenger);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.shouldCancelInteraction()) {
            if (type.interiorRoom) {
                if (!getWorld().isClient && player instanceof ServerPlayerEntity sp) InteriorManager.enter(sp, this);
                return ActionResult.success(getWorld().isClient);
            }
            return ActionResult.PASS;
        }
        if (stack.getItem() instanceof DyeItem dye) {
            if (!getWorld().isClient) {
                setPaint(dye.getColor().getFireworkColor());
                if (!player.getAbilities().creativeMode) stack.decrement(1);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (stack.isOf(ModRegistry.FUEL_CAN) && type.usesFuel()) {
            if (!getWorld().isClient) {
                if (getFuel() >= 0.999f) {
                    player.sendMessage(Text.translatable("msg.vehicleforge.tank_full").formatted(Formatting.GRAY), true);
                } else {
                    setFuel(getFuel() + 0.5f);
                    serverWorldSound("fuel_fill", 1f, 1f);
                    player.sendMessage(Text.translatable("msg.vehicleforge.refueled", Math.round(getFuel() * 100)).formatted(Formatting.GOLD), true);
                    if (!player.getAbilities().creativeMode) stack.decrement(1);
                }
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (stack.isOf(Items.IRON_INGOT) && getHealth() < type.health) {
            if (!getWorld().isClient) {
                setHealth(Math.min(type.health, getHealth() + type.health * 0.2f));
                getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.6f, 1.3f);
                player.sendMessage(Text.translatable("msg.vehicleforge.repaired", Math.round(getHealth() / type.health * 100)).formatted(Formatting.GREEN), true);
                if (!player.getAbilities().creativeMode) stack.decrement(1);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (type.category == Category.TRAIN && stack.isOf(ModRegistry.CARRIAGE_ITEM)) {
            if (!getWorld().isClient && getCarriages() < 6) {
                dataTracker.set(CARRIAGES, getCarriages() + 1);
                if (!player.getAbilities().creativeMode) stack.decrement(1);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (!getWorld().isClient) return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
        return ActionResult.SUCCESS;
    }

    // ------------------------------------------------------------- damage

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isRemoved()) return false;
        if (getWorld().isClient) return true;
        boolean creative = source.getAttacker() instanceof PlayerEntity p && p.getAbilities().creativeMode;
        dataTracker.set(WOBBLE, 10);
        float h = getHealth() - (creative ? type.health : amount * (source.getAttacker() instanceof PlayerEntity ? 4f : 1f));
        setHealth(h);
        if (h <= 0) destroy(!creative && source.getAttacker() instanceof PlayerEntity, creative);
        return true;
    }

    /** drop = broken up by a player (gives the item back); otherwise it's a wreck and goes up in flames */
    private void destroy(boolean drop, boolean quiet) {
        if (drop) {
            dropStack(new ItemStack(ModRegistry.ITEMS.get(type.id)));
        } else if (!quiet) {
            getWorld().createExplosion(this, getX(), getY() + 0.5, getZ(), 2.5f, World.ExplosionSourceType.NONE);
        }
        removeAllPassengers();
        discard();
    }

    /** Called by physics when the vehicle slams into something. Driver's client forwards it to the server. */
    private void reportCrash(float impact) {
        if (impact < 0.45f) return;
        if (getWorld().isClient) crashHook.accept(this, impact);
        else crash(impact);
    }

    /** Server: crash damage to the vehicle and (in big hits) the people inside. */
    public void crash(float impact) {
        if (getWorld().isClient || isRemoved()) return;
        impact = Math.min(impact, type.maxSpeed * 1.4f + 0.3f);
        if (impact < 0.45f) return;
        dataTracker.set(WOBBLE, 12);
        serverWorldSound("crash", 1.2f + impact, 0.85f + random.nextFloat() * 0.3f);
        if (getWorld() instanceof ServerWorld w) {
            Vec3d front = toWorld(0, type.height * 0.4, clientSpeed >= 0 ? type.frontZ : type.rearZ);
            w.spawnParticles(ParticleTypes.CRIT, front.x, front.y, front.z, 12, 0.4, 0.3, 0.4, 0.3);
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, front.x, front.y, front.z, 6, 0.3, 0.2, 0.3, 0.02);
        }
        if (impact > 0.75f) {
            for (Entity p : getPassengerList()) {
                if (p instanceof LivingEntity le) le.damage(getDamageSources().flyIntoWall(), Math.min(16f, (impact - 0.6f) * 14f));
            }
        }
        float h = getHealth() - (impact - 0.35f) * type.health * 0.35f;
        setHealth(h);
        if (h <= 0) destroy(false, false);
    }

    // --------------------------------------------------------------- tick

    @Override
    public void tick() {
        prevSteerVisual = steerVisual;
        prevRoll = roll;
        prevWheelSpin = wheelSpin;
        prevRotorSpin = rotorSpin;
        prevRpm = rpm;
        prevBodyPitch = bodyPitch;
        prevBodyRoll = bodyRoll;
        float oyaw = getYaw();
        super.tick();
        if (getWobble() > 0 && !getWorld().isClient) dataTracker.set(WOBBLE, getWobble() - 1);

        if (isLogicalSideForUpdatingMovement()) {
            lerpTicks = 0;
            physics();
        } else {
            yawDelta = 0;
            if (getWorld().isClient) lerpStep();
            else setVelocity(Vec3d.ZERO);
        }

        if (type.category == Category.TRAIN) recordPath();

        // motion since the end of last tick: works for the driver, other clients and the server alike
        Vec3d now = getPos();
        Vec3d d = lastEnd == null ? Vec3d.ZERO : now.subtract(lastEnd);
        lastEnd = now;
        if (d.lengthSquared() > 64) d = Vec3d.ZERO;   // teleported
        double moved = Math.sqrt(d.x * d.x + d.z * d.z);
        double dy = d.y;
        Vec3d f = forward();
        Vec3d left = leftVec();
        double signed = d.x * f.x + d.z * f.z;
        prevClientSpeed = clientSpeed;
        clientSpeed = (float) signed;
        measuredSpeed = measuredSpeed * 0.5f + (float) moved * 0.5f;
        if (moved > 0.01) motionDir = new Vec3d(d.x / moved, 0, d.z / moved);
        accelVis += ((clientSpeed - prevClientSpeed) - accelVis) * 0.25f;
        slipVis = (float) Math.abs(d.x * left.x + d.z * left.z);
        braking = (accelVis < -0.004f && clientSpeed > 0.05f) || clientSpeed < -0.02f;

        wheelSpin += (float) (signed / 0.4);
        float yawRate = MathHelper.wrapDegrees(getYaw() - oyaw);
        float steerTarget = type.turn > 0 ? MathHelper.clamp(-yawRate / Math.max(0.5f, type.turn), -1, 1) : 0;
        steerVisual += (steerTarget - steerVisual) * 0.3f;
        // bikes and aircraft lean INTO the turn (positive Z roll = toward the right side)
        float rollTarget = switch (type.category) {
            case BIKE -> -steerVisual * 25f * Math.min(1f, Math.abs(clientSpeed) / 0.5f);
            case PLANE -> -steerVisual * 35f;
            case HELICOPTER -> -steerVisual * 12f;
            case BOAT -> -steerVisual * 8f * Math.min(1f, Math.abs(clientSpeed) / 0.6f);
            default -> 0f;
        };
        roll += (rollTarget - roll) * 0.15f;
        boolean powered = engineRunning() || moved > 0.02;
        if (type.category == Category.HELICOPTER || type.category == Category.PLANE) {
            rotorSpin += powered ? (type.category == Category.HELICOPTER ? 55f : 70f) : 0f;
        }
        if (type.category == Category.SUBMARINE) rotorSpin += Math.abs(clientSpeed) * 60f + (engineRunning() ? 4f : 0f);
        thrusting = type.category == Category.ROCKET ? dy > 0.04 : (type.category == Category.PLANE && moved > 0.4);

        updateGearbox();
        if (getWorld().isClient) {
            updateBody();
            long t = getWorld().getTimeOfDay() % 24000L;
            lightsOn = getControllingPassenger() != null && (t > 12600 && t < 23400 || getWorld().isRaining()
                    || getWorld().getLightLevel(LightType.SKY, getBlockPos().up()) < 8 || isSubmergedInWater());
            clientTickHook.accept(this);
        } else {
            serverTick();
        }
    }

    /** Automatic gearbox: rpm climbs through each gear, drops when it shifts up. Drives the engine sound and tachometer. */
    private void updateGearbox() {
        float frac = Math.abs(clientSpeed) / Math.max(0.05f, type.maxSpeed);
        boolean running = engineRunning();
        float target;
        if (type.gears <= 0) {
            gear = 1;
            target = running ? 0.2f + Math.min(1f, frac) * 0.8f : 0f;
        } else if (clientSpeed < -0.02f) {
            gear = -1;
            target = 0.25f + Math.min(1f, frac / 0.35f) * 0.6f;
        } else {
            if (gear < 1) gear = 1;
            int n = type.gears;
            if (gear < n && frac / top(gear, n) > 0.95f) gear++;
            else if (gear > 1 && frac / top(gear - 1, n) < 0.62f) gear--;
            float within = MathHelper.clamp(frac / top(gear, n), 0f, 1.05f);
            target = running ? 0.22f + within * 0.78f : 0f;
            if (running && accelVis > 0.004f) target = Math.max(target, 0.3f);
        }
        rpm += (target - rpm) * 0.35f;
    }

    private static float top(int g, int n) {
        return (float) Math.pow(g / (double) n, 0.75) * 1.02f;
    }

    /** Suspension: body follows the ground under the axles, squats when accelerating, dives when braking, rolls in turns. */
    private void updateBody() {
        if (!type.wheeled()) {
            bodyPitch *= 0.8f;
            bodyRoll *= 0.8f;
            return;
        }
        float pitchT = MathHelper.clamp(-accelVis * 70f, -4f, 4f);
        float rollT = 0;
        double af = type.axleF(), ar = type.axleR();
        double hf = surfaceY(0, af), hr = surfaceY(0, ar);
        pitchT += (float) -Math.toDegrees(Math.atan2(hf - hr, af - ar));
        if (type.category != Category.BIKE) {
            double track = type.width * 0.42;
            double hl = (surfaceY(track, af * 0.5) + surfaceY(track, ar * 0.5)) / 2;
            double hrt = (surfaceY(-track, af * 0.5) + surfaceY(-track, ar * 0.5)) / 2;
            rollT += (float) Math.toDegrees(Math.atan2(hl - hrt, track * 2));
            rollT += steerVisual * Math.min(1f, Math.abs(clientSpeed) / 0.6f) * 3.5f;
        }
        pitchT = MathHelper.clamp(pitchT, -22f, 22f);
        rollT = MathHelper.clamp(rollT, -18f, 18f);
        // springy, slightly under-damped
        pitchVel = (pitchVel + (pitchT - bodyPitch) * 0.3f) * 0.62f;
        rollVel = (rollVel + (rollT - bodyRoll) * 0.3f) * 0.62f;
        bodyPitch += pitchVel;
        bodyRoll += rollVel;
    }

    /** top of the ground at a point beside/ahead of the vehicle (local x = left, z = forward) */
    private double surfaceY(double lx, double lz) {
        Vec3d p = getPos().add(rotate(lx, 0, lz, getYaw(), 0));
        int top = MathHelper.floor(getY() + 1.05);
        for (int y = top; y >= top - 3; y--) {
            BlockPos bp = BlockPos.ofFloored(p.x, y, p.z);
            VoxelShape shape = getWorld().getBlockState(bp).getCollisionShape(getWorld(), bp);
            if (!shape.isEmpty()) {
                double h = y + shape.getMax(Direction.Axis.Y);
                if (h <= getY() + 1.1) return h;
            }
        }
        return getY() - 1.5;
    }

    private void serverTick() {
        if (type.interiorRoom && age % 20 == 0) InteriorManager.track(this);
        if (sprayOn()) Weapons.sprayTick(this);
        if (sirenOn() && age % 34 == 0) {
            getWorld().playSound(null, getX(), getY(), getZ(), ModRegistry.sound("siren"), SoundCategory.NEUTRAL, 3f, 1f);
        }
        burnFuel();
        hitMobs();
        if (type.category == Category.SUBMARINE && age % 20 == 0) {
            for (Entity p : getPassengerList()) {
                if (p instanceof LivingEntity le && le.isSubmergedInWater()) {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 80, 0, true, false, true));
                }
            }
        }
        // wrecks burn
        if (getHealth() < type.health * 0.12f && age % 40 == 0 && getWorld() instanceof ServerWorld w) {
            w.spawnParticles(ParticleTypes.LAVA, getX(), getY() + type.height * 0.6, getZ(), 2, 0.4, 0.2, 0.4, 0);
        }
    }

    private void burnFuel() {
        if (!type.usesFuel()) return;
        LivingEntity driver = getControllingPassenger();
        if (driver == null || getFuel() <= 0) return;
        if (driver instanceof PlayerEntity p && p.getAbilities().creativeMode) return;
        if (!getWorld().getGameRules().getBoolean(VehicleForge.FUEL_RULE)) return;
        float load = Math.min(1f, measuredSpeed / Math.max(0.05f, type.maxSpeed));
        if (type.category == Category.HELICOPTER) load = Math.max(load, isOnGround() ? 0.1f : 0.6f);
        if (type.category == Category.PLANE) load = Math.max(load, throttle);
        if (type.category == Category.ROCKET) load = thrusting ? 1f : 0.02f;
        float before = getFuel();
        setFuel(before - (0.1f + 0.9f * load) / (type.endurance * 1200f));
        if (getFuel() <= 0 && before > 0 && driver instanceof PlayerEntity p) {
            p.sendMessage(Text.translatable("msg.vehicleforge.out_of_fuel").formatted(Formatting.RED), true);
        } else if (before > 0.1f && getFuel() <= 0.1f && driver instanceof PlayerEntity p) {
            p.sendMessage(Text.translatable("msg.vehicleforge.low_fuel").formatted(Formatting.GOLD), true);
        }
    }

    /** Running people and mobs over. Heavier vehicles hit harder. */
    private void hitMobs() {
        if (measuredSpeed < 0.3f || type.flies() && !isOnGround()) return;
        boolean ahead = clientSpeed >= 0 || type.category == Category.TRAIN;
        Vec3d dir = motionDir.lengthSquared() > 0 ? motionDir : forward();
        Vec3d front = toWorld(0, 0, ahead ? type.frontZ : type.rearZ);
        Vec3d mid = toWorld(0, 0, (ahead ? type.frontZ : type.rearZ) * 0.35);
        double hw = type.width / 2 + 0.25;
        Box box = new Box(front, mid).expand(hw, 0, hw).stretch(0, type.height, 0);
        LivingEntity driver = getControllingPassenger();
        DamageSource src = driver instanceof PlayerEntity p ? getDamageSources().playerAttack(p)
                : driver != null ? getDamageSources().mobAttack(driver) : getDamageSources().generic();
        float mass = MathHelper.clamp(type.health / 60f, 0.6f, 2.5f);
        for (LivingEntity e : getWorld().getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e.getRootVehicle() != this)) {
            if (e.timeUntilRegen > 10) continue;
            if (e.damage(src, measuredSpeed * 16f * mass)) {
                e.addVelocity(dir.x * measuredSpeed * 1.4, 0.3 + measuredSpeed * 0.25, dir.z * measuredSpeed * 1.4);
                e.velocityModified = true;
                serverWorldSound("crash", 0.6f, 1.4f);
            }
        }
    }

    // ------------------------------------------------------------ physics

    private void physics() {
        boolean driven = getControllingPassenger() != null && getWorld().isClient;
        boolean fwd = driven && InputState.forward, back = driven && InputState.back;
        boolean left = driven && InputState.left, right = driven && InputState.right;
        boolean up = driven && InputState.up, down = driven && InputState.down;
        boolean engine = driven && hasFuel();
        float steer = left ? 1 : right ? -1 : 0;
        float pedal = fwd ? 1 : back ? -1 : 0;
        // dead engine: you can still brake, but not accelerate
        if (!engine && (pedal > 0 || speed <= 0.05f)) pedal = 0;
        if (!driven) {
            pedal = 0;
            steer = 0;
        }
        yawDelta = 0;
        switch (type.category) {
            case HELICOPTER -> helicopter(driven, engine, pedal, steer, up, down);
            case PLANE -> plane(driven, engine, pedal, steer);
            case ROCKET -> rocket(engine, pedal, steer, up);
            case BOAT -> boat(pedal, steer);
            case SUBMARINE -> submarine(driven, engine, pedal, steer, up, down);
            case TRAIN -> train(pedal);
            default -> ground(driven, pedal, steer, up, engine);
        }
    }

    /** damaged engines lose power */
    private float healthMul() {
        float hp = getHealth() / type.health;
        return hp < 0.25f ? 0.6f : hp < 0.5f ? 0.85f : 1f;
    }

    private void accelerate(float pedal, float max) {
        if (pedal > 0) {
            if (speed < -0.02f) speed += type.brake;                    // braking out of reverse
            else speed += type.accel * Math.max(0.05f, 1 - speed / max) * healthMul();
        } else if (pedal < 0) {
            speed -= speed > 0.05f ? type.brake : type.accel * 0.6f;
        } else {
            speed *= 0.982f;                                             // rolling resistance + engine braking
        }
        if (speed > max) speed += (max - speed) * 0.1f;                 // over the limit (e.g. onto grass): bleed off
        speed = MathHelper.clamp(speed, -type.maxSpeed * 0.35f, type.maxSpeed * 1.3f);
    }

    private void steer(float steer) {
        float grip = MathHelper.clamp(Math.abs(speed) / 0.25f, 0, 1);
        yawDelta = -steer * type.turn * grip * Math.signum(speed);
        setYaw(getYaw() + yawDelta);
    }

    private enum Surface { ROAD, SOFT, MUD, ICE }

    private Surface surface() {
        BlockState st = getWorld().getBlockState(BlockPos.ofFloored(getX(), getY() - 0.2, getZ()));
        if (st.isIn(BlockTags.ICE)) return Surface.ICE;
        if (st.isOf(Blocks.MUD) || st.isOf(Blocks.SOUL_SAND) || st.isOf(Blocks.SOUL_SOIL)) return Surface.MUD;
        if (st.isIn(BlockTags.DIRT) || st.isIn(BlockTags.SAND) || st.isIn(BlockTags.SNOW) || st.isOf(Blocks.GRAVEL)
                || st.isOf(Blocks.FARMLAND) || st.isOf(Blocks.CLAY) || st.isOf(Blocks.DIRT_PATH)) return Surface.SOFT;
        return Surface.ROAD;
    }

    /**
     * Cars, trucks, bikes, tanks. The velocity keeps its momentum between ticks and the tyres only cancel part of
     * the sideways slide each tick: normally almost all of it, on ice or with the handbrake (Space) hardly any,
     * so you can drift.
     */
    private void ground(boolean driven, float pedal, float steerIn, boolean handbrake, boolean engine) {
        setNoGravity(false);
        Surface surf = surface();
        boolean onGround = isOnGround();
        float max = type.maxSpeed;
        float grip = 0.8f;
        switch (surf) {
            case SOFT -> {
                max *= type.offroad ? 0.92f : 0.6f;
                grip = 0.55f;
            }
            case MUD -> {
                max *= type.offroad ? 0.7f : 0.35f;
                grip = 0.45f;
            }
            case ICE -> grip = 0.05f;
            default -> { }
        }
        if (type.category == Category.MILITARY && type.weapon == VehicleType.Weapon.TANK_CANNON) grip = Math.max(grip, 0.7f); // tracks
        boolean wet = isTouchingWater();
        boolean floating = type.amphibious && wet && waterAt(getY() + 0.6);
        if (floating) {
            max = type.maxSpeed * 0.4f;
            grip = 0.2f;
        }
        // momentum from last tick, in the vehicle's own frame
        Vec3d vel = getVelocity();
        Vec3d f = forward(), left = leftVec();
        double lat = vel.x * left.x + vel.z * left.z;
        if (onGround || floating) speed = (float) (vel.x * f.x + vel.z * f.z);

        boolean drift = handbrake && Math.abs(speed) > 0.3f;
        if (onGround || floating) {
            float slope = driven ? slopeDeg() : 0;
            if (surf == Surface.ICE) pedal *= 0.5f;
            accelerate(pedal, max);
            speed -= (float) Math.sin(Math.toRadians(slope)) * 0.03f;   // hills slow you down / roll you back
            if (handbrake) speed *= drift ? 0.975f : 0.85f;
            if (!driven) speed *= 0.8f;                                  // parked: handbrake on
            if (drift) grip = Math.min(grip, 0.1f);
        }
        if (wet && !floating) {
            speed *= 0.9f;
            if (waterAt(getY() + 1.2) && type.usesFuel() && !type.amphibious) speed *= 0.5f;   // flooded engine
        }

        // steering: smoothed input, less lock at high speed, handbrake swings the tail
        steerInput += (steerIn - steerInput) * 0.3f;
        float low = MathHelper.clamp(Math.abs(speed) / 0.25f, 0, 1);
        float high = 1f - 0.4f * Math.min(1f, Math.abs(speed) / Math.max(0.3f, type.maxSpeed));
        float tyreGrip = onGround || floating ? 1f : 0.15f;
        yawDelta = -steerInput * type.turn * low * high * tyreGrip * (drift ? 1.7f : 1f) * Math.signum(speed);
        setYaw(getYaw() + yawDelta);

        f = forward();
        left = leftVec();
        if (onGround || floating) lat *= (1 - grip);
        double vy;
        if (floating) {
            vy = vel.y;
            if (waterAt(getY() + 1.0)) vy += 0.05;
            else vy *= 0.5;
            vy *= 0.85;
        } else {
            vy = onGround ? -0.04 : vel.y - 0.08;
            vy *= 0.98;
            if (wet) vy = Math.max(vy, -0.08);
        }
        Vec3d v = new Vec3d(f.x * speed + left.x * lat, vy, f.z * speed + left.z * lat);
        double before = v.horizontalLength();
        setVelocity(v);
        move(MovementType.SELF, v);
        if (horizontalCollision) {
            double after = getVelocity().horizontalLength();
            reportCrash((float) (before - after));
            speed *= 0.6f;
        }
        landing(onGround, 0.9f);
        setPitch(0);
    }

    /** hard landings after a jump or a fall */
    private void landing(boolean wasGround, float limit) {
        if (isOnGround() && !wasOnGround && lastFallVy < -limit) reportCrash((float) (-lastFallVy - limit + 0.5f));
        wasOnGround = isOnGround();
        lastFallVy = getVelocity().y;
    }

    /** hill angle under the vehicle in degrees: positive = driving uphill */
    private float slopeDeg() {
        double af = type.axleF(), ar = type.axleR();
        double hf = surfaceY(0, af), hr = surfaceY(0, ar);
        if (Math.abs(hf - hr) > 1.6) return 0;
        float deg = (float) Math.toDegrees(Math.atan2(hf - hr, af - ar));
        return speed >= 0 ? deg : deg;
    }

    private boolean waterAt(double y) {
        return getWorld().getFluidState(BlockPos.ofFloored(getX(), y, getZ())).isIn(FluidTags.WATER);
    }

    private void boat(float pedal, float steer) {
        setNoGravity(false);
        boolean floating = waterAt(getY() + 0.2) || waterAt(getY() - 0.3);
        Vec3d vel = getVelocity();
        double lat = vel.x * leftVec().x + vel.z * leftVec().z;
        accelerate(pedal, floating ? type.maxSpeed : 0.05f);
        steerInput += (steer - steerInput) * 0.25f;
        steer(steerInput);
        double vy = vel.y;
        if (waterAt(getY() + 0.35)) vy += 0.045;
        else if (waterAt(getY() - 0.2)) vy *= 0.5;
        else vy -= 0.06;
        vy *= 0.88;
        lat *= floating ? 0.88 : 0.5;                       // boats slide through turns
        Vec3d f = forward(), l = leftVec();
        setVelocity(f.x * speed + l.x * lat, vy, f.z * speed + l.z * lat);
        double before = getVelocity().horizontalLength();
        move(MovementType.SELF, getVelocity());
        if (horizontalCollision) {
            reportCrash((float) (before - getVelocity().horizontalLength()));
            speed *= 0.5f;
        }
        // bow rises with speed, bounces a little on chop
        float bob = floating ? (float) Math.sin(age * 0.25) * Math.min(1f, Math.abs(speed)) * 1.2f : 0;
        setPitch(MathHelper.lerp(0.1f, getPitch(), -Math.max(0, speed) * 5f + bob));
    }

    /** Submarine: W/S throttle, A/D turn, Space rises, the down key dives. Riders can breathe inside. */
    private void submarine(boolean driven, boolean engine, float pedal, float steer, boolean up, boolean down) {
        setNoGravity(false);
        boolean inWater = waterAt(getY() + 0.6);
        accelerate(pedal, inWater ? type.maxSpeed : 0.03f);
        steerInput += (steer - steerInput) * 0.2f;
        steer(steerInput);
        double vy = getVelocity().y;
        if (inWater) {
            double target = 0;
            if (engine && up) target = 0.12;
            else if (engine && down) target = -0.12;
            else if (!engine) target = 0.04;                     // ballast blown: floats up
            boolean surfaced = !waterAt(getY() + 2.0);
            if (surfaced && target > 0) target = 0;              // sail out of the water, hull just under
            vy += (target - vy) * 0.15;
        } else {
            vy -= 0.06;
        }
        vy *= 0.95;
        Vec3d f = forward();
        setVelocity(f.x * speed, vy, f.z * speed);
        double before = Math.abs(speed);
        move(MovementType.SELF, getVelocity());
        if (horizontalCollision) {
            reportCrash((float) before);
            speed *= 0.4f;
        }
        setPitch(MathHelper.lerp(0.08f, getPitch(), (float) MathHelper.clamp(-vy * 60, -12, 12)));
    }

    private void helicopter(boolean driven, boolean engine, float pedal, float steer, boolean up, boolean down) {
        if (!driven || !engine) {
            // no pilot / no fuel: autorotation, a fast but survivable descent
            setNoGravity(false);
            speed *= 0.95f;
            Vec3d f = forward();
            double vy = getVelocity().y - 0.08;
            if (driven) vy = Math.max(vy, -0.35);
            setVelocity(f.x * speed, vy, f.z * speed);
            boolean wasGround = isOnGround();
            move(MovementType.SELF, getVelocity());
            landing(wasGround, 0.6f);
            setPitch(MathHelper.lerp(0.1f, getPitch(), 0));
            return;
        }
        setNoGravity(true);
        float target = pedal * (pedal > 0 ? type.maxSpeed : type.maxSpeed * 0.4f);
        speed += (target - speed) * 0.04f * healthMul();
        steerInput += (steer - steerInput) * 0.2f;
        yawDelta = -steerInput * type.turn;
        setYaw(getYaw() + yawDelta);
        double vy = getVelocity().y;
        if (up) vy += 0.05;
        else if (down) vy -= 0.06;
        else vy += (-0.015 - vy) * 0.1;
        if (isOnGround() && !up) {
            vy = Math.max(vy, -0.05);
            speed *= 0.8f;
        }
        vy = MathHelper.clamp(vy * 0.95, -0.6, 0.6);
        Vec3d f = forward();
        setVelocity(f.x * speed, vy, f.z * speed);
        double before = getVelocity().length();
        boolean wasGround = isOnGround();
        move(MovementType.SELF, getVelocity());
        if (horizontalCollision) {
            reportCrash((float) (before - getVelocity().horizontalLength()));
            speed *= 0.4f;
        }
        landing(wasGround, 0.5f);
        setPitch(MathHelper.lerp(0.1f, getPitch(), speed * 12f));
    }

    private void plane(boolean driven, boolean engine, float pedal, float steer) {
        throttle = MathHelper.clamp(throttle + pedal * 0.02f, 0f, 1f);
        if (!driven || !engine) throttle = Math.max(0, throttle - 0.02f);
        float target = throttle * type.maxSpeed * healthMul();
        boolean onGround = isOnGround();
        float takeoff = type.maxSpeed * 0.38f;
        // in the air with no thrust, a plane glides: it trades height for speed
        if (!onGround && throttle <= 0.01f) target = Math.max(speed, takeoff * 1.05f) * 0.995f;
        speed += MathHelper.clamp(target - speed, -0.02f, type.accel);
        if (onGround && pedal < 0 && throttle <= 0) speed *= 0.9f;      // wheel brakes
        steerInput += (steer - steerInput) * 0.2f;
        yawDelta = -steerInput * type.turn * (onGround ? 0.6f : 1f) * MathHelper.clamp(speed / 0.2f, 0, 1);
        setYaw(getYaw() + yawDelta);

        LivingEntity pilot = getControllingPassenger();
        float desired = pilot != null ? MathHelper.clamp(pilot.getPitch(), -45, 45) : 15;
        if (!engine && !onGround) desired = Math.max(desired, 6f);       // engine out: nose down to keep flying
        float pitch = getPitch();
        if (speed > takeoff) {
            setNoGravity(true);
            pitch += (desired - pitch) * 0.08f;
        } else {
            setNoGravity(false);
            pitch += ((onGround ? 0 : 12) - pitch) * 0.05f;
        }
        if (onGround && pitch > 0) pitch = 0;
        setPitch(pitch);
        Vec3d dir = Vec3d.fromPolar(pitch, getYaw());
        Vec3d v = dir.multiply(speed);
        if (speed <= takeoff) {
            double vy = onGround ? -0.04 : getVelocity().y - 0.08;
            v = new Vec3d(v.x, Math.min(v.y, 0) + vy, v.z);
        }
        setVelocity(v);
        double fallVy = v.y;
        move(MovementType.SELF, v);
        if (horizontalCollision) {
            if (speed > 0.6f) reportCrash(speed);
            speed *= 0.3f;
        }
        if (verticalCollision && !onGround && fallVy < -0.7) reportCrash((float) -fallVy);
    }

    private void rocket(boolean engine, float pedal, float steer, boolean up) {
        setNoGravity(true);
        double vy = getVelocity().y;
        if (engine && up) vy += 0.05;
        else vy -= 0.04;
        vy = MathHelper.clamp(vy * 0.995, -2.5, type.maxSpeed);
        float tilt = getPitch() + pedal * 1.2f;
        if (isOnGround()) tilt *= 0.8f;
        tilt = MathHelper.clamp(tilt, -35, 35);
        setPitch(tilt);
        yawDelta = -steer * type.turn;
        setYaw(getYaw() + yawDelta);
        Vec3d f = forward();
        double side = Math.sin(Math.toRadians(tilt)) * Math.max(0, vy) * 0.9;
        if (isOnGround() && vy <= 0) {
            vy = Math.max(vy, -0.04);
            side = 0;
        }
        setVelocity(f.x * side, vy, f.z * side);
        prevVy = (float) vy;
        move(MovementType.SELF, getVelocity());
        if (verticalCollision && prevVy < -1.3) reportCrash(-prevVy);
    }

    // ------------------------------------------------------------- trains

    private static EnumSet<Direction> exits(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> EnumSet.of(Direction.NORTH, Direction.SOUTH);
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> EnumSet.of(Direction.EAST, Direction.WEST);
            case SOUTH_EAST -> EnumSet.of(Direction.SOUTH, Direction.EAST);
            case SOUTH_WEST -> EnumSet.of(Direction.SOUTH, Direction.WEST);
            case NORTH_WEST -> EnumSet.of(Direction.NORTH, Direction.WEST);
            case NORTH_EAST -> EnumSet.of(Direction.NORTH, Direction.EAST);
        };
    }

    @Nullable
    private BlockPos findRail(Vec3d p) {
        BlockPos at = BlockPos.ofFloored(p.x, p.y + 0.1, p.z);
        for (BlockPos c : new BlockPos[]{at, at.down(), at.up()}) {
            if (getWorld().getBlockState(c).getBlock() instanceof AbstractRailBlock) return c;
        }
        return null;
    }

    private RailShape shapeAt(BlockPos rp) {
        BlockState st = getWorld().getBlockState(rp);
        AbstractRailBlock rail = (AbstractRailBlock) st.getBlock();
        return st.get(rail.getShapeProperty());
    }

    private void train(float pedal) {
        setNoGravity(false);
        accelerate(pedal, type.maxSpeed);
        if (pedal == 0) speed *= 1.012f;                                   // trains coast a long way
        if (!hasPassengers()) speed *= 0.9f;
        // move in short steps so fast trains never skip a curve
        Vec3d start = getPos();
        double left = Math.abs(speed);
        int guard = 0;
        do {
            double step = Math.min(0.45, left);
            if (!trainStep(step)) break;
            left -= step;
        } while (left > 1.0E-4 && ++guard < 12);
        setVelocity(getPos().subtract(start));
    }

    /** returns false when the train can't continue (end of line / off the rails) */
    private boolean trainStep(double dist) {
        BlockPos rp = findRail(getPos());
        if (rp == null) {
            speed *= 0.8f;
            setVelocity(getVelocity().multiply(0.5, 1, 0.5).add(0, -0.08, 0));
            move(MovementType.SELF, getVelocity());
            return false;
        }
        RailShape shape = shapeAt(rp);
        EnumSet<Direction> ex = exits(shape);
        if (railDir == null || !railDir.getAxis().isHorizontal()) railDir = Direction.fromRotation(getYaw());
        Direction moveDir = speed >= 0 ? railDir : railDir.getOpposite();
        if (!ex.contains(moveDir)) {
            Direction from = moveDir.getOpposite();
            Direction next = null;
            for (Direction d : ex) if (d != from) next = d;
            if (next == null || !ex.contains(from)) {
                // derailed onto a rail at an odd angle: pick the closest exit
                next = ex.iterator().next();
            }
            moveDir = next;
            railDir = speed >= 0 ? moveDir : moveDir.getOpposite();
            setPosition(rp.getX() + 0.5, getY(), rp.getZ() + 0.5);
        }
        double nx = getX() + moveDir.getOffsetX() * dist;
        double nz = getZ() + moveDir.getOffsetZ() * dist;
        if (moveDir.getAxis() == Direction.Axis.X) nz = rp.getZ() + 0.5;
        else nx = rp.getX() + 0.5;
        BlockPos nrp = findRail(new Vec3d(nx, getY(), nz));
        if (nrp == null) {
            // end of the line
            if (Math.abs(speed) > 0.8f) reportCrash(Math.abs(speed) * 0.6f);
            speed = 0;
            return false;
        }
        double ny = nrp.getY();
        RailShape ns = shapeAt(nrp);
        if (ns.isAscending()) {
            Direction upDir = switch (ns) {
                case ASCENDING_EAST -> Direction.EAST;
                case ASCENDING_WEST -> Direction.WEST;
                case ASCENDING_NORTH -> Direction.NORTH;
                default -> Direction.SOUTH;
            };
            double fx = nx - nrp.getX(), fz = nz - nrp.getZ();
            double frac = switch (upDir) {
                case EAST -> fx;
                case WEST -> 1 - fx;
                case SOUTH -> fz;
                default -> 1 - fz;
            };
            ny += MathHelper.clamp(frac, 0, 1);
        }
        setPosition(nx, ny, nz);
        float targetYaw = railDir.asRotation();
        setYaw(getYaw() + MathHelper.wrapDegrees(targetYaw - getYaw()) * 0.35f);
        return true;
    }

    private void recordPath() {
        Vec3d p = getPos();
        if (path.isEmpty() || path.peekFirst().squaredDistanceTo(p) > 0.09) {
            path.addFirst(p);
            while (path.size() > 600) path.removeLast();
        }
    }

    public record CarriagePose(Vec3d pos, float yaw) {}

    /** Where carriage k (1 = first behind the locomotive) sits, following the exact track the train drove. */
    public CarriagePose carriagePose(int k) {
        double want = type.carriageStart + (k - 1) * 6.3;
        double walked = 0;
        Vec3d prev = getPos();
        Iterator<Vec3d> it = path.iterator();
        List<Vec3d> pts = new ArrayList<>();
        pts.add(prev);
        while (it.hasNext()) {
            Vec3d p = it.next();
            double d = p.distanceTo(prev);
            if (walked + d >= want && d > 1.0E-6) {
                Vec3d at = prev.lerp(p, (want - walked) / d);
                Vec3d dir = prev.subtract(p);
                float yaw = (float) (MathHelper.atan2(dir.z, dir.x) * (180 / Math.PI)) - 90f;
                return new CarriagePose(at, yaw);
            }
            walked += d;
            prev = p;
        }
        Vec3d f = forward();
        Vec3d at = getPos().subtract(f.multiply(want));
        return new CarriagePose(at, getYaw());
    }

    // ---------------------------------------------------------- remote lerp

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps, boolean interpolate) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yaw;
        lerpPitch = pitch;
        lerpTicks = Math.max(1, steps + 1);
    }

    private void lerpStep() {
        if (lerpTicks <= 0) return;
        double x = getX() + (lerpX - getX()) / lerpTicks;
        double y = getY() + (lerpY - getY()) / lerpTicks;
        double z = getZ() + (lerpZ - getZ()) / lerpTicks;
        float yaw = getYaw() + MathHelper.wrapDegrees(lerpYaw - getYaw()) / lerpTicks;
        float pitch = getPitch() + (lerpPitch - getPitch()) / lerpTicks;
        lerpTicks--;
        setPosition(x, y, z);
        setRotation(yaw, pitch);
    }

    /** current speed in blocks per tick (any side) */
    public float displaySpeed() {
        return Math.abs(clientSpeed);
    }

    public void serverWorldSound(String id, float volume, float pitch) {
        if (getWorld() instanceof ServerWorld w) {
            w.playSound(null, getX(), getY(), getZ(), ModRegistry.sound(id), SoundCategory.NEUTRAL, volume, pitch);
        }
    }
}
