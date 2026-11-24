package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
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
    public static final Item AMMO_MAGNUM = reg("ammo_magnum", new Item(new Item.Settings()));

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
    public static final GunItem SILENCED_PISTOL = reg("silenced_pistol", new GunItem(new Item.Settings(), "silenced_fire", () -> AMMO_9MM,
            12, 6, 34, 1, 6f, 1.6f, 50, 1.6f, 10, false, false));
    public static final GunItem REVOLVER = reg("revolver", new GunItem(new Item.Settings(), "revolver_fire", () -> AMMO_MAGNUM,
            6, 9, 55, 1, 13f, 1.1f, 70, 5f, 56, false, false));
    public static final GunItem DOUBLE_BARREL = reg("double_barrel", new GunItem(new Item.Settings(), "shotgun_fire", () -> AMMO_SHELLS,
            2, 6, 40, 9, 3.2f, 6.5f, 22, 8f, 64, false, false));
    public static final GunItem SNIPER_RIFLE = reg("sniper_rifle", new GunItem(new Item.Settings(), "sniper_fire", () -> AMMO_RIFLE,
            5, 30, 65, 1, 30f, 0.5f, 220, 9f, 96, false, true));
    public static final GunItem LIGHT_MACHINE_GUN = reg("light_machine_gun", new GunItem(new Item.Settings(), "rifle_fire", () -> AMMO_RIFLE,
            100, 2, 110, 1, 6.5f, 3.2f, 90, 1.3f, 80, true, false));

    // melee
    public static final Item BASEBALL_BAT = reg("baseball_bat", new SwordItem(ToolMaterials.WOOD, 4, -2.7f, new Item.Settings().maxDamage(160)));
    public static final Item SPIKED_BAT = reg("spiked_bat", new SwordItem(ToolMaterials.WOOD, 6, -2.8f, new Item.Settings().maxDamage(220)));
    public static final Item FLASHLIGHT = reg("flashlight", new FlashlightItem(new Item.Settings().maxCount(1).maxDamage(2400)));
    public static final Item CAR_KEY = reg("car_key", new CarKeyItem(new Item.Settings().maxCount(1)));
    public static final Item CROWBAR = reg("crowbar", new SwordItem(ToolMaterials.IRON, 2, -2.5f, new Item.Settings().maxDamage(700)));
    public static final Item MACHETE = reg("machete", new SwordItem(ToolMaterials.IRON, 4, -2.3f, new Item.Settings().maxDamage(380)));
    public static final Item SLEDGEHAMMER = reg("sledgehammer", new SwordItem(ToolMaterials.IRON, 8, -3.3f, new Item.Settings().maxDamage(300)));
    public static final Item FIRE_AXE = reg("fire_axe", new AxeItem(ToolMaterials.IRON, 7f, -3.1f, new Item.Settings().maxDamage(450)));
    public static final Item KITCHEN_KNIFE = reg("kitchen_knife", new SwordItem(ToolMaterials.IRON, 0, -1.4f, new Item.Settings().maxDamage(140)));
    public static final Item FRYING_PAN = reg("frying_pan", new SwordItem(ToolMaterials.IRON, 2, -2.6f, new Item.Settings().maxDamage(500)));
    public static final Item GOLF_CLUB = reg("golf_club", new SwordItem(ToolMaterials.IRON, 2, -2.5f, new Item.Settings().maxDamage(240)));
    public static final Item HOCKEY_STICK = reg("hockey_stick", new SwordItem(ToolMaterials.WOOD, 4, -2.6f, new Item.Settings().maxDamage(190)));
    public static final Item PIPE_WRENCH = reg("pipe_wrench", new SwordItem(ToolMaterials.IRON, 3, -2.8f, new Item.Settings().maxDamage(650)));
    public static final Item POLICE_BATON = reg("police_baton", new SwordItem(ToolMaterials.IRON, 1, -2.0f, new Item.Settings().maxDamage(420)));
    public static final Item CLAW_HAMMER = reg("claw_hammer", new SwordItem(ToolMaterials.IRON, 1, -2.4f, new Item.Settings().maxDamage(380)));

    // armour
    public static final Item POLICE_VEST = reg("police_vest", new ArmorItem(OutbreakArmor.POLICE, ArmorItem.Type.CHESTPLATE, new Item.Settings()));
    public static final Item MILITARY_VEST = reg("military_vest", new ArmorItem(OutbreakArmor.MILITARY, ArmorItem.Type.CHESTPLATE, new Item.Settings()));
    public static final Item MILITARY_HELMET = reg("military_helmet", new ArmorItem(OutbreakArmor.MILITARY, ArmorItem.Type.HELMET, new Item.Settings()));
    public static final Item RIOT_HELMET = reg("riot_helmet", new ArmorItem(OutbreakArmor.RIOT, ArmorItem.Type.HELMET, new Item.Settings()));

    // clothes
    public static final Item BEANIE = clothes("beanie", ArmorItem.Type.HELMET, 80, 1, 0, false);
    public static final Item BASEBALL_CAP = clothes("baseball_cap", ArmorItem.Type.HELMET, 80, 1, 0, false);
    public static final Item HARD_HAT = clothes("hard_hat", ArmorItem.Type.HELMET, 220, 2, 0.5f, false);
    public static final Item FIREFIGHTER_HELMET = clothes("firefighter_helmet", ArmorItem.Type.HELMET, 260, 2, 1f, false);
    public static final Item GAS_MASK = clothes("gas_mask", ArmorItem.Type.HELMET, 160, 1, 0, false);
    public static final Item HOODIE = clothes("hoodie", ArmorItem.Type.CHESTPLATE, 140, 2, 0, false);
    public static final Item LEATHER_JACKET = clothes("leather_jacket", ArmorItem.Type.CHESTPLATE, 260, 3, 0, true);
    public static final Item FIREFIGHTER_JACKET = clothes("firefighter_jacket", ArmorItem.Type.CHESTPLATE, 300, 4, 1f, false);
    public static final Item HI_VIS_VEST = clothes("hi_vis_vest", ArmorItem.Type.CHESTPLATE, 100, 1, 0, false);
    public static final Item JEANS = clothes("jeans", ArmorItem.Type.LEGGINGS, 200, 2, 0, false);
    public static final Item CARGO_PANTS = clothes("cargo_pants", ArmorItem.Type.LEGGINGS, 200, 2, 0, false);
    public static final Item FIREFIGHTER_PANTS = clothes("firefighter_pants", ArmorItem.Type.LEGGINGS, 280, 3, 1f, false);
    public static final Item SNEAKERS = clothes("sneakers", ArmorItem.Type.BOOTS, 120, 1, 0, false);
    public static final Item WORK_BOOTS = clothes("work_boots", ArmorItem.Type.BOOTS, 260, 2, 0, true);

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

    // scavenged materials
    public static final Item SCRAP_METAL = reg("scrap_metal", new Item(new Item.Settings()));
    public static final Item ELECTRONICS = reg("electronics", new Item(new Item.Settings()));
    public static final Item DUCT_TAPE = reg("duct_tape", new Item(new Item.Settings().maxCount(16)));
    public static final Item NAILS = reg("nails", new Item(new Item.Settings()));
    public static final Item SHEET_METAL = reg("sheet_metal", new Item(new Item.Settings().maxCount(16)));
    public static final Item RAG = reg("rag", new Item(new Item.Settings()));
    public static final Item GUN_PARTS = reg("gun_parts", new Item(new Item.Settings().maxCount(16)));
    public static final Item CAR_BATTERY = reg("car_battery", new Item(new Item.Settings().maxCount(4)));

    // more food
    public static final Item MRE = reg("mre", new Item(new Item.Settings().food(
            new FoodComponent.Builder().hunger(12).saturationModifier(0.9f).build())));
    public static final Item CANNED_TUNA = reg("canned_tuna", new Item(new Item.Settings().food(
            new FoodComponent.Builder().hunger(6).saturationModifier(0.8f).meat().build())));
    public static final Item ENERGY_DRINK = reg("energy_drink", new Item(new Item.Settings().maxCount(16).food(
            new FoodComponent.Builder().hunger(2).saturationModifier(0.2f).alwaysEdible()
                    .statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1), 1f)
                    .statusEffect(new StatusEffectInstance(StatusEffects.HASTE, 600, 0), 1f).build())));

    // crafted gear, tier 1 (scrap), tier 2 (reinforced), tier 3 (military-grade)
    public static final Item NAIL_BOARD = reg("nail_board", new SwordItem(ToolMaterials.WOOD, 4, -2.6f, new Item.Settings().maxDamage(120)));
    public static final Item MAKESHIFT_SPEAR = reg("makeshift_spear", new SwordItem(ToolMaterials.IRON, 3, -2.4f, new Item.Settings().maxDamage(200)));
    public static final Item REINFORCED_BAT = reg("reinforced_bat", new SwordItem(ToolMaterials.IRON, 6, -2.7f, new Item.Settings().maxDamage(700)));
    public static final Item SCRAP_ARMOR = clothes("scrap_armor", ArmorItem.Type.CHESTPLATE, 260, 5, 1f, false);
    public static final Item KEVLAR_VEST = reg("kevlar_vest", new ArmorItem(OutbreakArmor.KEVLAR, ArmorItem.Type.CHESTPLATE, new Item.Settings()));
    public static final Item TACTICAL_HELMET = reg("tactical_helmet", new ArmorItem(OutbreakArmor.KEVLAR, ArmorItem.Type.HELMET, new Item.Settings()));
    public static final Item NIGHT_VISION_GOGGLES = reg("night_vision_goggles", new NightVisionGoggles(new Clothing("night_vision_goggles", 600, 1, 0, false), new Item.Settings()));
    public static final Item PIPE_BOMB = reg("pipe_bomb", new ThrowableItem(new Item.Settings(), "tooltip.outbreak.pipe_bomb"));
    public static final Item MOLOTOV = reg("molotov", new ThrowableItem(new Item.Settings(), "tooltip.outbreak.molotov"));
    public static final Item NOISE_MAKER = reg("noise_maker", new ThrowableItem(new Item.Settings(), "tooltip.outbreak.noise_maker"));

    public static final net.minecraft.entity.EntityType<ThrownGear> THROWN = net.minecraft.registry.Registry.register(Registries.ENTITY_TYPE, Outbreak.id("thrown_gear"),
            net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder.<ThrownGear>create(net.minecraft.entity.SpawnGroup.MISC, ThrownGear::new)
                    .dimensions(net.minecraft.entity.EntityDimensions.fixed(0.3f, 0.3f)).trackRangeBlocks(64).trackedUpdateRate(10).build());

    public static final net.minecraft.entity.EntityType<CorpseEntity> CORPSE = net.minecraft.registry.Registry.register(Registries.ENTITY_TYPE, Outbreak.id("corpse"),
            net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder.<CorpseEntity>create(net.minecraft.entity.SpawnGroup.MISC, CorpseEntity::new)
                    .dimensions(net.minecraft.entity.EntityDimensions.fixed(1.3f, 0.45f)).trackRangeBlocks(64).trackedUpdateRate(20).build());

    // ---- more melee
    public static final Item KATANA = reg("katana", new SwordItem(ToolMaterials.IRON, 5, -2.2f, new Item.Settings().maxDamage(420)));
    public static final Item HATCHET = reg("hatchet", new SwordItem(ToolMaterials.IRON, 4, -2.6f, new Item.Settings().maxDamage(300)));
    public static final Item MEAT_CLEAVER = reg("meat_cleaver", new SwordItem(ToolMaterials.IRON, 3, -2.2f, new Item.Settings().maxDamage(220)));
    public static final Item TIRE_IRON = reg("tire_iron", new SwordItem(ToolMaterials.IRON, 2, -2.3f, new Item.Settings().maxDamage(600)));
    public static final Item PICKAXE_HANDLE = reg("pickaxe_handle", new SwordItem(ToolMaterials.WOOD, 3, -2.6f, new Item.Settings().maxDamage(180)));
    public static final Item BARBED_BAT = reg("barbed_bat", new SwordItem(ToolMaterials.WOOD, 7, -2.9f, new Item.Settings().maxDamage(240)));
    public static final Item ROLLING_PIN = reg("rolling_pin", new SwordItem(ToolMaterials.WOOD, 2, -2.4f, new Item.Settings().maxDamage(140)));
    public static final Item SHOVEL = reg("shovel_weapon", new SwordItem(ToolMaterials.IRON, 3, -2.8f, new Item.Settings().maxDamage(350)));

    // ---- skill books: three volumes per skill
    public static final java.util.List<Item> BOOKS = new java.util.ArrayList<>();
    static {
        for (Skills.Skill s : Skills.Skill.values())
            for (int v = 1; v <= 3; v++) BOOKS.add(reg("book_" + s.id() + "_" + v, new SkillBookItem(s, v, new Item.Settings().maxCount(1))));
    }

    private OutbreakItems() {}

    private static Item clothes(String name, ArmorItem.Type type, int durability, int protection, float toughness, boolean leather) {
        return reg(name, new ArmorItem(new Clothing(name, durability, protection, toughness, leather), type, new Item.Settings()));
    }

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
                    for (net.minecraft.block.Block b : OutbreakBlocks.ALL) entries.add(new ItemStack(b));
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, Outbreak.id("outbreak"), group);
    }
}
