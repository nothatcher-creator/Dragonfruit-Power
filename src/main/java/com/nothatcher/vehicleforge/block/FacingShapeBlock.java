package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Street furniture with a facing and a custom shape: cones, hydrants, barriers, bins, lamp heads.
 * Shapes are given for a block facing north (z towards 0) and turned for the other directions.
 */
public class FacingShapeBlock extends HorizontalFacingBlock {
    private final VoxelShape north, east, south, west;
    private final boolean faceAway;

    /** faceAway: the front points the same way the player looks (lamp arms reach out); otherwise it faces the player */
    public FacingShapeBlock(Settings settings, VoxelShape northShape, boolean faceAway) {
        super(settings);
        this.faceAway = faceAway;
        this.north = northShape;
        this.east = Shapes.rotate(northShape, 1);
        this.south = Shapes.rotate(northShape, 2);
        this.west = Shapes.rotate(northShape, 3);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction d = ctx.getHorizontalPlayerFacing();
        return getDefaultState().with(FACING, faceAway ? d : d.getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case EAST -> east;
            case SOUTH -> south;
            case WEST -> west;
            default -> north;
        };
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}
