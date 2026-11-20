package com.nothatcher.vehicleforge.power;

import com.nothatcher.vehicleforge.block.CityExtras;
import com.nothatcher.vehicleforge.block.FacingShapeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Generator (burns coal, charcoal, lava or fuel cans), solar panel (daylight with open sky), wind turbine (always
 * turning), battery (charges from the grid, then powers it) and power pole (links things up to 16 blocks apart).
 * Live sources also give out a full redstone signal.
 */
public class PowerBlock extends FacingShapeBlock implements BlockEntityProvider {
    public enum Kind { GENERATOR, SOLAR, WIND, BATTERY, POLE }

    public static final BooleanProperty ACTIVE = Properties.POWERED;
    public static final IntProperty LEVEL = IntProperty.of("level", 0, 3);   // 0-3, battery charge bars
    public final Kind kind;

    public PowerBlock(Settings settings, VoxelShape shape, Kind kind) {
        super(settings.luminance(s -> s.get(Properties.POWERED) && kind == Kind.GENERATOR ? 6 : 0), shape, false);
        this.kind = kind;
        setDefaultState(getDefaultState().with(ACTIVE, false).with(LEVEL, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ACTIVE, LEVEL);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (type != CityExtras.POWER_BE) return null;
        return world.isClient ? (BlockEntityTicker<T>) (BlockEntityTicker<PowerBlockEntity>) PowerBlockEntity::clientTick
                : (BlockEntityTicker<T>) (BlockEntityTicker<PowerBlockEntity>) PowerBlockEntity::serverTick;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return kind != Kind.POLE;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return kind != Kind.POLE && state.get(ACTIVE) ? 15 : 0;
    }

    /** fuel value in ticks, or 0 */
    static int fuel(ItemStack s) {
        if (s.isOf(Items.COAL) || s.isOf(Items.CHARCOAL)) return 2400;
        if (s.isOf(Items.COAL_BLOCK)) return 24000;
        if (s.isOf(Items.LAVA_BUCKET)) return 20000;
        if (s.isOf(Items.BLAZE_ROD)) return 3600;
        if (Registries.ITEM.getId(s.getItem()).getPath().equals("fuel_can")) return 12000;
        return 0;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof PowerBlockEntity be)) return ActionResult.PASS;
        ItemStack held = player.getStackInHand(hand);
        switch (kind) {
            case GENERATOR -> {
                int f = fuel(held);
                if (f > 0 && be.fuel < PowerBlockEntity.MAX_FUEL) {
                    be.fuel = Math.min(PowerBlockEntity.MAX_FUEL, be.fuel + f);
                    if (held.isOf(Items.LAVA_BUCKET)) player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                    else if (!player.getAbilities().creativeMode) held.decrement(1);
                    be.markDirty();
                    world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.7f, 0.8f);
                }
                int pct = be.fuel * 100 / PowerBlockEntity.MAX_FUEL;
                player.sendMessage(Text.translatable(be.fuel > 0 ? "msg.vehicleforge.generator_fuel" : "msg.vehicleforge.generator_empty", pct)
                        .formatted(be.fuel > 0 ? Formatting.YELLOW : Formatting.GRAY), true);
            }
            case BATTERY -> player.sendMessage(Text.translatable("msg.vehicleforge.battery", be.charge * 100 / PowerBlockEntity.MAX_CHARGE).formatted(Formatting.YELLOW), true);
            default -> player.sendMessage(Text.translatable(PowerGrid.isPowered(world, pos, null) ? "msg.vehicleforge.grid_live" : "msg.vehicleforge.grid_dead")
                    .formatted(Formatting.GRAY), true);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) PowerGrid.remove(world, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
