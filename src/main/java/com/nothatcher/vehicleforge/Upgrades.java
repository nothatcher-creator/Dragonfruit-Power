package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleType.Category;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Zomboid-style vehicle add-ons and trailers. Right-click a road vehicle with one to fit it.
 * Upgrades: armour plating (takes 40% less damage), a spiked bumper (rams hit twice as hard), window bars
 * (zombies do half damage), a roof rack (two more rows of storage) and a snow plow (shoves things aside harder).
 * Trailers hitch to the back and follow it: cargo (three more rows), camper (sneak + right-click to go in) and
 * a boat trailer. Using a trailer on a vehicle that already has that trailer unhitches it.
 */
public final class Upgrades {
    public static final int ARMOR = 1, SPIKES = 2, BARS = 4, RACK = 8, PLOW = 16;
    public static final String[] UPGRADE_IDS = {"armor_plating", "spiked_bumper", "window_bars", "roof_rack", "snow_plow"};
    public static final String[] TRAILER_IDS = {"cargo_trailer", "camper_trailer", "boat_trailer"};
    public static final List<Item> ITEMS = new ArrayList<>();

    public static class Part extends Item {
        public final int upgrade, trailer;

        Part(int upgrade, int trailer, Settings s) {
            super(s);
            this.upgrade = upgrade;
            this.trailer = trailer;
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
            tooltip.add(Text.translatable(getTranslationKey() + ".tip").formatted(Formatting.GRAY));
        }
    }

    private Upgrades() {}

    static void register() {
        for (int i = 0; i < UPGRADE_IDS.length; i++)
            ITEMS.add(Registry.register(Registries.ITEM, VehicleForge.id(UPGRADE_IDS[i]), new Part(1 << i, 0, new Item.Settings().maxCount(4))));
        for (int i = 0; i < TRAILER_IDS.length; i++)
            ITEMS.add(Registry.register(Registries.ITEM, VehicleForge.id(TRAILER_IDS[i]), new Part(0, i + 1, new Item.Settings().maxCount(1))));
    }

    /** road vehicles take upgrades and trailers */
    public static boolean fits(VehicleType t) {
        return switch (t.category) {
            case CAR, TRUCK, RV, MILITARY -> !t.buildable;
            default -> false;
        };
    }

    /** how far behind the vehicle's centre a trailer's middle sits, and how long it is */
    public static double trailerLength(int trailer) {
        return trailer == 2 ? 5.0 : trailer == 3 ? 5.4 : 3.6;
    }

    static boolean isGround(Category c) {
        return c == Category.CAR || c == Category.TRUCK || c == Category.RV || c == Category.MILITARY;
    }
}
