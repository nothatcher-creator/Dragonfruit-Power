package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The light a vehicle's headlights throw on the road ahead: invisible, no collision, and it removes itself a
 * moment after the beam moves on (or after a restart), so none are ever left behind.
 */
public class HeadlightBlock extends Block {
    public static final IntProperty LEVEL = IntProperty.of("level", 1, 15);
    private static final Map<ServerWorld, Map<Long, Long>> EXPIRY = new WeakHashMap<>();

    public HeadlightBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LEVEL, 15));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    /** light (or keep lit) this spot for a few more ticks; only into empty air */
    public void shine(ServerWorld w, BlockPos pos, int level) {
        BlockState here = w.getBlockState(pos);
        if (!here.isAir() && !here.isOf(this)) return;
        EXPIRY.computeIfAbsent(w, k -> new HashMap<>()).put(pos.asLong(), w.getTime() + 5);
        if (!here.isOf(this) || here.get(LEVEL) < level) {
            w.setBlockState(pos, getDefaultState().with(LEVEL, level), Block.NOTIFY_ALL);
            w.scheduleBlockTick(pos, this, 5);
        }
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
        Map<Long, Long> m = EXPIRY.get(world);
        Long until = m == null ? null : m.get(pos.asLong());
        long left = until == null ? 0 : until - world.getTime();
        if (left <= 0) {
            if (m != null) m.remove(pos.asLong());
            world.removeBlock(pos, false);
        } else world.scheduleBlockTick(pos, this, (int) Math.min(left, 20));
    }
}
