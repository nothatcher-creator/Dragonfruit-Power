package com.nothatcher.vehicleforge.block;

import com.nothatcher.vehicleforge.power.PowerGrid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** Computers and TVs: switch them on and off (they need power when the city grid is down). */
public class ToggleProp extends FacingShapeBlock {
    public static final BooleanProperty LIT = Properties.LIT;

    public ToggleProp(Settings settings, VoxelShape shape) {
        super(settings.luminance(s -> s.get(Properties.LIT) ? 7 : 0), shape, false);
        setDefaultState(getDefaultState().with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LIT);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        boolean on = state.get(LIT);
        if (!on && !PowerGrid.cityPowered(world, pos)) {
            player.sendMessage(Text.translatable("msg.vehicleforge.no_power").formatted(Formatting.GRAY), true);
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.5f, 0.8f);
            return ActionResult.CONSUME;
        }
        world.setBlockState(pos, state.with(LIT, !on));
        world.playSound(null, pos, on ? SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF : SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, on ? 0.5f : 0.25f, on ? 0.6f : 2f);
        if (!on) world.scheduleBlockTick(pos, this, 40);
        return ActionResult.CONSUME;
    }

    /** switch off if the power goes */
    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(LIT)) return;
        if (!PowerGrid.cityPowered(world, pos)) world.setBlockState(pos, state.with(LIT, false));
        else world.scheduleBlockTick(pos, this, 40);
    }
}
