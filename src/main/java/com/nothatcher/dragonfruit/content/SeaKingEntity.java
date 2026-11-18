package com.nothatcher.dragonfruit.content;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import java.util.ArrayDeque;

/**
 * Sea King: a huge sea serpent that hunts anyone in or on the ocean. Boss bar, bites, and breaches
 * out of the water. Only the head has a hitbox; the body is drawn from the head's recent path.
 */
public class SeaKingEntity extends WaterCreatureEntity {
    private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);
    /** client only: recent head positions, newest first */
    public final ArrayDeque<Vec3d> trail = new ArrayDeque<>();
    public int jawTicks;
    private int attackCooldown;
    private int breachTimer = 160;
    private Vec3d wanderGoal;

    public SeaKingEntity(EntityType<? extends WaterCreatureEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 80;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 220)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_ARMOR, 8);
    }

    /** Natural spawns: rare, and only well below sea level in deep water. */
    public static boolean canSpawnHere(EntityType<SeaKingEntity> type, ServerWorldAccess world, SpawnReason reason,
                                       BlockPos pos, Random random) {
        return random.nextInt(40) == 0
                && pos.getY() < world.getSeaLevel() - 6
                && world.getFluidState(pos).isIn(FluidTags.WATER)
                && WaterCreatureEntity.canSpawn(type, world, reason, pos, random);
    }

    @Override
    public Box getVisibilityBoundingBox() {
        return getBoundingBox().expand(18);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            trail.addFirst(getPos());
            while (trail.size() > 90) trail.removeLast();
            if (jawTicks > 0) jawTicks--;
        }
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 4) jawTicks = 12;
        else super.handleStatus(status);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        bossBar.setPercent(getHealth() / getMaxHealth());
        ServerWorld w = (ServerWorld) getWorld();
        boolean inWater = isTouchingWater();
        setNoGravity(inWater);

        PlayerEntity target = w.getClosestPlayer(this, 48);
        if (target != null && (target.isCreative() || target.isSpectator()
                || !(target.isTouchingWater() || target.hasVehicle() || target.getY() < w.getSeaLevel() + 3))) {
            target = null;
        }

        Vec3d goal;
        if (target != null) {
            goal = target.getPos().add(0, target.getHeight() * 0.5, 0);
        } else {
            if (wanderGoal == null || age % 140 == 0 || wanderGoal.squaredDistanceTo(getPos()) < 4) {
                Random r = getRandom();
                wanderGoal = getPos().add((r.nextDouble() - 0.5) * 32, (r.nextDouble() - 0.6) * 8, (r.nextDouble() - 0.5) * 32);
            }
            goal = wanderGoal;
        }

        if (inWater) {
            Vec3d to = goal.subtract(getPos());
            double d = to.length();
            if (d > 0.5) {
                Vec3d want = to.multiply(1.0 / d).multiply(target != null ? 0.5 : 0.2);
                // don't swim up into the air unless breaching
                if (!getWorld().getFluidState(getBlockPos().up(2)).isIn(FluidTags.WATER) && want.y > 0) {
                    want = new Vec3d(want.x, 0, want.z);
                }
                setVelocity(getVelocity().multiply(0.9).add(want.multiply(0.12)));
            }
            Vec3d v = getVelocity();
            if (v.horizontalLengthSquared() > 1.0E-4) {
                float yaw = (float) (MathHelper.atan2(v.z, v.x) * (180 / Math.PI)) - 90f;
                setYaw(yaw);
                bodyYaw = yaw;
                headYaw = yaw;
                setPitch((float) (-MathHelper.atan2(v.y, v.horizontalLength()) * (180 / Math.PI)));
            }
        }

        if (attackCooldown > 0) attackCooldown--;
        if (target != null && squaredDistanceTo(target) < 4.8 * 4.8 && attackCooldown <= 0) {
            tryAttack(target);
            target.addVelocity(getRotationVector().x * 0.8, 0.5, getRotationVector().z * 0.8);
            target.velocityModified = true;
            attackCooldown = 25;
            w.sendEntityStatus(this, (byte) 4);
            w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ATTACK, SoundCategory.HOSTILE, 2f, 0.5f);
            Vfx.impact(w, target.getPos().add(0, 1, 0), 2.2f, 0xFFFFFF, 6);
        }

        if (breachTimer > 0) breachTimer--;
        if (target != null && inWater && breachTimer <= 0 && squaredDistanceTo(target) < 22 * 22) {
            breachTimer = 220 + getRandom().nextInt(120);
            setVelocity(getVelocity().add(0, 1.35, 0));
            w.sendEntityStatus(this, (byte) 4);
            w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 3f, 0.5f);
            w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.HOSTILE, 3f, 0.5f);
            Vfx.burst(w, getPos().add(0, 1, 0), Vfx.UP, Vfx.B_SPLASH, 3f);
            Vfx.shake(w, getPos(), 0.8f, 20, 36);
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 3f;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }
}
