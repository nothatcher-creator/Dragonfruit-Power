package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** A manhole cover you can lift off (use it) to climb down the ladder into the sewers, and drag back over. */
public class ManholeBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.of("open");
    private static final VoxelShape RIM = VoxelShapes.union(createCuboidShape(0, 0, 0, 16, 16, 2), createCuboidShape(0, 0, 14, 16, 16, 16),
            createCuboidShape(0, 0, 2, 2, 16, 14), createCuboidShape(14, 0, 2, 16, 16, 14));

    public ManholeBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(OPEN);
    }

    @Override
    public ActionResult onUse(BlockState st, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
        if (!w.isClient) {
            w.setBlockState(pos, st.cycle(OPEN), Block.NOTIFY_ALL);
            w.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1f, 0.6f);
            w.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.5f, 0.5f);
        }
        return ActionResult.success(w.isClient);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return st.get(OPEN) ? RIM : VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return st.get(OPEN) ? RIM : VoxelShapes.fullCube();
    }

    @Override
    public boolean isTransparent(BlockState st, BlockView w, BlockPos pos) {
        return st.get(OPEN);
    }
}
