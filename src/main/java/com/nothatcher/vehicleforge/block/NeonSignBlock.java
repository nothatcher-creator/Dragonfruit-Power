package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Neon lettering: just the glowing tube letters, no board. Use it to type what it says and pick the colour, size
 * and whether it flickers. Hangs on a wall (or stands free) facing out; it lights up its surroundings.
 */
public class NeonSignBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    /** set by the client: opens the editing screen */
    public static Consumer<BlockPos> openEditor = p -> {};
    private static final VoxelShape[] SHAPES = {
            createCuboidShape(0, 2, 13, 16, 14, 16), createCuboidShape(0, 2, 0, 16, 14, 3),
            createCuboidShape(13, 2, 0, 16, 14, 16), createCuboidShape(0, 2, 0, 3, 14, 16)};

    public NeonSignBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(net.minecraft.state.property.Properties.LIT, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(FACING, net.minecraft.state.property.Properties.LIT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        return getDefaultState().with(FACING, side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite())
                .with(net.minecraft.state.property.Properties.LIT, com.nothatcher.vehicleforge.power.PowerGrid.cityPowered(ctx.getWorld(), ctx.getBlockPos()));
    }

    /** goes dark with the city grid unless something local powers it */
    @Override
    public boolean hasRandomTicks(BlockState st) {
        return true;
    }

    @Override
    public void randomTick(BlockState st, net.minecraft.server.world.ServerWorld w, BlockPos pos, net.minecraft.util.math.random.Random r) {
        boolean on = com.nothatcher.vehicleforge.power.PowerGrid.cityPowered(w, pos);
        if (on != st.get(net.minecraft.state.property.Properties.LIT)) w.setBlockState(pos, st.with(net.minecraft.state.property.Properties.LIT, on), Block.NOTIFY_LISTENERS);
    }

    @Override
    public BlockRenderType getRenderType(BlockState st) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return switch (st.get(FACING)) {
            case NORTH -> SHAPES[0];       // text faces north, so it sits against the south side
            case SOUTH -> SHAPES[1];
            case WEST -> SHAPES[2];
            default -> SHAPES[3];
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState st) {
        return new NeonSignBlockEntity(pos, st);
    }

    @Override
    public void onPlaced(World w, BlockPos pos, BlockState st, LivingEntity placer, ItemStack stack) {
        if (w.isClient && placer instanceof PlayerEntity) openEditor.accept(pos);
    }

    @Override
    public ActionResult onUse(BlockState st, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
        if (w.isClient) openEditor.accept(pos);
        return ActionResult.success(w.isClient);
    }

    @Override
    public BlockState rotate(BlockState st, BlockRotation r) {
        return st.with(FACING, r.rotate(st.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState st, BlockMirror m) {
        return st.rotate(m.getRotation(st.get(FACING)));
    }
}
