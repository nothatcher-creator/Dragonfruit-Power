package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class OutbreakItems {
    public static final List<Item> ALL = new ArrayList<>();

    // ammo
    public static final Item AMMO_9MM = reg("ammo_9mm", new Item(new Item.Settings()));
    public static final Item AMMO_SHELLS = reg("ammo_shells", new Item(new Item.Settings()));
    public static final Item AMMO_RIFLE = reg("ammo_rifle", new Item(new Item.Settings()));

    // guns: sound, ammo, mag, interval, reload, pellets, damage, spread, range, recoil, noise, auto, scope
    public static final GunItem PISTOL = reg("pistol", new GunItem(new Item.Settings(), "pistol_fire", () -> AMMO_9MM,
            15, 5, 30, 1, 7f, 1.4f, 60, 2.2f, 48, false, false));
    public static final GunItem SMG = reg("smg", new GunItem(new Item.Settings(), "smg_fire", () -> AMMO_9MM,
            30, 2, 40, 1, 5f, 3.2f, 50, 1.1f, 48, true, false));
    public static final GunItem SHOTGUN = reg("shotgun", new GunItem(new Item.Settings(), "shotgun_fire", () -> AMMO_SHELLS,
            6, 16, 60, 8, 3f, 5.5f, 26, 6.5f, 64, false, false));
    public static final GunItem HUNTING_RIFLE = reg("hunting_rifle", new GunItem(new Item.Settings(), "rifle_fire", () -> AMMO_RIFLE,
            5, 24, 50, 1, 19f, 0.6f, 140, 7f, 80, false, true));
    public static final GunItem ASSAULT_RIFLE = reg("assault_rifle", new GunItem(new Item.Settings(), "rifle_fire", () -> AMMO_RIFLE,
            30, 3, 45, 1, 7.5f, 1.9f, 90, 1.5f, 72, true, false));

    // melee
    public static final Item BASEBALL_BAT = reg("baseball_bat", new SwordItem(ToolMaterials.WOOD, 4, -2.7f, new Item.Settings().maxDamage(160)));
    public static final Item SPIKED_BAT = reg("spiked_bat", new SwordItem(ToolMaterials.WOOD, 6, -2.8f, new Item.Settings().maxDamage(220)));
    public static final Item CROWBAR = reg("crowbar", new SwordItem(ToolMaterials.IRON, 2, -2.5f, new Item.Settings().maxDamage(700)));
    public static final Item MACHETE = reg("machete", new SwordItem(ToolMaterials.IRON, 4, -2.3f, new Item.Settings().maxDamage(380)));
    public static final Item SLEDGEHAMMER = reg("sledgehammer", new SwordItem(ToolMaterials.IRON, 8, -3.3f, new Item.Settings().maxDamage(300)));

    // armour
    public static final Item POLICE_VEST = reg("police_vest", new ArmorItem(OutbreakArmor.POLICE, ArmorItem.Type.CHESTPLATE, new Item.Settings()));
    public static final Item MILITARY_VEST = reg("military_vest", new ArmorItem(OutbreakArmor.MILITARY, ArmorItem.Type.CHESTPLATE, new Item.Settings()));
    public static final Item MILITARY_HELMET = reg("military_helmet", new ArmorItem(OutbreakArmor.MILITARY, ArmorItem.Type.HELMET, new Item.Settings()));
    public static final Item RIOT_HELMET = reg("riot_helmet", new ArmorItem(OutbreakArmor.RIOT, ArmorItem.Type.HELMET, new Item.Settings()));

    // medical
    public static final Item BANDAGE = reg("bandage", new MedItem(new Item.Settings().maxCount(16), MedItem.Kind.BANDAGE));
    public static final Item PAINKILLERS = reg("painkillers", new MedItem(new Item.Settings().maxCount(16), MedItem.Kind.PAINKILLERS));
    public static final Item ANTIBIOTICS = reg("antibiotics", new MedItem(new Item.Settings().maxCount(8), MedItem.Kind.ANTIBIOTICS));
    public static final Item FIRST_AID_KIT = reg("first_aid_kit", new MedItem(new Item.Settings().maxCount(4), MedItem.Kind.FIRST_AID));

    // food
    public static final Item CANNED_BEANS = reg("canned_beans", new Item(new Item.Settings().food(
            new FoodComponent.Builder().hunger(8).saturationModifier(0.6f).build())));
    public static final Item CANNED_SOUP = reg("canned_soup", new Item(new Item.Settings().food(
            new FoodComponent.Builder().hunger(7).saturationModifier(0.7f).build())));
    public static final Item CHIPS = reg("chips", new Item(new Item.Settings().food(
            new FoodComponent.Builder().hunger(3).saturationModifier(0.2f).snack().build())));
    public static final Item ENERGY_BAR = reg("energy_bar", new Item(new Item.Settings().food(
            new FoodComponent.Builder().hunger(5).saturationModifier(0.8f).snack()
                    .statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 0), 1f).build())));
    public static final Item BOTTLED_WATER = reg("bottled_water", new Item(new Item.Settings().maxCount(16).food(
            new FoodComponent.Builder().hunger(1).saturationModifier(0.3f).alwaysEdible().build())));

    public static final Item BACKPACK = reg("backpack", new BackpackItem(new Item.Settings()));

    private OutbreakItems() {}

    private static <T extends Item> T reg(String name, T item) {
        Registry.register(Registries.ITEM, Outbreak.id(name), item);
        ALL.add(item);
        return item;
    }

    public static void register() {
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(PISTOL))
                .displayName(Text.translatable("itemGroup.outbreak"))
                .entries((ctx, entries) -> {
                    for (Item i : ALL) entries.add(i instanceof GunItem g ? g.loaded() : new ItemStack(i));
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, Outbreak.id("outbreak"), group);
    }
}
