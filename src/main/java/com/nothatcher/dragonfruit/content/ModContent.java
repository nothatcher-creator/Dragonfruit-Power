package com.nothatcher.dragonfruit.content;

import com.nothatcher.dragonfruit.DragonfruitMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Rarity;
import net.minecraft.world.Heightmap;

import java.util.List;

/** Ship, katanas, seastone, cuffs, Sea King. */
public final class ModContent {
    public static final Block SEASTONE_BLOCK = new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.CYAN).strength(4f, 12f).requiresTool().sounds(BlockSoundGroup.DEEPSLATE));

    public static final EntityType<ShipEntity> SHIP = FabricEntityTypeBuilder.<ShipEntity>create(SpawnGroup.MISC, ShipEntity::new)
            .dimensions(EntityDimensions.fixed(3.0f, 1.2f))
            .trackRangeBlocks(160)
            .build();

    public static final EntityType<SeaKingEntity> SEA_KING = FabricEntityTypeBuilder.<SeaKingEntity>createMob()
            .entityFactory(SeaKingEntity::new)
            .spawnGroup(SpawnGroup.WATER_CREATURE)
            .dimensions(EntityDimensions.fixed(2.4f, 2.4f))
            .trackRangeBlocks(160)
            .defaultAttributes(SeaKingEntity::createAttributes)
            .spawnRestriction(SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, SeaKingEntity::canSpawnHere)
            .build();

    public static final Item SEASTONE_ITEM = new BlockItem(SEASTONE_BLOCK, new Item.Settings());
    public static final Item SEASTONE_CUFFS = new SeastoneCuffsItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item SHIP_ITEM = new ShipItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final KatanaItem WHITE_KATANA = new KatanaItem(ToolMaterials.DIAMOND, 4, -2.2f,
            new Item.Settings().rarity(Rarity.RARE), 0xE8F4FF);
    public static final KatanaItem CRIMSON_KATANA = new KatanaItem(ToolMaterials.DIAMOND, 4, -2.2f,
            new Item.Settings().rarity(Rarity.RARE), 0xFF2A3A);
    public static final KatanaItem BLACK_KATANA = new KatanaItem(ToolMaterials.NETHERITE, 4, -2.3f,
            new Item.Settings().rarity(Rarity.EPIC).fireproof(), 0x8A4CFF);
    public static final List<KatanaItem> KATANAS = List.of(WHITE_KATANA, CRIMSON_KATANA, BLACK_KATANA);
    public static final Item SEA_KING_SPAWN_EGG = new SpawnEggItem(SEA_KING, 0x0F4C5C, 0xFF7A1A, new Item.Settings());

    private ModContent() {}

    public static void register() {
        Registry.register(Registries.BLOCK, DragonfruitMod.id("seastone"), SEASTONE_BLOCK);
        Registry.register(Registries.ITEM, DragonfruitMod.id("seastone"), SEASTONE_ITEM);
        Registry.register(Registries.ITEM, DragonfruitMod.id("seastone_cuffs"), SEASTONE_CUFFS);
        Registry.register(Registries.ITEM, DragonfruitMod.id("pirate_ship"), SHIP_ITEM);
        Registry.register(Registries.ITEM, DragonfruitMod.id("white_katana"), WHITE_KATANA);
        Registry.register(Registries.ITEM, DragonfruitMod.id("crimson_katana"), CRIMSON_KATANA);
        Registry.register(Registries.ITEM, DragonfruitMod.id("black_katana"), BLACK_KATANA);
        Registry.register(Registries.ITEM, DragonfruitMod.id("sea_king_spawn_egg"), SEA_KING_SPAWN_EGG);

        Registry.register(Registries.ENTITY_TYPE, DragonfruitMod.id("pirate_ship"), SHIP);
        Registry.register(Registries.ENTITY_TYPE, DragonfruitMod.id("sea_king"), SEA_KING);

        // Sea Kings live in deep oceans (canSpawnHere keeps them rare).
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_DEEP_OCEAN), SpawnGroup.WATER_CREATURE, SEA_KING, 2, 1, 1);
    }

    public static List<Item> creativeItems() {
        return List.of(SHIP_ITEM, WHITE_KATANA, CRIMSON_KATANA, BLACK_KATANA, SEASTONE_ITEM, SEASTONE_CUFFS, SEA_KING_SPAWN_EGG);
    }
}
