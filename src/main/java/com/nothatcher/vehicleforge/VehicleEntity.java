package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleType.Category;
import com.nothatcher.vehicleforge.interior.InteriorManager;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
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
    /** special ability on/off (UFO beam, mower blades, forks up, time machine charged, rocket boost) */
    private static final TrackedData<Boolean> ABILITY = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /** shrink car: target size (1 = normal) */
    private static final TrackedData<Float> SCALE = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    /** a burnt-out wreck: won't start or drive until it's been fully repaired with iron ingots */
    private static final TrackedData<Boolean> WRECK = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /** build frame: the blocks on its deck */
    private static final TrackedData<NbtCompound> BUILT = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    /** fitted upgrades (Upgrades.ARMOR etc, a bit mask) and the hitched trailer (0 none, 1 cargo, 2 camper, 3 boat) */
    private static final TrackedData<Integer> UPGRADES = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TRAILER = DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);

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
    /** current (animated) size and forklift fork height */
    public float scaleNow = 1f, prevScale = 1f, forkHeight, prevForkHeight;
    /** rocket cart boost, run by the driver's client */
    public int boostTicks, boostCooldown;
    /** server: ticks left on a timed ability (boost flame) */
    public int abilityTicks;
    /** server: time machine trail, newest first: x, y, z, yaw every half second */
    public final ArrayDeque<double[]> timeHistory = new ArrayDeque<>();
    /** server: what this crane is holding or this tow truck is towing */
    public Entity carried;
    /** server: the crane or tow truck holding this vehicle (its own physics pause) */
    public VehicleEntity carriedBy;

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
        dataTracker.startTracking(ABILITY, false);
        dataTracker.startTracking(SCALE, 1f);
        dataTracker.startTracking(WRECK, false);
        dataTracker.startTracking(BUILT, new NbtCompound());
        dataTracker.startTracking(UPGRADES, 0);
        dataTracker.startTracking(TRAILER, 0);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (BUILT.equals(data) && type != null) setBoundingBox(calculateBoundingBox());
    }

    public boolean isWreck() {
        return dataTracker.get(WRECK);
    }

    public void setWreck(boolean w) {
        dataTracker.set(WRECK, w);
    }

    public boolean abilityOn() {
        return dataTracker.get(ABILITY);
    }

    public void setAbility(boolean on) {
        dataTracker.set(ABILITY, on);
    }

    public float getScaleTarget() {
        return dataTracker.get(SCALE);
    }

    public void setScaleTarget(float s) {
        dataTracker.set(SCALE, s);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return super.getDimensions(pose).scaled(scaleNow);
    }

    /** rocket cart: start a boost on the driver's client (the server shows the flames) */
    public boolean tryLocalBoost() {
        if (boostCooldown > 0) return false;
        boostTicks = 40;
        boostCooldown = 120;
        return true;
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
        return getControllingPassenger() != null && hasFuel() && !isWreck();
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
        if (nbt.contains("Scale")) {
            dataTracker.set(SCALE, nbt.getFloat("Scale"));
            scaleNow = prevScale = nbt.getFloat("Scale");
        }
        interiorId = nbt.contains("Interior") ? nbt.getInt("Interior") : -1;
        dataTracker.set(WRECK, nbt.getBoolean("Wreck"));
        if (nbt.contains("Built")) dataTracker.set(BUILT, nbt.getCompound("Built"));
        dataTracker.set(UPGRADES, nbt.getInt("Upgrades"));
        dataTracker.set(TRAILER, nbt.getInt("Trailer"));
        readCargo(nbt);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Paint", dataTracker.get(PAINT));
        nbt.putFloat("Health", dataTracker.get(HEALTH));
        nbt.putInt("Carriages", dataTracker.get(CARRIAGES));
        if (isWreck()) nbt.putBoolean("Wreck", true);
        nbt.putFloat("Fuel", dataTracker.get(FUEL));
        nbt.putFloat("Scale", dataTracker.get(SCALE));
        nbt.putInt("Interior", interiorId);
        if (type.buildable) nbt.put("Built", dataTracker.get(BUILT));
        nbt.putInt("Upgrades", dataTracker.get(UPGRADES));
        nbt.putInt("Trailer", dataTracker.get(TRAILER));
        writeCargo(nbt);
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

    /**
     * The hitbox covers the whole vehicle, bumper to bumper, turned with the vehicle (as the box around the rotated
     * footprint). Blocks, players, mobs and other vehicles all collide with the full length.
     */
    @Override
    protected Box calculateBoundingBox() {
        if (type == null) return super.calculateBoundingBox();
        return footprint(getX(), getY(), getZ(), getYaw());
    }

    public Box footprint(double x, double y, double z, float yaw) {
        float s = scaleNow > 0 ? scaleNow : 1f;
        double hw = type.width * 0.5 * s;
        double f = Math.max(type.frontZ * s, hw), r = Math.min(type.rearZ * s, -hw);
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        double rad = Math.toRadians(yaw), c = Math.cos(rad), sn = Math.sin(rad);
        for (int i = 0; i < 4; i++) {
            double lx = (i & 1) == 0 ? hw : -hw, lz = (i & 2) == 0 ? f : r;
            double wx = lx * c - lz * sn, wz = lx * sn + lz * c;
            minX = Math.min(minX, wx);
            maxX = Math.max(maxX, wx);
            minZ = Math.min(minZ, wz);
            maxZ = Math.max(maxZ, wz);
        }
        double top = type.height;
        if (type.buildable) for (Built b : built()) top = Math.max(top, DECK + b.y() + 1);
        return new Box(x + minX, y, z + minZ, x + maxX, y + top * s, z + maxZ);
    }

    // ------------------------------------------------------------- build frame

    /** the top of the build frame's deck; built blocks stand on it in a grid x -1..1, z -2..2, y 0..4 */
    public static final double DECK = 0.85;
    private static final int BUILD_X = 1, BUILD_Z = 2, BUILD_Y = 4;

    public record Built(int x, int y, int z, BlockState state) {}

    private NbtCompound builtTag;
    private List<Built> builtCache = List.of();

    public List<Built> built() {
        NbtCompound t = dataTracker.get(BUILT);
        if (t != builtTag) {
            builtTag = t;
            List<Built> out = new ArrayList<>();
            net.minecraft.nbt.NbtList list = t.getList("B", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                BlockState st = net.minecraft.nbt.NbtHelper.toBlockState(net.minecraft.registry.Registries.BLOCK.getReadOnlyWrapper(), c.getCompound("S"));
                if (!st.isAir()) out.add(new Built(c.getInt("X"), c.getInt("Y"), c.getInt("Z"), st));
            }
            builtCache = out;
        }
        return builtCache;
    }

    private void setBuilt(List<Built> list) {
        NbtCompound t = new NbtCompound();
        net.minecraft.nbt.NbtList l = new net.minecraft.nbt.NbtList();
        for (Built b : list) {
            NbtCompound c = new NbtCompound();
            c.putInt("X", b.x());
            c.putInt("Y", b.y());
            c.putInt("Z", b.z());
            c.put("S", net.minecraft.nbt.NbtHelper.fromBlockState(b.state()));
            l.add(c);
        }
        t.put("B", l);
        dataTracker.set(BUILT, t);
    }

    private Vec3d localOffset(Vec3d d) {
        double r = Math.toRadians(getYaw()), c = Math.cos(r), sn = Math.sin(r);
        return new Vec3d(d.x * c + d.z * sn, d.y, -d.x * sn + d.z * c);
    }

    /** what the player is pointing at on the frame: a built block (with the face they hit) or the bare deck */
    private record BuildHit(int x, int y, int z, int nx, int ny, int nz, boolean deck, double t) {}

    private BuildHit buildRay(PlayerEntity p) {
        Vec3d o = localOffset(p.getEyePos().subtract(getPos())), d = localOffset(p.getRotationVec(1f));
        double reach = 6;
        BuildHit best = null;
        for (Built b : built()) {
            double[] lo = {b.x() - 0.5, DECK + b.y(), b.z() - 0.5}, hi = {b.x() + 0.5, DECK + b.y() + 1, b.z() + 0.5};
            double[] oo = {o.x, o.y, o.z}, dd = {d.x, d.y, d.z};
            double tIn = 0, tOut = reach;
            int axis = -1;
            boolean miss = false;
            for (int a = 0; a < 3 && !miss; a++) {
                if (Math.abs(dd[a]) < 1e-9) {
                    if (oo[a] < lo[a] || oo[a] > hi[a]) miss = true;
                    continue;
                }
                double t0 = (lo[a] - oo[a]) / dd[a], t1 = (hi[a] - oo[a]) / dd[a];
                if (t0 > t1) {
                    double tmp = t0;
                    t0 = t1;
                    t1 = tmp;
                }
                if (t0 > tIn) {
                    tIn = t0;
                    axis = a;
                }
                tOut = Math.min(tOut, t1);
                if (tIn > tOut) miss = true;
            }
            if (miss || axis < 0 || (best != null && tIn >= best.t())) continue;
            int sgn = dd[axis] > 0 ? -1 : 1;
            best = new BuildHit(b.x(), b.y(), b.z(), axis == 0 ? sgn : 0, axis == 1 ? sgn : 0, axis == 2 ? sgn : 0, false, tIn);
        }
        if (d.y < -1e-6) {
            double t = (DECK - o.y) / d.y;
            if (t > 0 && t < reach && (best == null || t < best.t())) {
                double x = o.x + d.x * t, z = o.z + d.z * t;
                if (Math.abs(x) <= 1.5 && Math.abs(z) <= 2.5) best = new BuildHit((int) Math.round(x), -1, (int) Math.round(z), 0, 1, 0, true, t);
            }
        }
        return best;
    }

    /** the empty grid cell a block held by this player would go into, or null */
    private int[] placeCell(PlayerEntity p) {
        BuildHit h = buildRay(p);
        if (h == null) return null;
        int x = h.x() + h.nx(), y = h.y() + h.ny(), z = h.z() + h.nz();
        if (Math.abs(x) > BUILD_X || Math.abs(z) > BUILD_Z || y < 0 || y > BUILD_Y) return null;
        if (x == 0 && z == 2 && y <= 1) return null;                     // the driver sits here
        for (Built b : built()) if (b.x() == x && b.y() == y && b.z() == z) return null;
        return new int[]{x, y, z};
    }

    private ActionResult buildPlace(PlayerEntity player, ItemStack stack, net.minecraft.item.BlockItem bi) {
        int[] c = placeCell(player);
        if (c == null) return ActionResult.PASS;
        if (!getWorld().isClient) {
            BlockState st = bi.getBlock().getDefaultState();
            if (st.contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING))
                st = st.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.SOUTH);
            List<Built> list = new ArrayList<>(built());
            list.add(new Built(c[0], c[1], c[2], st));
            setBuilt(list);
            if (!player.getAbilities().creativeMode) stack.decrement(1);
            var snd = st.getSoundGroup();
            getWorld().playSound(null, getX(), getY() + 1, getZ(), snd.getPlaceSound(), SoundCategory.BLOCKS, (snd.getVolume() + 1f) / 2f, snd.getPitch() * 0.8f);
        }
        return ActionResult.success(getWorld().isClient);
    }

    /** a player hitting a built block knocks it off (and gets it back) instead of damaging the frame */
    private boolean buildBreak(PlayerEntity p) {
        BuildHit h = buildRay(p);
        if (h == null || h.deck()) return false;
        List<Built> list = new ArrayList<>(built());
        for (Iterator<Built> it = list.iterator(); it.hasNext(); ) {
            Built b = it.next();
            if (b.x() != h.x() || b.y() != h.y() || b.z() != h.z()) continue;
            it.remove();
            setBuilt(list);
            if (!p.getAbilities().creativeMode && b.state().getBlock().asItem() != Items.AIR) p.getInventory().offerOrDrop(new ItemStack(b.state().getBlock()));
            var snd = b.state().getSoundGroup();
            getWorld().playSound(null, getX(), getY() + 1, getZ(), snd.getBreakSound(), SoundCategory.BLOCKS, (snd.getVolume() + 1f) / 2f, snd.getPitch() * 0.8f);
            return true;
        }
        return false;
    }

    /** Turn by delta degrees unless the longer footprint would swing into a wall (kerbs it can climb don't count). */
    private void turn(float delta) {
        if (delta == 0) return;
        float old = getYaw();
        setYaw(old + delta);
        Box nb = calculateBoundingBox();
        double lift = Math.min(1.05, getStepHeight() * 0.9 + 0.05);
        Box test = nb.contract(0.12, 0, 0.12).offset(0, lift, 0);
        boolean free = true;
        for (VoxelShape shape : getWorld().getBlockCollisions(this, test)) {
            if (!shape.isEmpty()) {
                free = false;
                break;
            }
        }
        if (free) {
            setBoundingBox(nb);
        } else {
            setYaw(old);
            yawDelta = 0;
        }
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
            Vec3d s = type.seats.get(index).multiply(scaleNow);
            LivingEntity drv = getControllingPassenger();
            if (type.seatTurns && index == 0 && drv != null) {
                // the operator's cab swings round with the house, which follows where they look
                double a = Math.toRadians(-MathHelper.wrapDegrees(drv.getHeadYaw() - getYaw()));
                double px = type.turret.x, pz = type.turret.z, dx = s.x - px, dz = s.z - pz;
                s = new Vec3d(px + dx * Math.cos(a) + dz * Math.sin(a), s.y, pz - dx * Math.sin(a) + dz * Math.cos(a));
            }
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
                toWorld(0, 0, type.rearZ - 1.2), toWorld(0, 0, type.frontZ + 1.2), getPos().add(0, type.height + 0.2, 0)
        };
        for (Vec3d t : tries) {
            Vec3d ground = new Vec3d(t.x, Math.max(getY(), t.y - 1.5), t.z);
            Box box = passenger.getBoundingBox().offset(ground.subtract(passenger.getPos()));
            if (getWorld().isSpaceEmpty(passenger, box)) return ground;
        }
        return super.updatePassengerForDismount(passenger);
    }

    // ------------------------------------------------------------- cargo: the trunk, bed, hold or saddlebags

    /** what's in the back; made on first use, sized to the vehicle */
    private Cargo cargo;
    /** loot not yet rolled into the cargo (parked cars in the city), and its seed */
    private net.minecraft.util.Identifier cargoLoot;
    private long cargoSeed;

    /** an inventory that only stays open while you're next to the (still existing) vehicle */
    private final class Cargo extends net.minecraft.inventory.SimpleInventory {
        Cargo(int size) {
            super(size);
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return isAlive() && player.squaredDistanceTo(VehicleEntity.this) < 100.0;
        }
    }

    /** rows of nine: saddlebags on bikes and animals, a trunk on cars, a whole bed or hold on trucks and big craft */
    public int upgrades() {
        return dataTracker.get(UPGRADES);
    }

    public boolean has(int upgrade) {
        return (upgrades() & upgrade) != 0;
    }

    public int trailer() {
        return dataTracker.get(TRAILER);
    }

    /** where the trailer sits: following the path the vehicle drove */
    public CarriagePose trailerPose() {
        return poseAt(-type.rearZ + 0.5 + Upgrades.trailerLength(trailer()) / 2);
    }

    public int cargoRows() {
        int extra = (has(Upgrades.RACK) ? 2 : 0) + (trailer() == 1 ? 3 : 0);
        return Math.min(6, baseCargoRows() + extra);
    }

    private int baseCargoRows() {
        switch (type.category) {
            case BIKE, ANIMAL, BALL, BALLOON -> { return 1; }
            case TRUCK, RV, TRAIN -> { return 6; }
            default -> { }
        }
        float len = type.frontZ - type.rearZ;
        float size = type.width * type.height * Math.max(1f, len);
        return size < 8 ? 2 : size < 30 ? 3 : size < 60 ? 4 : 6;
    }

    public net.minecraft.inventory.Inventory getCargo() {
        if (cargo == null) cargo = new Cargo(cargoRows() * 9);
        return cargo;
    }

    /** fill the cargo from a loot table the first time it's opened */
    public void setCargoLoot(net.minecraft.util.Identifier table, long seed) {
        cargoLoot = table;
        cargoSeed = seed;
    }

    private void rollCargoLoot(PlayerEntity opener) {
        if (cargoLoot == null || !(getWorld() instanceof ServerWorld w)) return;
        net.minecraft.loot.LootTable table = w.getServer().getLootManager().getLootTable(cargoLoot);
        cargoLoot = null;
        net.minecraft.loot.context.LootContextParameterSet params = new net.minecraft.loot.context.LootContextParameterSet.Builder(w)
                .add(net.minecraft.loot.context.LootContextParameters.ORIGIN, getPos())
                .luck(opener.getLuck()).add(net.minecraft.loot.context.LootContextParameters.THIS_ENTITY, opener)
                .build(net.minecraft.loot.context.LootContextTypes.CHEST);
        table.supplyInventory(getCargo(), params, cargoSeed);
    }

    private void readCargo(NbtCompound nbt) {
        cargoLoot = nbt.contains("CargoLoot") ? new net.minecraft.util.Identifier(nbt.getString("CargoLoot")) : null;
        cargoSeed = nbt.getLong("CargoSeed");
        if (!nbt.contains("Cargo")) return;
        net.minecraft.inventory.Inventory inv = getCargo();
        net.minecraft.nbt.NbtList list = nbt.getList("Cargo", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            int slot = e.getByte("Slot") & 255;
            if (slot < inv.size()) inv.setStack(slot, ItemStack.fromNbt(e));
        }
    }

    private void writeCargo(NbtCompound nbt) {
        if (cargoLoot != null) {
            nbt.putString("CargoLoot", cargoLoot.toString());
            nbt.putLong("CargoSeed", cargoSeed);
        }
        if (cargo == null) return;
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack st = cargo.getStack(i);
            if (st.isEmpty()) continue;
            NbtCompound e = new NbtCompound();
            e.putByte("Slot", (byte) i);
            st.writeNbt(e);
            list.add(e);
        }
        nbt.put("Cargo", list);
    }

    /** does a click at this point (relative to the vehicle) land on the back, where the cargo is? */
    public boolean atCargo(Vec3d rel) {
        double yaw = Math.toRadians(getYaw());
        double along = -rel.x * Math.sin(yaw) + rel.z * Math.cos(yaw);         // + toward the front
        double len = type.frontZ - type.rearZ;
        double share = switch (type.category) {
            case TRUCK -> 0.45;                                                // pickup beds, vans, box trucks
            case BIKE, ANIMAL, BALL, BALLOON -> 0.35;
            default -> 0.3;
        };
        return along <= type.rearZ + len * share;
    }

    /** a local point at the back of the vehicle, for opening the cargo without a click position (the iso menu) */
    public Vec3d cargoPoint() {
        return toWorld(0, type.height * 0.5, type.rearZ + 0.3);
    }

    public void openCargo(PlayerEntity player) {
        if (getWorld().isClient) return;
        rollCargoLoot(player);
        int rows = cargoRows();
        net.minecraft.screen.ScreenHandlerType<net.minecraft.screen.GenericContainerScreenHandler> handler = switch (rows) {
            case 1 -> net.minecraft.screen.ScreenHandlerType.GENERIC_9X1;
            case 2 -> net.minecraft.screen.ScreenHandlerType.GENERIC_9X2;
            case 3 -> net.minecraft.screen.ScreenHandlerType.GENERIC_9X3;
            case 4 -> net.minecraft.screen.ScreenHandlerType.GENERIC_9X4;
            case 5 -> net.minecraft.screen.ScreenHandlerType.GENERIC_9X5;
            default -> net.minecraft.screen.ScreenHandlerType.GENERIC_9X6;
        };
        String kind = switch (type.category) {
            case BIKE, ANIMAL, BALL, BALLOON -> "saddlebags";
            case CAR, MILITARY -> "trunk";
            case TRUCK -> "bed";
            default -> "hold";
        };
        Text title = Text.translatable("container.vehicleforge." + kind, getName());
        net.minecraft.inventory.Inventory inv = getCargo();
        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new net.minecraft.screen.GenericContainerScreenHandler(handler, syncId, playerInv, inv, rows), title));
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.NEUTRAL, 0.6f, 0.8f);
    }

    /** a click on the back of the vehicle opens its cargo; anywhere else gets you in (see interact) */
    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
        if (type.buildable && hand == Hand.MAIN_HAND && !hasPassenger(player) && player.getStackInHand(hand).getItem() instanceof net.minecraft.item.BlockItem bi) {
            ActionResult r = buildPlace(player, player.getStackInHand(hand), bi);
            if (r != ActionResult.PASS) return r;
        }
        if (hand != Hand.MAIN_HAND || player.shouldCancelInteraction() || hasPassenger(player)) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        // items that do something to the vehicle itself take priority
        if (stack.getItem() instanceof DyeItem || stack.isOf(ModRegistry.FUEL_CAN) || stack.isOf(Items.IRON_INGOT)
                || stack.isOf(ModRegistry.CARRIAGE_ITEM) || (type.category == Category.ANIMAL && stack.isOf(Items.WHEAT))) return ActionResult.PASS;
        if (!atCargo(hitPos)) return ActionResult.PASS;
        openCargo(player);
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof Upgrades.Part part && Upgrades.fits(type)) {
            if (!getWorld().isClient) {
                if (part.trailer > 0) {
                    int had = trailer();
                    if (had == part.trailer) {
                        dataTracker.set(TRAILER, 0);
                        player.getInventory().offerOrDrop(new ItemStack(stack.getItem()));
                    } else {
                        if (had > 0) player.getInventory().offerOrDrop(new ItemStack(Upgrades.ITEMS.get(Upgrades.UPGRADE_IDS.length + had - 1)));
                        dataTracker.set(TRAILER, part.trailer);
                        path.clear();
                        if (!player.getAbilities().creativeMode) stack.decrement(1);
                    }
                } else if (!has(part.upgrade)) {
                    dataTracker.set(UPGRADES, upgrades() | part.upgrade);
                    if (!player.getAbilities().creativeMode) stack.decrement(1);
                }
                getWorld().playSound(null, getX(), getY(), getZ(), net.minecraft.sound.SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.6f, 1.2f);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (player.shouldCancelInteraction()) {
            if (type.interiorRoom || trailer() == 2) {
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
        if (type.category == Category.ANIMAL && stack.isOf(Items.WHEAT) && getHealth() < type.health) {
            if (!getWorld().isClient) {
                setHealth(Math.min(type.health, getHealth() + type.health * 0.3f));
                if (getWorld() instanceof ServerWorld w) w.spawnParticles(ParticleTypes.HEART, getX(), getY() + 2.2, getZ(), 3, 0.3, 0.2, 0.3, 0);
                serverWorldSound("ostrich_call", 0.6f, 1.4f);
                if (!player.getAbilities().creativeMode) stack.decrement(1);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (stack.isOf(Items.IRON_INGOT) && getHealth() < type.health && type.category != Category.ANIMAL) {
            if (!getWorld().isClient) {
                setHealth(Math.min(type.health, getHealth() + type.health * (isWreck() ? 0.1f : 0.2f)));
                getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.6f, 1.3f);
                if (isWreck() && getHealth() >= type.health) {
                    setWreck(false);
                    player.sendMessage(Text.translatable("msg.vehicleforge.wreck_fixed").formatted(Formatting.GREEN), true);
                } else
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
        if (isWreck()) {
            if (!getWorld().isClient) player.sendMessage(Text.translatable("msg.vehicleforge.wrecked").formatted(Formatting.GRAY), true);
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
        if (type.buildable && source.getAttacker() instanceof PlayerEntity bp && source.getSource() == bp && buildBreak(bp)) return true;
        boolean creative = source.getAttacker() instanceof PlayerEntity p && p.getAbilities().creativeMode;
        dataTracker.set(WOBBLE, 10);
        if (has(Upgrades.ARMOR)) amount *= 0.6f;
        if (has(Upgrades.BARS) && source.getAttacker() instanceof net.minecraft.entity.mob.HostileEntity) amount *= 0.5f;
        float h = getHealth() - (creative ? type.health : amount * (source.getAttacker() instanceof PlayerEntity ? 4f : 1f));
        setHealth(h);
        if (h <= 0) destroy(!creative && source.getAttacker() instanceof PlayerEntity, creative);
        return true;
    }

    /** drop = broken up by a player (gives the item back); otherwise it's a wreck and goes up in flames */
    private void destroy(boolean drop, boolean quiet) {
        if (cargo != null) net.minecraft.util.ItemScatterer.spawn(getWorld(), this, cargo);   // whatever was in the back
        if (!quiet) for (Built b : built()) if (b.state().getBlock().asItem() != Items.AIR) dropStack(new ItemStack(b.state().getBlock()));
        if (drop && isWreck()) {
            dropStack(new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(3)));   // scrap, not a free car
        } else if (drop) {
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
        prevScale = scaleNow;
        prevForkHeight = forkHeight;
        float oyaw = getYaw();
        float st = getScaleTarget();
        if (scaleNow != st) {
            scaleNow += (st - scaleNow) * 0.12f;
            if (Math.abs(st - scaleNow) < 0.003f) scaleNow = st;
            calculateDimensions();
        }
        super.tick();
        if (getWobble() > 0 && !getWorld().isClient) dataTracker.set(WOBBLE, getWobble() - 1);
        // some wrecks are still smouldering
        if (isWreck() && getWorld().isClient && Math.floorMod(getId(), 3) == 0 && random.nextInt(5) == 0) {
            Vec3d at = toWorld((random.nextFloat() - 0.5f) * type.width * 0.6f, type.height * 0.6, type.frontZ * 0.6f);
            getWorld().addParticle(random.nextInt(4) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 0.04, 0);
        }

        if (isLogicalSideForUpdatingMovement()) {
            lerpTicks = 0;
            physics();
        } else {
            yawDelta = 0;
            if (getWorld().isClient) lerpStep();
            else setVelocity(Vec3d.ZERO);
        }

        if (type.category == Category.TRAIN || trailer() > 0) recordPath();

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
        thrusting = switch (type.category) {
            case ROCKET -> dy > 0.04;
            case PLANE -> moved > 0.4;
            case BALLOON -> dy > 0.004 || (getWorld().isClient && InputState.vehicleId == getId() && InputState.up && engineRunning());
            default -> type.weapon == VehicleType.Weapon.BOOST && abilityOn();
        };
        if (type.weapon == VehicleType.Weapon.MOWER && abilityOn()) rotorSpin += 50f;
        if (type.weapon == VehicleType.Weapon.FORKS) forkHeight += ((abilityOn() ? type.lift : 0f) - forkHeight) * 0.08f;
        if (type.weapon == VehicleType.Weapon.BLADE) forkHeight += ((abilityOn() ? 0f : type.lift) - forkHeight) * 0.1f;

        updateGearbox();
        setBoundingBox(calculateBoundingBox());
        if (getWorld().isClient) {
            // the suspension traces the ground under each wheel: no need while it's parked and settled
            boolean settled = getControllingPassenger() == null && Math.abs(clientSpeed) < 0.001f && Math.abs(accelVis) < 0.0005f
                    && Math.abs(pitchVel) + Math.abs(rollVel) < 0.01f && (age + getId()) % 20 != 0;
            if (!settled) updateBody();
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

    /** at night (or in rain, darkness, underwater) a driven vehicle's headlights light up the road ahead */
    private void headlights() {
        if (getControllingPassenger() == null || (age + getId()) % 2 != 0 || !(getWorld() instanceof ServerWorld w)) return;
        switch (type.category) {
            case ANIMAL, BALL, BALLOON -> { return; }
            default -> { }
        }
        long t = w.getTimeOfDay() % 24000L;
        boolean dark = t > 12600 && t < 23400 || w.isRaining() || w.getLightLevel(LightType.SKY, getBlockPos().up()) < 8 || isSubmergedInWater();
        if (!dark) return;
        float[] ahead = {2.5f, 7f, 12f, 17f};
        int[] level = {15, 14, 13, 11};
        for (int i = 0; i < ahead.length; i++) {
            Vec3d at = toWorld(0, 0.7, type.frontZ + ahead[i] * Math.max(0.6f, scaleNow));
            BlockPos p = BlockPos.ofFloored(at);
            if (!w.getBlockState(p).isAir() && !w.getBlockState(p).isOf(com.nothatcher.vehicleforge.block.CityExtras.HEADLIGHT)) p = p.up();
            com.nothatcher.vehicleforge.block.CityExtras.HEADLIGHT.shine(w, p, level[i]);
        }
    }

    private void serverTick() {
        headlights();
        if ((type.interiorRoom || trailer() == 2) && age % 20 == 0) InteriorManager.track(this);
        if (sprayOn()) Weapons.sprayTick(this);
        if (sirenOn() && age % 34 == 0) {
            getWorld().playSound(null, getX(), getY(), getZ(), ModRegistry.sound("siren"), SoundCategory.NEUTRAL, 3f, 1f);
        }
        burnFuel();
        hitMobs();
        switch (type.weapon) {
            case TIME_JUMP -> {
                if (age % 10 == 0) {
                    timeHistory.addFirst(new double[]{getX(), getY(), getZ(), getYaw()});
                    while (timeHistory.size() > 61) timeHistory.removeLast();
                }
                boolean charged = getControllingPassenger() != null && measuredSpeed >= type.maxSpeed * 0.75f;
                if (charged != abilityOn()) setAbility(charged);
            }
            case SHRINK -> {
                if (age % 20 == 0 && getScaleTarget() < 0.9f) {
                    for (Entity p : getPassengerList()) {
                        if (p instanceof LivingEntity le) le.addStatusEffect(new StatusEffectInstance(ModEffects.SHRUNK, 2400, 0, false, false, true));
                    }
                }
            }
            case TRACTOR_BEAM -> {
                if (abilityOn()) {
                    if (getControllingPassenger() == null) setAbility(false);
                    else Weapons.beamTick(this);
                }
            }
            case MOWER -> {
                if (abilityOn() && age % 2 == 0) Weapons.mowTick(this);
            }
            case BOOST, DIG -> {
                if (abilityTicks > 0 && --abilityTicks == 0) setAbility(false);
            }
            case BLADE -> {
                if (abilityOn() && measuredSpeed > 0.02f) Weapons.bladeTick(this);
            }
            case PAVE -> {
                if (abilityOn()) Weapons.paveTick(this);
            }
            case CRANE, TOW -> {
                if (carried != null) {
                    if (getControllingPassenger() == null || carried.isRemoved() || carried.squaredDistanceTo(this) > 400) Weapons.release(this);
                    else if (type.weapon == VehicleType.Weapon.CRANE) Weapons.craneTick(this);
                    else Weapons.towTick(this);
                }
            }
            default -> { }
        }
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
            float ram = measuredSpeed * 16f * mass * (has(Upgrades.SPIKES) ? 2f : 1f);
            double shove = has(Upgrades.PLOW) ? 2.4 : 1.4;
            if (e.damage(src, ram)) {
                e.addVelocity(dir.x * measuredSpeed * shove, 0.3 + measuredSpeed * 0.25, dir.z * measuredSpeed * shove);
                e.velocityModified = true;
                serverWorldSound("crash", 0.6f, 1.4f);
            }
        }
    }

    // ------------------------------------------------------------ physics

    private void physics() {
        if (carriedBy != null) {
            if (carriedBy.isRemoved() || carriedBy.carried != this) carriedBy = null;
            else return;
        }
        if (asleep()) {
            setVelocity(Vec3d.ZERO);
            return;
        }
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
            case BALLOON -> balloon(driven, engine, pedal, steer, up);
            case TRAIN -> train(pedal);
            default -> ground(driven, pedal, steer, up, engine);
        }
    }

    /**
     * A parked ground vehicle that isn't moving skips its physics (and the block collision that comes with it) except
     * for a check every half second, so a city full of parked cars and wrecks costs next to nothing. Anything that
     * gives it a shove (an explosion, another car) sets its velocity and wakes it up.
     */
    private boolean asleep() {
        if (getWorld().isClient || !getPassengerList().isEmpty() || !isOnGround()) return false;
        switch (type.category) {
            case HELICOPTER, PLANE, ROCKET, BOAT, SUBMARINE, BALLOON, TRAIN -> { return false; }
            default -> { }
        }
        if (Math.abs(speed) > 0.002f || getVelocity().horizontalLengthSquared() > 1.0e-6 || boostTicks > 0) return false;
        if (abilityOn() || sirenOn() || isTouchingWater() || scaleNow != getScaleTarget()) return false;
        return (age + getId()) % 10 != 0;
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
        turn(yawDelta);
    }

    private enum Surface { ROAD, TRACK, SOFT, SNOW, MUD, ICE }

    private Surface surface() {
        BlockState st = getWorld().getBlockState(BlockPos.ofFloored(getX(), getY() - 0.2, getZ()));
        if (st.isIn(BlockTags.ICE)) return Surface.ICE;
        if (st.isIn(BlockTags.SNOW)) return Surface.SNOW;
        if (st.isOf(Blocks.MUD) || st.isOf(Blocks.SOUL_SAND) || st.isOf(Blocks.SOUL_SOIL)) return Surface.MUD;
        if (st.isOf(Blocks.DIRT_PATH) || st.isOf(Blocks.COARSE_DIRT)) return Surface.TRACK;   // dirt roads
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
            case TRACK -> {                                               // packed dirt: a bit slower, a bit looser
                max *= type.offroad ? 0.97f : 0.85f;
                grip = 0.68f;
            }
            case SOFT, SNOW -> {
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
        if (type.hover) {                                                // air cushion: slides everywhere, fast on water
            grip = 0.12f;
            max = type.maxSpeed * (floating ? 1f : 0.95f);
        }
        if (type.snow && (surf == Surface.ICE || surf == Surface.SNOW)) {
            grip = 0.65f;
            max = type.maxSpeed * 1.1f;
        }
        max *= 0.5f + 0.5f * scaleNow;                                    // shrunk cars are slower
        boolean boosting = boostTicks > 0;
        if (boosting) {
            max = Math.max(max, 2.0f);
            boostTicks--;
        }
        if (boostCooldown > 0) boostCooldown--;
        boolean animal = type.category == Category.ANIMAL, ball = type.category == Category.BALL;
        // momentum from last tick, in the vehicle's own frame
        Vec3d vel = getVelocity();
        Vec3d f = forward(), left = leftVec();
        double lat = vel.x * left.x + vel.z * left.z;
        if (onGround || floating) speed = (float) (vel.x * f.x + vel.z * f.z);

        boolean drift = handbrake && Math.abs(speed) > 0.3f && !animal && !ball;
        if (onGround || floating) {
            float slope = driven ? slopeDeg() : 0;
            if (surf == Surface.ICE) pedal *= 0.5f;
            accelerate(pedal, max);
            speed -= (float) Math.sin(Math.toRadians(slope)) * (ball ? 0.07f : 0.03f);   // hills slow you down / roll you back
            if (boosting) speed += 0.09f;
            if (handbrake && !animal) speed *= drift ? 0.975f : 0.85f;
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
        turn(yawDelta);

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
            if (animal && handbrake && onGround) vy = 0.62;                // ostriches jump
        }
        Vec3d v = new Vec3d(f.x * speed + left.x * lat, vy, f.z * speed + left.z * lat);
        double before = v.horizontalLength();
        setVelocity(v);
        move(MovementType.SELF, v);
        if (horizontalCollision) {
            if (ball) {
                setVelocity(-v.x * 0.55, getVelocity().y, -v.z * 0.55);     // boing
                speed = -speed * 0.55f;
            } else {
                double after = getVelocity().horizontalLength();
                if (!animal) reportCrash((float) (before - after));
                speed *= 0.6f;
            }
        }
        if (!animal && !ball) landing(onGround, 0.9f);
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

    /** Hot air balloon: hold Space to fire the burner and rise, let go to sink slowly. The wind pushes you around. */
    private void balloon(boolean driven, boolean engine, float pedal, float steer, boolean up) {
        setNoGravity(true);
        Vec3d vel = getVelocity();
        double vy = vel.y;
        boolean burn = driven && engine && up;
        vy += burn ? 0.007 : -0.0035;
        vy = MathHelper.clamp(vy * 0.985, -0.18, 0.25);
        boolean ground = isOnGround();
        if (ground && vy < 0) vy = 0;
        float target = driven ? pedal * type.maxSpeed : 0;
        speed += (target - speed) * 0.03f;
        if (ground) speed *= 0.8f;
        steerInput += (steer - steerInput) * 0.1f;
        yawDelta = -steerInput * type.turn;
        setYaw(getYaw() + yawDelta);
        Vec3d f = forward();
        double wx = 0, wz = 0;
        if (!ground) {
            double t = getWorld().getTime() * 0.0007;
            wx = Math.cos(t) * 0.035;
            wz = Math.sin(t * 1.3) * 0.035;
        }
        setVelocity(f.x * speed + wx, vy, f.z * speed + wz);
        move(MovementType.SELF, getVelocity());
        setPitch(0);
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
        return poseAt(type.carriageStart + (k - 1) * 6.3);
    }

    /** the point a given distance back along the path driven, and which way the path ran there */
    public CarriagePose poseAt(double want) {
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
