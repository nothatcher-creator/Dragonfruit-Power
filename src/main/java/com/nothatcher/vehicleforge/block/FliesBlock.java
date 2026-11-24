package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** An invisible block that's just a swarm of flies buzzing around: over bins, trash bags and anything rotten. */
public class FliesBlock extends Block {
    private static final VoxelShape OUTLINE = createCuboidShape(4, 4, 4, 12, 12, 12);

    public FliesBlock(Settings s) {
        super(s);
    }

    @Override
    public BlockRenderType getRenderType(BlockState st) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return VoxelShapes.empty();
    }

    @Override
    public void randomDisplayTick(BlockState st, World w, BlockPos pos, Random r) {
        int n = 2 + r.nextInt(3);
        for (int i = 0; i < n; i++)
            w.addParticle(CityExtras.FLY, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.1 + r.nextDouble() * 0.6,
                    pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0, 0);
        if (r.nextInt(6) == 0)
            w.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.ENTITY_BEE_LOOP, SoundCategory.AMBIENT,
                    0.08f, 2.0f, false);
    }
}
