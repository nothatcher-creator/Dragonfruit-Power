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

    /** how far away you can be and still have the smoke drawn (kept modest: these are big see-through puffs) */
    private static final double SMOKE_RANGE = 112, STEAM_RANGE = 40;

    public static void clientTick(World w, BlockPos pos, BlockState state, SmokeBlockEntity be) {
        Random r = w.random;
        boolean steam = state.getBlock() instanceof SmokeBlock sb && sb.steam;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.2, z = pos.getZ() + 0.5;
        // only near someone, and only every few ticks: each source used to keep hundreds of puffs in the air
        if (((w.getTime() + pos.hashCode()) & 3) != 0) return;
        if (!w.isPlayerInRange(x, y, z, steam ? STEAM_RANGE : SMOKE_RANGE)) return;
        if (steam) {
            if (r.nextInt(2) == 0) w.addParticle(CityExtras.STEAM, x + (r.nextDouble() - 0.5) * 0.5, y, z + (r.nextDouble() - 0.5) * 0.5,
                    0, 0.05 + r.nextDouble() * 0.03, 0);
            if (r.nextInt(30) == 0) w.playSound(x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.AMBIENT, 0.15f, 0.6f + r.nextFloat() * 0.2f, false);
            return;
        }
        // dark column: forced so it still shows from a distance, but far fewer puffs (they're big and last a while)
        if (r.nextInt(3) == 0) w.addImportantParticle(CityExtras.DARK_SMOKE, true, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6,
                0, 0.06 + r.nextDouble() * 0.03, 0);
        if (r.nextInt(3) == 0) w.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
        if (r.nextInt(5) == 0) w.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
        if (r.nextInt(12) == 0) w.playSound(x, y, z, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 0.6f, 0.8f + r.nextFloat() * 0.3f, false);
    }
}
