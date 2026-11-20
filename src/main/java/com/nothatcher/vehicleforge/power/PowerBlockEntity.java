package com.nothatcher.vehicleforge.power;

import com.nothatcher.vehicleforge.block.CityExtras;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PowerBlockEntity extends BlockEntity {
    public static final int MAX_FUEL = 48000, MAX_CHARGE = 72000;
    int fuel, charge;
    /** client: wind turbine rotor angle in degrees */
    public float angle, prevAngle;

    public PowerBlockEntity(BlockPos pos, BlockState state) {
        super(CityExtras.POWER_BE, pos, state);
    }

    /** for pre-fuelled generators placed by the city generator */
    public void setFuel(int f) {
        fuel = Math.min(MAX_FUEL, f);
        markDirty();
    }

    private PowerBlock.Kind kind() {
        return getCachedState().getBlock() instanceof PowerBlock pb ? pb.kind : PowerBlock.Kind.POLE;
    }

    public static void serverTick(World w, BlockPos pos, BlockState state, PowerBlockEntity be) {
        if ((w.getTime() + pos.hashCode()) % 20 != 0) return;
        PowerBlock.Kind k = be.kind();
        boolean active;
        switch (k) {
            case GENERATOR -> {
                active = be.fuel > 0;
                if (active) {
                    be.fuel = Math.max(0, be.fuel - 20);
                    be.markDirty();
                }
            }
            case SOLAR -> active = w.isDay() && !w.isThundering() && w.isSkyVisible(pos.up());
            case WIND -> active = true;
            case BATTERY -> {
                if (PowerGrid.isPowered(w, pos, pos, true)) be.charge = Math.min(MAX_CHARGE, be.charge + 400);
                else if (be.charge > 0) be.charge = Math.max(0, be.charge - 20);
                active = be.charge > 0;
                be.markDirty();
            }
            default -> active = PowerGrid.isPowered(w, pos, pos);
        }
        PowerGrid.report(w, pos, active && k != PowerBlock.Kind.POLE, k != PowerBlock.Kind.BATTERY);
        int level = k != PowerBlock.Kind.BATTERY || be.charge == 0 ? 0 : Math.min(3, 1 + (int) ((long) be.charge * 3 / (MAX_CHARGE + 1)));
        if (state.get(PowerBlock.ACTIVE) != active || state.get(PowerBlock.LEVEL) != level) {
            w.setBlockState(pos, state.with(PowerBlock.ACTIVE, active).with(PowerBlock.LEVEL, level));
        }
        if (k == PowerBlock.Kind.GENERATOR && active) {
            w.playSound(null, pos, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.6f, 0.55f);
            if (w.random.nextInt(3) == 0) w.playSound(null, pos, SoundEvents.ENTITY_MINECART_RIDING, SoundCategory.BLOCKS, 0.15f, 0.5f);
        }
    }

    public static void clientTick(World w, BlockPos pos, BlockState state, PowerBlockEntity be) {
        PowerBlock.Kind k = be.kind();
        if (k == PowerBlock.Kind.WIND) {
            be.prevAngle = be.angle;
            float speed = 3f + (pos.getY() - 60) * 0.04f + (w.isRaining() ? 2f : 0f);
            be.angle = (be.angle + Math.max(2f, speed)) % 360f;
            if (be.angle < be.prevAngle) be.prevAngle -= 360f;
        } else if (k == PowerBlock.Kind.GENERATOR && state.get(PowerBlock.ACTIVE) && w.random.nextInt(3) == 0) {
            Direction back = state.get(PowerBlock.FACING).getOpposite();
            w.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + back.getOffsetX() * 0.45, pos.getY() + 0.95, pos.getZ() + 0.5 + back.getOffsetZ() * 0.45, 0, 0.04, 0);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        fuel = nbt.getInt("Fuel");
        charge = nbt.getInt("Charge");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Fuel", fuel);
        nbt.putInt("Charge", charge);
    }
}
