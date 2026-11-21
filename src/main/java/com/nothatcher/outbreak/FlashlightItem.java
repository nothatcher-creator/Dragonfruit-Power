package com.nothatcher.outbreak;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A flashlight: use it to switch it on or off. While it's on and in either hand, it lights up whatever you point it
 * at (see Flashlights). The durability bar is the battery; sneak and use it with a car battery in your other hand to
 * recharge, or repair it with one on an anvil.
 */
public class FlashlightItem extends Item {
    public FlashlightItem(Settings settings) {
        super(settings);
    }

    public static boolean isOn(ItemStack s) {
        return s.getItem() instanceof FlashlightItem && s.hasNbt() && s.getNbt().getBoolean("On") && s.getDamage() < s.getMaxDamage() - 1;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack st = player.getStackInHand(hand);
        ItemStack other = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
        if (player.isSneaking() && other.isOf(OutbreakItems.CAR_BATTERY) && st.getDamage() > 0) {
            if (!world.isClient) {
                st.setDamage(0);
                if (!player.getAbilities().creativeMode) other.decrement(1);
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 0.5f, 1.8f);
                player.sendMessage(Text.translatable("msg.outbreak.flashlight_charged").formatted(Formatting.GRAY), true);
            }
            return TypedActionResult.success(st, world.isClient);
        }
        if (!world.isClient) {
            boolean on = !st.getOrCreateNbt().getBoolean("On");
            if (on && st.getDamage() >= st.getMaxDamage() - 1) {
                player.sendMessage(Text.translatable("msg.outbreak.flashlight_dead").formatted(Formatting.GRAY), true);
                return TypedActionResult.fail(st);
            }
            st.getOrCreateNbt().putBoolean("On", on);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.4f, on ? 1.9f : 1.5f);
        }
        return TypedActionResult.success(st, world.isClient);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(OutbreakItems.CAR_BATTERY);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int pct = Math.round(100f * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage());
        tooltip.add(Text.translatable(isOn(stack) ? "tooltip.outbreak.flashlight_on" : "tooltip.outbreak.flashlight_off", pct).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.outbreak.flashlight").formatted(Formatting.DARK_GRAY));
    }
}
