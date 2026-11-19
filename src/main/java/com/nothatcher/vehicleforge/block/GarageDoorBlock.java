package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
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
import net.minecraft.entity.LivingEntity;

/**
 * A 3x3 roll-up garage door. Nine blocks act as one: PART 0-8 is (column + 3 * row) from the bottom-left (lowest
 * x or z), AXIS is the direction the door runs along. Use any part to roll the whole door up or down.
 */
public class GarageDoorBlock extends Block {
    public static final IntProperty PART = IntProperty.of("part", 0, 8);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final VoxelShape PANEL_X = Block.createCuboidShape(0, 0, 7, 16, 16, 9);
    private static final VoxelShape PANEL_Z = Block.createCuboidShape(7, 0, 0, 9, 16, 16);
    private static final VoxelShape ROLL = Block.createCuboidShape(0, 12, 0, 16, 16, 16);

    public GarageDoorBlock(Settings s) {
        super(s);
        setDefaultState(getStateManager().getDefaultState().with(PART, 1).with(AXIS, Direction.Axis.X).with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(PART, AXIS, OPEN);
    }

    private static Direction along(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /** the bottom-left corner of the door this part belongs to */
    private static BlockPos origin(BlockState st, BlockPos pos) {
        int part = st.get(PART);
        return pos.offset(along(st.get(AXIS)), -(part % 3)).down(part / 3);
    }

    private static BlockPos partPos(BlockPos origin, Direction.Axis axis, int part) {
        return origin.offset(along(axis), part % 3).up(part / 3);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        if (st.get(OPEN)) return st.get(PART) >= 6 ? ROLL : VoxelShapes.empty();
        return st.get(AXIS) == Direction.Axis.X ? PANEL_X : PANEL_Z;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        if (st.get(OPEN)) return st.get(PART) >= 6 ? ROLL : VoxelShapes.empty();
        return st.get(AXIS) == Direction.Axis.X ? PANEL_X : PANEL_Z;
    }

    @Override
    public boolean canPathfindThrough(BlockState st, BlockView w, BlockPos pos, NavigationType type) {
        return st.get(OPEN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // the door runs across the way you're facing; the block you click becomes the bottom middle
        Direction.Axis axis = ctx.getHorizontalPlayerFacing().rotateYClockwise().getAxis();
        BlockPos origin = ctx.getBlockPos().offset(along(axis), -1);
        World w = ctx.getWorld();
        for (int i = 0; i < 9; i++) {
            BlockPos p = partPos(origin, axis, i);
            if (!w.getBlockState(p).canReplace(ctx) || !w.getWorldBorder().contains(p) || p.getY() >= w.getTopY()) return null;
        }
        return getDefaultState().with(AXIS, axis).with(PART, 1);
    }

    @Override
    public void onPlaced(World w, BlockPos pos, BlockState st, LivingEntity placer, ItemStack stack) {
        if (w.isClient) return;
        BlockPos o = origin(st, pos);
        for (int i = 0; i < 9; i++) if (i != 1) w.setBlockState(partPos(o, st.get(AXIS), i), st.with(PART, i), Block.NOTIFY_ALL);
    }

    @Override
    public ActionResult onUse(BlockState st, World w, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!w.isClient) toggle(w, st, pos);
        return ActionResult.success(w.isClient);
    }

    public static void toggle(World w, BlockState st, BlockPos pos) {
        boolean open = !st.get(OPEN);
        BlockPos o = origin(st, pos);
        for (int i = 0; i < 9; i++) {
            BlockPos p = partPos(o, st.get(AXIS), i);
            BlockState s = w.getBlockState(p);
            if (s.getBlock() instanceof GarageDoorBlock) w.setBlockState(p, s.with(OPEN, open), Block.NOTIFY_LISTENERS);
        }
        BlockPos mid = partPos(o, st.get(AXIS), 4);
        w.playSound(null, mid, open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1f, 0.55f);
        w.playSound(null, mid, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.8f, 0.6f);
    }

    @Override
    public void onBreak(World w, BlockPos pos, BlockState st, PlayerEntity player) {
        if (!w.isClient && !player.getAbilities().creativeMode) dropStack(w, pos, new ItemStack(this));
        super.onBreak(w, pos, st, player);
    }

    @Override
    public void onStateReplaced(BlockState st, World w, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(st, w, pos, newState, moved);
        if (w.isClient || newState.isOf(this)) return;
        // take the rest of the door with it
        BlockPos o = origin(st, pos);
        for (int i = 0; i < 9; i++) {
            BlockPos p = partPos(o, st.get(AXIS), i);
            if (p.equals(pos)) continue;
            BlockState s = w.getBlockState(p);
            if (s.isOf(this) && s.get(AXIS) == st.get(AXIS)) w.setBlockState(p, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
        }
    }
}
