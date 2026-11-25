package com.nothatcher.vehicleforge.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * A traffic signal head. Every light in the world runs on the same 20-second clock from the world time, so the
 * north-south lights and east-west lights at a junction always take turns (green, yellow, all-red, then the other way).
 */
public class TrafficLightBlock extends FacingShapeBlock {
    public static final IntProperty PHASE = IntProperty.of("phase", 0, 2);   // 0 green, 1 yellow, 2 red
    /** off when the city's grid is down (and nothing local powers it): treat the junction as a four-way stop */
    public static final net.minecraft.state.property.BooleanProperty POWERED = net.minecraft.state.property.Properties.POWERED;
    private static final int CYCLE = 400;

    public TrafficLightBlock(Settings settings, VoxelShape shape) {
        super(settings, shape, false);
        setDefaultState(getDefaultState().with(PHASE, 2).with(POWERED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PHASE, POWERED);
    }

    /** phase and ticks until it changes, for lights facing along the given axis */
    private static int[] phaseAt(long time, Direction.Axis axis) {
        int t = (int) Math.floorMod(time, (long) CYCLE);
        if (axis == Direction.Axis.Z) {
            if (t < 170) return new int[]{0, 170 - t};
            if (t < 210) return new int[]{1, 210 - t};
            return new int[]{2, CYCLE - t};
        }
        int u = Math.floorMod(t - 220, CYCLE);
        if (u < 150) return new int[]{0, 150 - u};
        if (u < 180) return new int[]{1, 180 - u};
        return new int[]{2, CYCLE - u};
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && !oldState.isOf(this)) world.scheduleBlockTick(pos, this, 1);
    }

    /** backup: if a light ever loses its timer, a random tick restarts it */
    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!world.getBlockTickScheduler().isQueued(pos, this)) world.scheduleBlockTick(pos, this, 1);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!com.nothatcher.vehicleforge.power.PowerGrid.cityPowered(world, pos)) {
            if (state.get(POWERED)) world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_LISTENERS);
            world.scheduleBlockTick(pos, this, 100);
            return;
        }
        if (!state.get(POWERED)) state = state.with(POWERED, true);
        int[] p = phaseAt(world.getTime(), state.get(FACING).getAxis());
        if (state.get(PHASE) != p[0] || world.getBlockState(pos) != state) world.setBlockState(pos, state.with(PHASE, p[0]), Block.NOTIFY_LISTENERS);
        world.scheduleBlockTick(pos, this, Math.max(1, p[1]));
    }
}
