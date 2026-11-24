package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A pothole: a road block worn down to any height, like layers of snow. 7 layers is a shallow dip, 1 layer is a
 * deep hole. Placing another pothole on one fills it up a layer; using it with an empty hand digs it a layer
 * deeper (and back round to shallow).
 */
public class PotholeBlock extends Block {
    public static final IntProperty LAYERS = Properties.LAYERS;
    private static final VoxelShape[] SHAPES = new VoxelShape[9];

    static {
        for (int i = 1; i <= 8; i++) SHAPES[i] = createCuboidShape(0, 0, 0, 16, i * 2, 16);
        SHAPES[0] = SHAPES[1];
    }

    public PotholeBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(LAYERS, 5));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(LAYERS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return SHAPES[st.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return SHAPES[st.get(LAYERS)];
    }

    @Override
    public VoxelShape getSidesShape(BlockState st, BlockView w, BlockPos pos) {
        return SHAPES[st.get(LAYERS)];
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return SHAPES[st.get(LAYERS)];
    }

    @Override
    public boolean hasSidedTransparency(BlockState st) {
        return true;
    }

    @Override
    public boolean canPathfindThrough(BlockState st, BlockView w, BlockPos pos, NavigationType type) {
        return type == NavigationType.LAND && st.get(LAYERS) >= 5;
    }

    @Override
    public boolean canReplace(BlockState st, ItemPlacementContext ctx) {
        return ctx.getStack().isOf(asItem()) && st.get(LAYERS) < 8 && (!ctx.canReplaceExisting() || ctx.getSide() == net.minecraft.util.math.Direction.UP);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState here = ctx.getWorld().getBlockState(ctx.getBlockPos());
        if (here.isOf(this)) return here.with(LAYERS, Math.min(8, here.get(LAYERS) + 1));
        return getDefaultState();
    }

    @Override
    public ActionResult onUse(BlockState st, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
        if (!p.getStackInHand(hand).isEmpty()) return ActionResult.PASS;
        if (!w.isClient) {
            int l = st.get(LAYERS) - 1;
            w.setBlockState(pos, st.with(LAYERS, l < 1 ? 7 : l), Block.NOTIFY_ALL);
            w.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 0.8f, 0.9f);
        }
        return ActionResult.success(w.isClient);
    }
}
