package com.nothatcher.vehicleforge.block;

import com.nothatcher.vehicleforge.power.PowerGrid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;

/**
 * A light on the city grid (street lamps): lit while the mains are on, or when your own generator, solar panel or
 * turbine powers it. Rechecks now and then (random ticks); the blackout itself switches them all at once.
 */
public class GridLampBlock extends FacingShapeBlock {
    public GridLampBlock(Settings settings, VoxelShape shape, boolean faceAway, int light) {
        super(settings.luminance(s -> s.get(Properties.LIT) ? light : 0), shape, faceAway);
        setDefaultState(getDefaultState().with(Properties.LIT, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.LIT);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean on = PowerGrid.cityPowered(world, pos);
        if (on != state.get(Properties.LIT)) world.setBlockState(pos, state.with(Properties.LIT, on), Block.NOTIFY_LISTENERS);
    }
}
