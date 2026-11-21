package com.nothatcher.outbreak;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The key to one particular car (Vehicle Forge vehicles parked in the city). */
public class CarKeyItem extends Item {
    public CarKeyItem(Settings settings) {
        super(settings);
    }

    public static ItemStack forVehicle(Entity v) {
        ItemStack s = new ItemStack(OutbreakItems.CAR_KEY);
        NbtCompound n = s.getOrCreateNbt();
        n.putUuid("Vehicle", v.getUuid());
        n.putString("VehicleType", v.getType().getTranslationKey());
        return s;
    }

    public static boolean fits(ItemStack s, Entity v) {
        if (!s.isOf(OutbreakItems.CAR_KEY)) return false;
        NbtCompound n = s.getNbt();
        return n != null && n.containsUuid("Vehicle") && n.getUuid("Vehicle").equals(v.getUuid());
    }

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound n = stack.getNbt();
        if (n != null && n.contains("VehicleType")) return Text.translatable("item.outbreak.car_key.named", Text.translatable(n.getString("VehicleType")));
        return super.getName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.outbreak.car_key.tip").formatted(Formatting.GRAY));
    }
}
