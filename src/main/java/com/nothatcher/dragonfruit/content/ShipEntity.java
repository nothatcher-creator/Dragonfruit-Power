package com.nothatcher.dragonfruit.content;

import com.nothatcher.dragonfruit.net.Vfx;
import com.nothatcher.dragonfruit.power.Abilities;
import com.nothatcher.dragonfruit.power.Scheduler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Rideable pirate ship. Built on the vanilla boat (same WASD controls and water physics) but faster,
 * with a big custom model (see client/render/ShipRenderer) and broadside cannons.
 */
public class ShipEntity extends BoatEntity {
    /** local cannon positions along the hull (z), both sides */
    public static final double[] CANNON_Z = {-1.6, 0.0, 1.6};
    private int nextCannonAge;

    public ShipEntity(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    @Override
    public Item asItem() {
        return ModContent.SHIP_ITEM;
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.75;
    }

    @Override
    public Box getVisibilityBoundingBox() {
        return getBoundingBox().expand(7, 11, 7);
    }

    private boolean onWater() {
        return getWorld().getFluidState(getBlockPos()).isIn(FluidTags.WATER)
                || getWorld().getFluidState(getBlockPos().down()).isIn(FluidTags.WATER);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3d v = getVelocity();
        double h = Math.hypot(v.x, v.z);
        if (getControllingPassenger() != null && onWater() && h > 0.02 && h < 0.75) {
            // sails catch the wind: faster than a rowing boat
            setVelocity(v.x * 1.035, v.y, v.z * 1.035);
        }
        if (getWorld().isClient && h > 0.08 && onWater()) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3d bow = toWorld(side * 0.9, 0.2, 5.4);
                getWorld().addParticle(ParticleTypes.SPLASH, bow.x, bow.y, bow.z, 0, 0.1, 0);
                Vec3d wake = toWorld(side * 1.6, 0.1, -4.5);
                getWorld().addParticle(ParticleTypes.CLOUD, wake.x, wake.y, wake.z, 0, 0.01, 0);
            }
        }
    }

    /** Ship-local (x = side, y = up, z = toward bow) to world position. */
    public Vec3d toWorld(double x, double y, double z) {
        Vec3d d = toWorldDir(x, 0, z);
        return getPos().add(d.x, y, d.z);
    }

    public Vec3d toWorldDir(double x, double y, double z) {
        double yaw = Math.toRadians(getYaw());
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new Vec3d(x * c - z * s, y, x * s + z * c);
    }

    /** Broadside! Fires every cannon on both sides. */
    public void fireCannons(ServerPlayerEntity shooter) {
        if (age < nextCannonAge) {
            shooter.sendMessage(Text.translatable("msg.dragonfruit.cannons_reloading").formatted(Formatting.GRAY), true);
            return;
        }
        nextCannonAge = age + 50;
        ServerWorld w = (ServerWorld) getWorld();
        int shot = 0;
        for (int side = -1; side <= 1; side += 2) {
            for (double z : CANNON_Z) {
                final Vec3d muzzle = toWorld(side * 2.7, 1.1, z);
                final Vec3d dir = toWorldDir(side, 0.06, 0).normalize();
                Scheduler.after(shot * 2, () -> {
                    Vfx.burst(w, muzzle, dir, Vfx.B_CANNON, 1f);
                    Vfx.projectile(w, shooter.getId(), Vfx.P_CANNON, muzzle, dir, 2.4f, 30, 0.35f);
                    Abilities.sound(w, muzzle, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.4f, 1.3f);
                    Abilities.simulate(shooter, w, muzzle, dir, 2.4f, 30, 0.7f, hit -> {
                        Vfx.burst(w, hit, Vfx.UP, Vfx.B_FIRE_SMALL, 1f);
                        Vfx.burst(w, hit, dir, Vfx.B_CANNON, 1.5f);
                        Vfx.impact(w, hit, 2f, 0xFFD27A, 5);
                        Vfx.shake(w, hit, 0.4f, 8, 16);
                        Abilities.sound(w, hit, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
                        for (LivingEntity e : Abilities.inRadius(w, hit, 3, shooter)) {
                            if (e.getVehicle() == this) continue;
                            Abilities.hurt(shooter, e, 12f, true);
                            Abilities.push(e, e.getPos().subtract(hit).normalize().multiply(0.8).add(0, 0.4, 0));
                        }
                    });
                });
                shot++;
            }
        }
        Vfx.shake(w, getPos(), 0.5f, 14, 10);
    }

}
