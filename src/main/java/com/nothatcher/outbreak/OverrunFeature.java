package com.nothatcher.outbreak;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Runs after the city is built, once per chunk: smashes some ground-floor windows, and scatters blood, litter,
 * broken glass, scorch marks, weeds, trash bags and climbing vines over streets, yards and ground floors.
 */
public class OverrunFeature extends Feature<DefaultFeatureConfig> {
    private static final int STREET = 64;

    public OverrunFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
        StructureWorldAccess w = ctx.getWorld();
        Random r = ctx.getRandom();
        int x0 = ctx.getOrigin().getX() & ~15, z0 = ctx.getOrigin().getZ() & ~15;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                boolean smashed = false;
                for (int y = STREET + 1; y <= STREET + 3; y++) {
                    m.set(x, y, z);
                    BlockState s = w.getBlockState(m);
                    if (s.getSoundGroup() == BlockSoundGroup.GLASS && s.getLuminance() == 0 && r.nextFloat() < 0.14f) {
                        w.setBlockState(m, Blocks.AIR.getDefaultState(), 2);
                        smashed = true;
                    }
                }
                m.set(x, STREET, z);
                if (!w.getBlockState(m).isAir()) continue;
                BlockPos below = m.down();
                if (!w.getBlockState(below).isSideSolidFullSquare(w, below, Direction.UP)) continue;
                BlockState put = null;
                float f = r.nextFloat();
                if (smashed || f < 0.006f) put = OutbreakBlocks.BROKEN_GLASS.getDefaultState();
                else if (f < 0.018f) put = OutbreakBlocks.BLOOD.getDefaultState();
                else if (f < 0.042f) put = OutbreakBlocks.LITTER.getDefaultState();
                else if (f < 0.066f) put = OutbreakBlocks.WEEDS.getDefaultState();
                else if (f < 0.070f) put = OutbreakBlocks.TRASH_BAG.getDefaultState();
                else if (f < 0.074f) put = OutbreakBlocks.SCORCH.getDefaultState();
                if (put != null) {
                    w.setBlockState(m, put, 2);
                    if (put.isOf(OutbreakBlocks.BLOOD)) trail(w, r, x0, z0, x, z);
                } else if (r.nextFloat() < 0.04f) {
                    vines(w, r, x, z);
                }
            }
        }
        return true;
    }

    /** a few more drops of blood nearby (inside this chunk) */
    private void trail(StructureWorldAccess w, Random r, int x0, int z0, int x, int z) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        int n = r.nextInt(4);
        for (int i = 0; i < n; i++) {
            x += r.nextInt(3) - 1;
            z += r.nextInt(3) - 1;
            if (x < x0 || x >= x0 + 16 || z < z0 || z >= z0 + 16) return;
            m.set(x, STREET, z);
            if (w.getBlockState(m).isAir() && w.getBlockState(m.down()).isSideSolidFullSquare(w, m.down(), Direction.UP))
                w.setBlockState(m, OutbreakBlocks.BLOOD.getDefaultState(), 2);
        }
    }

    /** vines creeping up a wall next to this column */
    private void vines(StructureWorldAccess w, Random r, int x, int z) {
        Direction d = Direction.fromHorizontal(r.nextInt(4));
        BlockPos.Mutable m = new BlockPos.Mutable(), n = new BlockPos.Mutable();
        int top = STREET + 2 + r.nextInt(7);
        for (int y = top; y >= STREET; y--) {
            m.set(x, y, z);
            n.set(x + d.getOffsetX(), y, z + d.getOffsetZ());
            if (!w.getBlockState(m).isAir()) continue;
            if (!w.getBlockState(n).isSideSolidFullSquare(w, n, d.getOpposite())) continue;
            w.setBlockState(m, Blocks.VINE.getDefaultState().with(VineBlock.getFacingProperty(d), true), 2);
        }
    }
}
