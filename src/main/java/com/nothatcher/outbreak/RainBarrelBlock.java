package com.nothatcher.outbreak;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A rain barrel for the safehouse: out in the open it fills up when it rains (four levels). Fill a glass bottle
 * from it for bottled water, or a bucket when it's full. Water lasts after the taps run dry.
 */
public class RainBarrelBlock extends Block {
    public static final IntProperty LEVEL = IntProperty.of("level", 0, 3);
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 15, 14);

    public RainBarrelBlock(Settings s) {
        super(s);
        setDefaultState(getStateManager().getDefaultState().with(LEVEL, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
        b.add(LEVEL);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return state.get(LEVEL) < 3;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld w, BlockPos pos, Random r) {
        if (w.hasRain(pos.up()) && r.nextInt(3) == 0) w.setBlockState(pos, state.with(LEVEL, state.get(LEVEL) + 1));
    }

    @Override
    public void precipitationTick(BlockState state, World w, BlockPos pos, net.minecraft.world.biome.Biome.Precipitation p) {
        if (p == net.minecraft.world.biome.Biome.Precipitation.RAIN && state.get(LEVEL) < 3 && w.random.nextInt(4) == 0)
            w.setBlockState(pos, state.with(LEVEL, state.get(LEVEL) + 1));
    }

    @Override
    public ActionResult onUse(BlockState state, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
        ItemStack held = p.getStackInHand(hand);
        int level = state.get(LEVEL);
        if (held.isOf(Items.GLASS_BOTTLE) && level > 0) {
            if (!w.isClient) {
                if (!p.getAbilities().creativeMode) held.decrement(1);
                p.getInventory().offerOrDrop(new ItemStack(OutbreakItems.BOTTLED_WATER));
                w.setBlockState(pos, state.with(LEVEL, level - 1));
                w.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1f, 1f);
            }
            return ActionResult.success(w.isClient);
        }
        if (held.isOf(Items.BUCKET) && level == 3) {
            if (!w.isClient) {
                if (!p.getAbilities().creativeMode) held.decrement(1);
                p.getInventory().offerOrDrop(new ItemStack(Items.WATER_BUCKET));
                w.setBlockState(pos, state.with(LEVEL, 0));
                w.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1f, 1f);
            }
            return ActionResult.success(w.isClient);
        }
        if (!w.isClient) p.sendMessage(net.minecraft.text.Text.translatable("msg.outbreak.rain_barrel_" + level), true);
        return ActionResult.success(w.isClient);
    }
}
