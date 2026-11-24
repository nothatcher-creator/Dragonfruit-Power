package com.nothatcher.vehicleforge.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * A smooth 45 degree slope that rises towards FACING: proper sloped roofs and ramps, in place of stair steps.
 * Walks and collides like stairs.
 */
public class SlopeBlock extends FacingShapeBlock {
    private static final VoxelShape NORTH = VoxelShapes.union(box(0, 0, 0, 16, 8, 16), box(0, 8, 0, 16, 16, 8));

    public SlopeBlock(Settings s) {
        super(s, NORTH, true);
    }

    private static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return net.minecraft.block.Block.createCuboidShape(x0, y0, z0, x1, y1, z1);
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    public static Direction up(BlockState s) {
        return s.get(FACING);
    }
}
