package com.nothatcher.outbreak;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * The invisible light a flashlight puts where it's pointed. It removes itself a moment after the beam moves on or
 * the light goes off (or after a restart, if the game stopped mid-beam), so it never gets left behind.
 */
public class FlashlightLightBlock extends Block {
    public FlashlightLightBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        long left = Flashlights.expiry(world, pos) - world.getTime();
        if (left <= 0) world.removeBlock(pos, false);
        else world.scheduleBlockTick(pos, this, (int) Math.min(left, 20));
    }
}
