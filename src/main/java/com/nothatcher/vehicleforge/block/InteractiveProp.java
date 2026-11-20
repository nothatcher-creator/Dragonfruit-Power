package com.nothatcher.vehicleforge.block;

import com.nothatcher.vehicleforge.VehicleForge;
import com.nothatcher.vehicleforge.power.PowerGrid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * Props that do something when used: vending machines and ATMs pay out until they run dry, the water cooler fills
 * bottles, the stove cooks whatever raw food you're holding, and the parking meter has opinions.
 */
public class InteractiveProp extends FacingShapeBlock {
    public enum Kind { VENDING, ATM, WATER, STOVE, METER }

    public static final IntProperty STOCK = IntProperty.of("stock", 0, 4);
    private final Kind kind;

    public InteractiveProp(Settings settings, VoxelShape shape, Kind kind) {
        super(settings, shape, false);
        this.kind = kind;
        setDefaultState(getDefaultState().with(STOCK, 4));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(STOCK);
    }

    private static void tell(PlayerEntity p, String key) {
        p.sendMessage(Text.translatable(key).formatted(Formatting.GRAY), true);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        ServerWorld sw = (ServerWorld) world;
        Direction front = state.get(FACING);
        ItemStack held = player.getStackInHand(hand);
        switch (kind) {
            case VENDING, ATM -> {
                if (!PowerGrid.cityPowered(world, pos)) {
                    tell(player, "msg.vehicleforge.no_power");
                    return ActionResult.CONSUME;
                }
                int stock = state.get(STOCK);
                if (stock == 0) {
                    tell(player, kind == Kind.ATM ? "msg.vehicleforge.atm_empty" : "msg.vehicleforge.sold_out");
                    return ActionResult.CONSUME;
                }
                world.setBlockState(pos, state.with(STOCK, stock - 1));
                if (kind == Kind.ATM) {
                    dropFront(world, pos, front, new ItemStack(Items.EMERALD, 1 + world.random.nextInt(3)));
                    world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.BLOCKS, 0.4f, 1.6f);
                    tell(player, "msg.vehicleforge.atm");
                } else {
                    LootTable t = sw.getServer().getLootManager().getLootTable(VehicleForge.id("props/vending"));
                    for (ItemStack s : t.generateLoot(new LootContextParameterSet.Builder(sw).add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                            .build(LootContextTypes.CHEST))) dropFront(world, pos, front, s);
                    world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.6f, 1.4f);
                }
            }
            case WATER -> {
                if (held.isOf(Items.GLASS_BOTTLE)) {
                    held.decrement(1);
                    player.getInventory().offerOrDrop(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER));
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1f, 1f);
                } else {
                    player.getHungerManager().add(1, 0.2f);
                    world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.8f, 1.1f);
                    world.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundCategory.BLOCKS, 0.6f, 1.2f);
                }
            }
            case STOVE -> {
                if (!PowerGrid.cityPowered(world, pos)) {
                    tell(player, "msg.vehicleforge.no_power");
                    return ActionResult.CONSUME;
                }
                var match = world.getRecipeManager().getFirstMatch(RecipeType.SMOKING, new SimpleInventory(held.copyWithCount(1)), world);
                if (held.isEmpty() || match.isEmpty()) {
                    tell(player, "msg.vehicleforge.stove_hint");
                    return ActionResult.CONSUME;
                }
                ItemStack out = match.get().getOutput(world.getRegistryManager()).copy();
                held.decrement(1);
                player.getInventory().offerOrDrop(out);
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 1.6f);
                world.playSound(null, pos, SoundEvents.BLOCK_SMOKER_SMOKE, SoundCategory.BLOCKS, 0.8f, 1f);
                sw.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 8, 0.25, 0.05, 0.25, 0.01);
            }
            case METER -> tell(player, world.random.nextInt(4) == 0 ? "msg.vehicleforge.meter_paid" : "msg.vehicleforge.meter_expired");
        }
        return ActionResult.CONSUME;
    }

    private static void dropFront(World w, BlockPos pos, Direction front, ItemStack s) {
        Block.dropStack(w, pos.offset(front), s);
    }
}
