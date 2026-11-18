package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleType.Category;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Right-click to place the vehicle where you're looking (boats on water, trains on rails). */
public class VehicleItem extends Item {
    public final VehicleType type;

    public VehicleItem(VehicleType type, Settings settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean water = type.needsWater();
        BlockHitResult hit = raycast(world, user, water ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE);
        if (hit.getType() == HitResult.Type.MISS) return TypedActionResult.pass(stack);
        BlockPos bp = hit.getBlockPos();
        Vec3d at = hit.getPos();
        if (water && !world.getFluidState(bp).isIn(FluidTags.WATER)) {
            user.sendMessage(Text.translatable("msg.vehicleforge.needs_water").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        if (type.category == Category.TRAIN) {
            BlockPos rail = world.getBlockState(bp).getBlock() instanceof AbstractRailBlock ? bp
                    : world.getBlockState(bp.up()).getBlock() instanceof AbstractRailBlock ? bp.up() : null;
            if (rail == null) {
                user.sendMessage(Text.translatable("msg.vehicleforge.needs_rail").formatted(Formatting.GRAY), true);
                return TypedActionResult.fail(stack);
            }
            at = new Vec3d(rail.getX() + 0.5, rail.getY(), rail.getZ() + 0.5);
        }
        if (!world.isClient) {
            VehicleEntity v = ModRegistry.ENTITY_TYPES.get(type.id).create(world);
            if (v == null) return TypedActionResult.fail(stack);
            float yaw = user.getYaw();
            if (type.category == Category.TRAIN) yaw = Math.round(yaw / 90f) * 90f;
            v.refreshPositionAndAngles(at.x, at.y, at.z, yaw, 0);
            if (!world.isSpaceEmpty(v, v.getBoundingBox().contract(0.05))) {
                user.sendMessage(Text.translatable("msg.vehicleforge.no_room").formatted(Formatting.GRAY), true);
                return TypedActionResult.fail(stack);
            }
            world.spawnEntity(v);
            world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.NEUTRAL, 0.4f, 1.6f);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.vehicleforge." + type.category.name().toLowerCase()).formatted(Formatting.GRAY));
        if (type.weapon != VehicleType.Weapon.NONE) {
            tooltip.add(Text.translatable("tooltip.vehicleforge.weapon." + type.weapon.name().toLowerCase()).formatted(Formatting.RED));
        }
        if (type.interiorRoom) tooltip.add(Text.translatable("tooltip.vehicleforge.room").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.vehicleforge.seats", type.seats.size()).formatted(Formatting.DARK_GRAY));
        if (type.usesFuel()) tooltip.add(Text.translatable("tooltip.vehicleforge.care").formatted(Formatting.DARK_GRAY));
    }
}
