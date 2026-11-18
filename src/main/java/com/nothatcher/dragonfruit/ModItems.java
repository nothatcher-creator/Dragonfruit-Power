package com.nothatcher.dragonfruit;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final Map<FruitType, DragonfruitItem> FRUITS = new EnumMap<>(FruitType.class);

    private ModItems() {}

    public static void register() {
        for (FruitType type : FruitType.values()) {
            if (type == FruitType.NONE) continue;
            DragonfruitItem item = new DragonfruitItem(type, new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .food(new FoodComponent.Builder()
                            .hunger(4)
                            .saturationModifier(0.6f)
                            .alwaysEdible()
                            .build()));
            Registry.register(Registries.ITEM, DragonfruitMod.id(type.id + "_dragonfruit"), item);
            FRUITS.put(type, item);
        }

        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(FRUITS.get(FruitType.RUBBER)))
                .displayName(Text.translatable("itemGroup.dragonfruit.fruits"))
                .entries((context, entries) -> {
                    for (DragonfruitItem item : FRUITS.values()) entries.add(item);
                    for (Item item : com.nothatcher.dragonfruit.content.ModContent.creativeItems()) entries.add(item);
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, DragonfruitMod.id("fruits"), group);
    }
}
