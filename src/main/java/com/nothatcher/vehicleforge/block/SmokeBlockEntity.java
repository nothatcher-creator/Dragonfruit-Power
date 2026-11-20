package com.nothatcher.vehicleforge.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class SmokeBlockEntity extends BlockEntity {
    public SmokeBlockEntity(BlockPos pos, BlockState state) {
        super(CityExtras.SMOKE_BE, pos, state);
    }

    public static void clientTick(World w, BlockPos pos, BlockState state, SmokeBlockEntity be) {
        Random r = w.random;
        boolean steam = state.getBlock() instanceof SmokeBlock sb && sb.steam;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.2, z = pos.getZ() + 0.5;
        if (steam) {
            if (r.nextInt(2) == 0) w.addImportantParticle(CityExtras.STEAM, true, x + (r.nextDouble() - 0.5) * 0.5, y, z + (r.nextDouble() - 0.5) * 0.5,
                    0, 0.05 + r.nextDouble() * 0.03, 0);
            if (r.nextInt(120) == 0) w.playSound(x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.AMBIENT, 0.15f, 0.6f + r.nextFloat() * 0.2f, false);
            return;
        }
        if (r.nextInt(3) == 0) w.addImportantParticle(CityExtras.DARK_SMOKE, true, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6,
                0, 0.06 + r.nextDouble() * 0.03, 0);
        if (r.nextInt(6) == 0) w.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
        if (r.nextInt(14) == 0) w.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
        if (r.nextInt(50) == 0) w.playSound(x, y, z, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 0.6f, 0.8f + r.nextFloat() * 0.3f, false);
    }
}
