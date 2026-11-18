package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.interior.RvDoorBlock;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModRegistry {
    public static final Map<String, EntityType<VehicleEntity>> ENTITY_TYPES = new LinkedHashMap<>();
    public static final Map<EntityType<?>, VehicleType> TYPE_OF = new HashMap<>();
    public static final Map<String, VehicleItem> ITEMS = new LinkedHashMap<>();
    public static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    public static final String[] SOUND_IDS = {
            "engine_car", "engine_truck", "engine_bike", "engine_tank", "engine_heli", "engine_prop", "engine_jet",
            "engine_rocket", "engine_train", "engine_boat",
            "horn_car", "horn_truck", "bike_bell", "horn_train", "horn_boat",
            "siren", "cannon", "rocket_launch", "gunshot", "water_spray",
            "skid", "crash", "fuel_fill",
            "ostrich_call", "time_warp", "shrink", "ice_cream_jingle", "engine_electric", "burner", "ufo_hum", "compactor"
    };

    public static final Block RV_DOOR = new RvDoorBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.BROWN).strength(-1f, 3600000f).sounds(BlockSoundGroup.WOOD).nonOpaque());
    public static Item CARRIAGE_ITEM;
    public static Item FUEL_CAN;

    private ModRegistry() {}

    public static void register() {
        VehicleTypes.init();
        for (String s : SOUND_IDS) {
            Identifier id = VehicleForge.id(s);
            SOUNDS.put(s, Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id)));
        }

        for (VehicleType t : VehicleTypes.ALL.values()) {
            EntityType<VehicleEntity> et = FabricEntityTypeBuilder.<VehicleEntity>create(SpawnGroup.MISC, VehicleEntity::new)
                    .dimensions(EntityDimensions.fixed(t.width, t.height))
                    .trackRangeBlocks(160)
                    .trackedUpdateRate(2)
                    .build();
            Registry.register(Registries.ENTITY_TYPE, VehicleForge.id(t.id), et);
            ENTITY_TYPES.put(t.id, et);
            TYPE_OF.put(et, t);
            VehicleItem item = new VehicleItem(t, new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
            Registry.register(Registries.ITEM, VehicleForge.id(t.id), item);
            ITEMS.put(t.id, item);
        }

        CARRIAGE_ITEM = Registry.register(Registries.ITEM, VehicleForge.id("train_carriage"), new Item(new Item.Settings().maxCount(16)));
        FUEL_CAN = Registry.register(Registries.ITEM, VehicleForge.id("fuel_can"), new FuelCanItem(new Item.Settings().maxCount(16)));
        Registry.register(Registries.BLOCK, VehicleForge.id("rv_door"), RV_DOOR);
        Registry.register(Registries.ITEM, VehicleForge.id("rv_door"), new BlockItem(RV_DOOR, new Item.Settings()));

        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ITEMS.get("sports_car")))
                .displayName(Text.translatable("itemGroup.vehicleforge.vehicles"))
                .entries((ctx, entries) -> {
                    for (VehicleItem item : ITEMS.values()) entries.add(item);
                    entries.add(CARRIAGE_ITEM);
                    entries.add(FUEL_CAN);
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, VehicleForge.id("vehicles"), group);
    }

    public static SoundEvent sound(String id) {
        return SOUNDS.get(id);
    }
}
