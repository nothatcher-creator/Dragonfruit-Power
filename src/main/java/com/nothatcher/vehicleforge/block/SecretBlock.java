package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Looks exactly like an ordinary bookshelf / quartz floor / stone floor. Use it and it slides away (with any
 * stacked secret blocks above or below it), leaving a gap; use the sill it leaves to slide it back.
 */
public class SecretBlock extends Block {
    public static final BooleanProperty OPEN = Properties.OPEN;
    private static final VoxelShape SILL = Block.createCuboidShape(0, 0, 0, 16, 1, 16);

    public SecretBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return st.get(OPEN) ? SILL : VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return st.get(OPEN) ? VoxelShapes.empty() : VoxelShapes.fullCube();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        boolean open = !state.get(OPEN);
        if (!open && blocked(world, pos)) {                                    // someone's standing in the gap
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.6f, 0.6f);
            return ActionResult.CONSUME;
        }
        toggle(world, pos, open);
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN}) {
            BlockPos p = pos.offset(d);
            for (int i = 0; i < 2 && world.getBlockState(p).isOf(this); i++, p = p.offset(d)) toggle(world, p, open);
        }
        world.playSound(null, pos, open ? SoundEvents.BLOCK_PISTON_CONTRACT : SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.6f, 0.7f);
        world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.3f, 0.5f);
        return ActionResult.CONSUME;
    }

    /** anything in the column of secret blocks that would get shut inside it */
    private boolean blocked(World w, BlockPos pos) {
        BlockPos lo = pos, hi = pos;
        for (int i = 0; i < 2 && w.getBlockState(lo.down()).isOf(this); i++) lo = lo.down();
        for (int i = 0; i < 2 && w.getBlockState(hi.up()).isOf(this); i++) hi = hi.up();
        return !w.getOtherEntities(null, new net.minecraft.util.math.Box(lo.getX(), lo.getY(), lo.getZ(), hi.getX() + 1, hi.getY() + 1, hi.getZ() + 1).contract(0.05),
                e -> e.isAlive() && !e.isSpectator()).isEmpty();
    }

    private void toggle(World w, BlockPos p, boolean open) {
        w.setBlockState(p, w.getBlockState(p).with(OPEN, open));
    }
}
