package com.nothatcher.vehicleforge.power;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** A floodlight panel that comes on when it's on a live grid (or given a redstone signal). */
public class ElectricLightBlock extends Block {
    public ElectricLightBlock(Settings s) {
        super(s.luminance(st -> st.get(Properties.LIT) ? 15 : 0));
        setDefaultState(getDefaultState().with(Properties.LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.LIT);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState old, boolean notify) {
        if (!world.isClient) world.scheduleBlockTick(pos, this, 5);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        update(state, world, pos);
        world.scheduleBlockTick(pos, this, 30);
    }

    /** lights built into the city check now and then (random ticks) rather than constantly */
    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        update(state, world, pos);
    }

    private void update(BlockState state, ServerWorld world, BlockPos pos) {
        boolean on = PowerGrid.cityPowered(world, pos);
        if (on != state.get(Properties.LIT)) world.setBlockState(pos, state.with(Properties.LIT, on));
    }

    @Override
    public net.minecraft.util.ActionResult onUse(BlockState state, World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player,
                                                 net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
        if (world instanceof ServerWorld sw) update(state, sw, pos);
        return net.minecraft.util.ActionResult.success(world.isClient);
    }
}
