package com.nothatcher.dragonfruit;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.Map;

/** Dragonfruits are rare treasure: shipwrecks, buried treasure, dungeons, temples, end cities. */
public final class ModLoot {
    private static final Map<Identifier, Float> CHANCES = Map.of(
            LootTables.BURIED_TREASURE_CHEST, 0.20f,
            LootTables.SHIPWRECK_TREASURE_CHEST, 0.12f,
            LootTables.SIMPLE_DUNGEON_CHEST, 0.06f,
            LootTables.DESERT_PYRAMID_CHEST, 0.06f,
            LootTables.JUNGLE_TEMPLE_CHEST, 0.08f,
            LootTables.END_CITY_TREASURE_CHEST, 0.15f,
            LootTables.ANCIENT_CITY_CHEST, 0.10f
    );

    private ModLoot() {}

    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (!source.isBuiltin()) return;
            Float chance = CHANCES.get(id);
            if (chance == null) return;
            LootPool.Builder pool = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(RandomChanceLootCondition.builder(chance));
            for (DragonfruitItem item : ModItems.FRUITS.values()) {
                pool.with(ItemEntry.builder(item).weight(1));
            }
            tableBuilder.pool(pool);
        });
    }
}
