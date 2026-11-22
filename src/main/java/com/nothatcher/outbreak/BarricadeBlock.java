package com.nothatcher.outbreak;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A barricaded door, window or opening. It takes the place of whatever was there (a door, a pane of glass, or
 * nothing) and keeps it in its block entity, which draws it; the block's own model is the boards or the sheet
 * metal, nailed on the side it was barricaded from. Doors take two of these (PART lower and upper), and the lower
 * one holds the boards and their health for both.
 */
public class BarricadeBlock extends BlockWithEntity {
    public enum Part implements StringIdentifiable {
        SINGLE, LOWER, UPPER;

        @Override
        public String asString() {
            return name().toLowerCase();
        }
    }

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty BOARDS = IntProperty.of("boards", 0, Barricades.MAX_BOARDS);
    public static final BooleanProperty METAL = BooleanProperty.of("metal");
    public static final EnumProperty<Part> PART = EnumProperty.of("part", Part.class);

    public BarricadeBlock(Settings s) {
        super(s);
        setDefaultState(getStateManager().getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH)
                .with(BOARDS, 1).with(METAL, false).with(PART, Part.SINGLE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(FACING, BOARDS, METAL, PART);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BarricadeBlockEntity(pos, state);
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) player.sendMessage(Barricades.describe(world, pos), true);
        return ActionResult.SUCCESS;
    }

    /** a creative player breaking one half of a door barricade takes the whole door with it */
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        Part part = state.get(PART);
        if (part == Part.SINGLE || world.isClient) return;
        BlockPos other = part == Part.LOWER ? pos.up() : pos.down();
        if (world.getBlockState(other).getBlock() instanceof BarricadeBlock)
            world.setBlockState(other, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(net.minecraft.item.Items.OAK_PLANKS);
    }
}
