package com.nothatcher.vehicleforge.block;

import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** The jet at the top of a fountain: sprays water up and lets it rain back down into the basin. */
public class FountainSpoutBlock extends FacingShapeBlock {
    public FountainSpoutBlock(Settings s, VoxelShape shape) {
        super(s, shape, false);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random r) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.8, cz = pos.getZ() + 0.5;
        for (int i = 0; i < 6; i++) {
            world.addParticle(ParticleTypes.SPLASH, cx + (r.nextDouble() - 0.5) * 0.3, cy + r.nextDouble() * 0.4, cz + (r.nextDouble() - 0.5) * 0.3,
                    (r.nextDouble() - 0.5) * 0.3, 0.6 + r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.3);
        }
        for (int i = 0; i < 4; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 0.9 + r.nextDouble() * 1.1;
            world.addParticle(ParticleTypes.FALLING_WATER, cx + Math.cos(a) * d, cy + 0.6 + r.nextDouble(), cz + Math.sin(a) * d, 0, 0, 0);
        }
        if (r.nextInt(6) == 0) world.playSound(cx, cy, cz, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, 0.4f, 1.2f + r.nextFloat() * 0.3f, false);
    }
}
