package com.nothatcher.dragonfruit.content;

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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Right-click on water to launch your ship. */
public class ShipItem extends Item {
    public ShipItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        BlockHitResult hit = raycast(world, user, RaycastContext.FluidHandling.ANY);
        if (hit.getType() == HitResult.Type.MISS) return TypedActionResult.pass(stack);
        if (!world.getFluidState(hit.getBlockPos()).isIn(FluidTags.WATER)) {
            user.sendMessage(Text.translatable("msg.dragonfruit.ship_water").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        if (!world.isClient) {
            ShipEntity ship = new ShipEntity(ModContent.SHIP, world);
            Vec3d p = hit.getPos();
            ship.refreshPositionAndAngles(p.x, p.y, p.z, user.getYaw(), 0);
            if (!world.isSpaceEmpty(ship, ship.getBoundingBox())) {
                user.sendMessage(Text.translatable("msg.dragonfruit.ship_space").formatted(Formatting.GRAY), true);
                return TypedActionResult.fail(stack);
            }
            world.spawnEntity(ship);
            world.playSound(null, p.x, p.y, p.z, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.NEUTRAL, 1.5f, 0.7f);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.dragonfruit.ship").formatted(Formatting.GRAY));
    }
}
