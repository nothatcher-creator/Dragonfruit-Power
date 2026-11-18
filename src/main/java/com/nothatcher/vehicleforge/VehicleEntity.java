package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleType.Category;
import com.nothatcher.vehicleforge.interior.InteriorManager;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
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

    /** Client hook (engine sounds etc.), set by the client mod. */
    public static Consumer<VehicleEntity> clientTickHook = e -> { };

    public final VehicleType type;

    // driving state (on the logical side)
    public float speed, throttle;
    private float yawDelta;
    private Direction railDir;
    private float prevVy;

    // visual state (client)
    public float steerVisual, prevSteerVisual, roll, prevRoll;
    public float wheelSpin, prevWheelSpin, rotorSpin, prevRotorSpin;
    public float clientSpeed;
    public boolean thrusting;
    public Object engineSound;

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
        interiorId = nbt.contains("Interior") ? nbt.getInt("Interior") : -1;
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Paint", dataTracker.get(PAINT));
        nbt.putFloat("Health", dataTracker.get(HEALTH));
        nbt.putInt("Carriages", dataTracker.get(CARRIAGES));
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
        if (h <= 0) {
            if (!creative && source.getAttacker() instanceof PlayerEntity) {
                dropStack(new ItemStack(ModRegistry.ITEMS.get(type.id)));
            } else if (!creative) {
                // destroyed by explosions/weapons: wreck goes up
                getWorld().createExplosion(this, getX(), getY() + 0.5, getZ(), 2.5f, World.ExplosionSourceType.NONE);
            }
            removeAllPassengers();
            discard();
        }
        return true;
    }

    // --------------------------------------------------------------- tick

    @Override
    public void tick() {
        prevSteerVisual = steerVisual;
        prevRoll = roll;
        prevWheelSpin = wheelSpin;
        prevRotorSpin = rotorSpin;
        double ox = getX(), oy = getY(), oz = getZ();
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

        // visuals derived from actual motion, so every client sees the same thing
        double dx = getX() - ox, dz = getZ() - oz, dy = getY() - oy;
        double moved = Math.sqrt(dx * dx + dz * dz);
        Vec3d f = forward();
        double signed = dx * f.x + dz * f.z;
        clientSpeed = (float) (Math.signum(signed) * moved);
        wheelSpin += (float) (signed / 0.4);
        float yawRate = MathHelper.wrapDegrees(getYaw() - oyaw);
        float steerTarget = type.turn > 0 ? MathHelper.clamp(-yawRate / Math.max(0.5f, type.turn), -1, 1) : 0;
        steerVisual += (steerTarget - steerVisual) * 0.3f;
        float rollTarget = switch (type.category) {
            case BIKE -> steerVisual * 25f * Math.min(1f, Math.abs(clientSpeed) / 0.5f);
            case PLANE -> steerVisual * 35f;
            case HELICOPTER -> steerVisual * 12f;
            default -> 0f;
        };
        roll += (rollTarget - roll) * 0.15f;
        boolean powered = hasPassengers() || moved > 0.02;
        if (type.category == Category.HELICOPTER || type.category == Category.PLANE) {
            rotorSpin += powered ? (type.category == Category.HELICOPTER ? 55f : 70f) : 0f;
        }
        thrusting = type.category == Category.ROCKET ? dy > 0.04 : (type.category == Category.PLANE && moved > 0.4);

        if (getWorld().isClient) clientTickHook.accept(this);
        else serverTick();
    }

    private void serverTick() {
        if (type.interiorRoom && age % 20 == 0) InteriorManager.track(this);
        if (sprayOn()) Weapons.sprayTick(this);
        if (sirenOn() && age % 34 == 0) {
            getWorld().playSound(null, getX(), getY(), getZ(), ModRegistry.sound("siren"), SoundCategory.NEUTRAL, 3f, 1f);
        }
    }

    // ------------------------------------------------------------ physics

    private void physics() {
        boolean driven = getControllingPassenger() != null && getWorld().isClient;
        boolean fwd = driven && InputState.forward, back = driven && InputState.back;
        boolean left = driven && InputState.left, right = driven && InputState.right;
        boolean up = driven && InputState.up, down = driven && InputState.down;
        float steer = left ? 1 : right ? -1 : 0;
        float pedal = fwd ? 1 : back ? -1 : 0;
        yawDelta = 0;
        switch (type.category) {
            case HELICOPTER -> helicopter(driven, pedal, steer, up, down);
            case PLANE -> plane(driven, pedal, steer);
            case ROCKET -> rocket(driven, pedal, steer, up);
            case BOAT -> boat(pedal, steer);
            case TRAIN -> train(pedal);
            default -> ground(pedal, steer, up);
        }
    }

    private void accelerate(float pedal, float max) {
        if (pedal > 0) speed += type.accel * Math.max(0.05f, 1 - speed / max);
        else if (pedal < 0) speed -= speed > 0.05f ? type.brake : type.accel * 0.6f;
        else speed *= 0.975f;
        speed = MathHelper.clamp(speed, -max * 0.35f, max);
    }

    private void steer(float steer) {
        float grip = MathHelper.clamp(Math.abs(speed) / 0.25f, 0, 1);
        yawDelta = -steer * type.turn * grip * Math.signum(speed);
        setYaw(getYaw() + yawDelta);
    }

    private void ground(float pedal, float steer, boolean handbrake) {
        setNoGravity(false);
        accelerate(pedal, type.maxSpeed);
        if (handbrake) speed *= 0.86f;
        boolean wet = isTouchingWater();
        if (wet) speed *= 0.9f;
        steer(steer);
        Vec3d f = forward();
        double vy = isOnGround() ? -0.04 : getVelocity().y - 0.08;
        vy *= 0.98;
        if (wet) vy = Math.max(vy, -0.08);
        setVelocity(f.x * speed, vy, f.z * speed);
        move(MovementType.SELF, getVelocity());
        if (horizontalCollision) speed *= 0.6f;
        setPitch(0);
    }

    private boolean waterAt(double y) {
        return getWorld().getFluidState(BlockPos.ofFloored(getX(), y, getZ())).isIn(FluidTags.WATER);
    }

    private void boat(float pedal, float steer) {
        setNoGravity(false);
        boolean floating = waterAt(getY() + 0.2) || waterAt(getY() - 0.3);
        accelerate(pedal, floating ? type.maxSpeed : 0.05f);
        steer(steer);
        double vy = getVelocity().y;
        if (waterAt(getY() + 0.35)) vy += 0.045;
        else if (waterAt(getY() - 0.2)) vy *= 0.5;
        else vy -= 0.06;
        vy *= 0.88;
        Vec3d f = forward();
        setVelocity(f.x * speed, vy, f.z * speed);
        move(MovementType.SELF, getVelocity());
        if (horizontalCollision) speed *= 0.5f;
        setPitch(MathHelper.lerp(0.1f, getPitch(), -Math.max(0, speed) * 5f));
    }

    private void helicopter(boolean driven, float pedal, float steer, boolean up, boolean down) {
        if (!driven) {
            setNoGravity(false);
            speed *= 0.95f;
            Vec3d f = forward();
            setVelocity(f.x * speed, getVelocity().y - 0.08, f.z * speed);
            move(MovementType.SELF, getVelocity());
            setPitch(MathHelper.lerp(0.1f, getPitch(), 0));
            return;
        }
        setNoGravity(true);
        float target = pedal * (pedal > 0 ? type.maxSpeed : type.maxSpeed * 0.4f);
        speed += (target - speed) * 0.04f;
        yawDelta = -steer * type.turn;
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
        move(MovementType.SELF, getVelocity());
        if (horizontalCollision) speed *= 0.4f;
        setPitch(MathHelper.lerp(0.1f, getPitch(), speed * 12f));
    }

    private void plane(boolean driven, float pedal, float steer) {
        throttle = MathHelper.clamp(throttle + pedal * 0.02f, 0f, 1f);
        if (!driven) throttle = Math.max(0, throttle - 0.02f);
        float target = throttle * type.maxSpeed;
        speed += MathHelper.clamp(target - speed, -0.02f, type.accel);
        if (isOnGround() && pedal < 0 && throttle <= 0) speed *= 0.9f;
        float takeoff = type.maxSpeed * 0.38f;
        boolean onGround = isOnGround();
        yawDelta = -steer * type.turn * (onGround ? 0.6f : 1f) * MathHelper.clamp(speed / 0.2f, 0, 1);
        setYaw(getYaw() + yawDelta);

        LivingEntity pilot = getControllingPassenger();
        float desired = pilot != null ? MathHelper.clamp(pilot.getPitch(), -45, 45) : 15;
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
        move(MovementType.SELF, v);
        if (horizontalCollision) {
            if (speed > 0.9f && !getWorld().isClient) damage(getDamageSources().flyIntoWall(), speed * 30);
            speed *= 0.3f;
        }
    }

    private void rocket(boolean driven, float pedal, float steer, boolean up) {
        setNoGravity(true);
        double vy = getVelocity().y;
        if (driven && up) vy += 0.05;
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
        if (verticalCollision && prevVy < -1.3 && !getWorld().isClient) {
            damage(getDamageSources().flyIntoWall(), type.health * 0.6f);
        }
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
        if (pedal == 0) speed *= 0.995f;
        BlockPos rp = findRail(getPos());
        if (rp == null) {
            speed *= 0.8f;
            setVelocity(getVelocity().multiply(0.5, 1, 0.5).add(0, -0.08, 0));
            move(MovementType.SELF, getVelocity());
            return;
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
        double dist = Math.abs(speed);
        double nx = getX() + moveDir.getOffsetX() * dist;
        double nz = getZ() + moveDir.getOffsetZ() * dist;
        if (moveDir.getAxis() == Direction.Axis.X) nz = rp.getZ() + 0.5;
        else nx = rp.getX() + 0.5;
        BlockPos nrp = findRail(new Vec3d(nx, getY(), nz));
        if (nrp == null) {
            // end of the line
            speed = 0;
            return;
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
        Vec3d delta = new Vec3d(nx - getX(), ny - getY(), nz - getZ());
        setVelocity(delta);
        setPosition(nx, ny, nz);
        float targetYaw = railDir.asRotation();
        setYaw(getYaw() + MathHelper.wrapDegrees(targetYaw - getYaw()) * 0.5f);
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
        double want = 5.6 + (k - 1) * 6.3;
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
