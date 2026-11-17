package com.nothatcher.dragonfruit;

import com.nothatcher.dragonfruit.power.PowerManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DragonfruitItem extends Item {
    public final FruitType type;

    public DragonfruitItem(FruitType type, Settings settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            PowerManager.grant(player, type);
        }
        return result;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.dragonfruit." + type.id).formatted(Formatting.GRAY, Formatting.ITALIC));
        for (int i = 0; i < type.abilities.length; i++) {
            final int color = type.color;
            tooltip.add(Text.literal(" • ").append(type.abilityName(i)).styled(s -> s.withColor(color)));
        }
        tooltip.add(Text.translatable("tooltip.dragonfruit.warning").formatted(Formatting.DARK_AQUA));
    }
}
