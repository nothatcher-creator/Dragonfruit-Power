package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.EmptyEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.function.SetNbtLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import static com.nothatcher.outbreak.OutbreakItems.*;

/**
 * Puts Outbreak gear into containers: every Vehicle Forge city loot table (houses, apartments, shops, police,
 * hospitals, fire stations, schools, offices, warehouses, construction sites) and vanilla village houses, dungeons,
 * mineshafts, outposts and shipwrecks. Works without Vehicle Forge too.
 */
public final class LootInjector {
    private LootInjector() {}

    /** a pool builder: entries are (item, weight, min, max); emptyWeight leaves some rolls with nothing */
    private static final class P {
        final LootPool.Builder b;

        P(float minRolls, float maxRolls, int emptyWeight) {
            b = LootPool.builder().rolls(UniformLootNumberProvider.create(minRolls, maxRolls));
            if (emptyWeight > 0) b.with(EmptyEntry.builder().weight(emptyWeight));
        }

        P add(Item item, int weight, int min, int max) {
            var e = ItemEntry.builder(item).weight(weight);
            if (max > 1) e.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)));
            b.with(e);
            return this;
        }

        P gun(GunItem gun, int weight) {
            NbtCompound n = new NbtCompound();
            n.putInt("Ammo", Math.max(1, gun.mag / 3));
            b.with(ItemEntry.builder(gun).weight(weight).apply(SetNbtLootFunction.builder(n)));
            return this;
        }

        void into(LootTable.Builder t) {
            t.pool(b);
        }
    }

    private static P food(float lo, float hi, int empty) {
        return new P(lo, hi, empty).add(CANNED_BEANS, 10, 1, 3).add(CANNED_SOUP, 10, 1, 3).add(CHIPS, 12, 1, 4)
                .add(ENERGY_BAR, 8, 1, 3).add(BOTTLED_WATER, 10, 1, 4);
    }

    private static P meds(float lo, float hi, int empty) {
        return new P(lo, hi, empty).add(BANDAGE, 14, 1, 4).add(PAINKILLERS, 8, 1, 3).add(ANTIBIOTICS, 3, 1, 2).add(FIRST_AID_KIT, 2, 1, 1);
    }

    private static P homeDefence(int empty) {
        return new P(1, 1, empty).add(BASEBALL_BAT, 10, 1, 1).add(CROWBAR, 6, 1, 1).add(MACHETE, 3, 1, 1)
                .add(KITCHEN_KNIFE, 8, 1, 1).add(FRYING_PAN, 6, 1, 1).add(GOLF_CLUB, 4, 1, 1).add(HOCKEY_STICK, 5, 1, 1)
                .add(CLAW_HAMMER, 4, 1, 1)
                .gun(PISTOL, 4).gun(SHOTGUN, 2).gun(HUNTING_RIFLE, 1).gun(REVOLVER, 2).gun(DOUBLE_BARREL, 2)
                .add(AMMO_9MM, 6, 4, 14).add(AMMO_SHELLS, 4, 2, 8).add(AMMO_RIFLE, 3, 2, 6).add(AMMO_MAGNUM, 3, 2, 6)
                .add(BACKPACK, 3, 1, 1);
    }

    /** scavenging materials for crafting */
    private static P mats(float lo, float hi, int empty) {
        return new P(lo, hi, empty).add(SCRAP_METAL, 8, 1, 4).add(DUCT_TAPE, 6, 1, 2).add(NAILS, 6, 3, 10).add(RAG, 5, 1, 3)
                .add(ELECTRONICS, 3, 1, 2).add(CAR_BATTERY, 1, 1, 1);
    }

    private static P wardrobe(float lo, float hi, int empty) {
        return new P(lo, hi, empty).add(HOODIE, 8, 1, 1).add(LEATHER_JACKET, 4, 1, 1).add(JEANS, 8, 1, 1).add(CARGO_PANTS, 4, 1, 1)
                .add(SNEAKERS, 7, 1, 1).add(WORK_BOOTS, 3, 1, 1).add(BEANIE, 5, 1, 1).add(BASEBALL_CAP, 5, 1, 1);
    }

    public static void register() {
        LootTableEvents.MODIFY.register((resources, loot, id, table, source) -> {
            if (!source.isBuiltin()) return;
            String ns = id.getNamespace(), path = id.getPath();
            if (ns.equals("vehicleforge") && path.startsWith("city/")) city(path.substring(5), table);
            else if (ns.equals("vehicleforge") && path.equals("props/vending"))
                new P(1, 1, 2).add(CHIPS, 6, 1, 2).add(ENERGY_BAR, 4, 1, 1).add(ENERGY_DRINK, 4, 1, 1).add(BOTTLED_WATER, 6, 1, 1).into(table);
            else if (id.equals(Outbreak.id("gameplay/vehicle_search"))) {
                // a jerry can of fuel now and then, if Vehicle Forge is installed
                Identifier can = new Identifier("vehicleforge", "fuel_can");
                if (net.minecraft.registry.Registries.ITEM.containsId(can))
                    new P(1, 1, 6).add(net.minecraft.registry.Registries.ITEM.get(can), 2, 1, 1).into(table);
                mats(0, 2, 4).into(table);
            }
            else if (ns.equals("vehicleforge") && path.equals("vehicles/trunk")) {
                // the back of a parked car: road-trip supplies, now and then something useful
                Identifier can = new Identifier("vehicleforge", "fuel_can");
                if (net.minecraft.registry.Registries.ITEM.containsId(can))
                    new P(1, 1, 5).add(net.minecraft.registry.Registries.ITEM.get(can), 2, 1, 1).into(table);
                new P(1, 1, 3).add(BOTTLED_WATER, 5, 1, 2).add(CHIPS, 4, 1, 2).add(ENERGY_BAR, 3, 1, 2).add(BANDAGE, 3, 1, 2).add(DUCT_TAPE, 2, 1, 1).into(table);
                mats(0, 2, 4).into(table);
            }
            else if (ns.equals("vehicleforge") && path.equals("vehicles/wreck")) mats(0, 1, 3).into(table);
            else if (ns.equals(Identifier.DEFAULT_NAMESPACE)) vanilla(path, table);
        });
    }

    private static void city(String kind, LootTable.Builder t) {
        kind = switch (kind) {                       // Vehicle Forge's newer places stock like the older ones
            case "diner" -> "kitchen";
            case "motel" -> "house";
            case "mechanic" -> "garage";
            case "bank", "dealership" -> "office";
            default -> kind;
        };
        switch (kind) {
            case "house", "apartment" -> {
                new P(0, 1, 9).add(FLASHLIGHT, 1, 1, 1).into(t);
                food(1, 3, 4).into(t);
                meds(0, 1, 6).into(t);
                homeDefence(18).into(t);
                wardrobe(0, 1, 8).into(t);
                mats(0, 1, 8).into(t);
            }
            case "fridge" -> {
                food(1, 3, 3).into(t);
                new P(0, 1, 6).add(CANNED_TUNA, 4, 1, 2).add(ENERGY_DRINK, 5, 1, 3).add(BOTTLED_WATER, 6, 1, 3).into(t);
            }
            case "kitchen" -> new P(0, 2, 5).add(KITCHEN_KNIFE, 5, 1, 1).add(FRYING_PAN, 3, 1, 1).add(CANNED_BEANS, 5, 1, 2)
                    .add(CANNED_SOUP, 4, 1, 2).add(CANNED_TUNA, 3, 1, 2).add(RAG, 4, 1, 2).add(DUCT_TAPE, 1, 1, 1).into(t);
            case "mail" -> new P(0, 1, 10).add(DUCT_TAPE, 2, 1, 1).add(ELECTRONICS, 1, 1, 1).add(BANDAGE, 2, 1, 1).into(t);
            case "bunker" -> {
                new P(2, 4, 1).add(MRE, 8, 1, 3).add(BOTTLED_WATER, 8, 2, 4).add(CANNED_BEANS, 5, 1, 3).add(FIRST_AID_KIT, 3, 1, 1)
                        .add(ANTIBIOTICS, 3, 1, 2).add(AMMO_9MM, 5, 8, 24).add(AMMO_SHELLS, 4, 4, 12).add(DUCT_TAPE, 3, 1, 3).add(CAR_BATTERY, 2, 1, 1).add(FLASHLIGHT, 3, 1, 1).into(t);
                new P(0, 1, 3).gun(SHOTGUN, 3).gun(PISTOL, 3).gun(HUNTING_RIFLE, 2).add(GAS_MASK, 2, 1, 1).into(t);
            }
            case "bunker_vault" -> new P(2, 4, 0).gun(ASSAULT_RIFLE, 3).gun(SNIPER_RIFLE, 2).gun(LIGHT_MACHINE_GUN, 1).add(KEVLAR_VEST, 3, 1, 1)
                    .add(TACTICAL_HELMET, 3, 1, 1).add(NIGHT_VISION_GOGGLES, 2, 1, 1).add(AMMO_RIFLE, 6, 16, 40).add(PIPE_BOMB, 3, 2, 4)
                    .add(GUN_PARTS, 3, 2, 4).add(MRE, 4, 2, 4).into(t);
            case "armory" -> {
                new P(2, 4, 1).gun(ASSAULT_RIFLE, 5).gun(SMG, 4).gun(SNIPER_RIFLE, 2).gun(LIGHT_MACHINE_GUN, 2).gun(SILENCED_PISTOL, 2)
                        .add(KEVLAR_VEST, 3, 1, 1).add(TACTICAL_HELMET, 3, 1, 1).add(MILITARY_VEST, 3, 1, 1).add(NIGHT_VISION_GOGGLES, 2, 1, 1)
                        .add(PIPE_BOMB, 4, 2, 5).add(GUN_PARTS, 4, 2, 4).into(t);
                new P(2, 4, 0).add(AMMO_RIFLE, 8, 20, 48).add(AMMO_9MM, 6, 16, 40).add(AMMO_SHELLS, 4, 8, 20).add(MRE, 4, 1, 3).into(t);
            }
            case "crypt" -> new P(0, 2, 4).add(ANTIBIOTICS, 3, 1, 2).add(MACHETE, 2, 1, 1).add(RAG, 4, 2, 4).into(t);
            case "luggage" -> {
                wardrobe(1, 2, 4).into(t);
                new P(0, 2, 4).add(PAINKILLERS, 4, 1, 2).add(CHIPS, 4, 1, 3).add(ENERGY_BAR, 3, 1, 2).add(BANDAGE, 3, 1, 2).add(BACKPACK, 1, 1, 1)
                        .add(ELECTRONICS, 2, 1, 1).add(LEATHER_JACKET, 2, 1, 1).into(t);
            }
            case "farm" -> new P(0, 2, 4).add(MACHETE, 2, 1, 1).add(FIRE_AXE, 1, 1, 1).add(CANNED_BEANS, 3, 1, 2).add(BOTTLED_WATER, 4, 1, 3)
                    .add(DUCT_TAPE, 2, 1, 1).add(WORK_BOOTS, 2, 1, 1).add(CARGO_PANTS, 2, 1, 1).add(AMMO_SHELLS, 2, 2, 6).gun(DOUBLE_BARREL, 1).into(t);
            case "cabin" -> new P(1, 2, 3).gun(HUNTING_RIFLE, 3).gun(DOUBLE_BARREL, 2).add(AMMO_RIFLE, 5, 4, 12).add(AMMO_SHELLS, 4, 3, 10)
                    .add(MACHETE, 3, 1, 1).add(RAG, 3, 1, 3).add(CANNED_TUNA, 3, 1, 2).add(BEANIE, 2, 1, 1).into(t);
            case "garage" -> {
                mats(1, 3, 3).into(t);
                new P(0, 1, 3).add(FLASHLIGHT, 1, 1, 1).add(CAR_BATTERY, 1, 1, 1).into(t);
                new P(1, 2, 3).add(CROWBAR, 6, 1, 1).add(SLEDGEHAMMER, 4, 1, 1).add(PIPE_WRENCH, 6, 1, 1).add(CLAW_HAMMER, 6, 1, 1)
                        .add(FIRE_AXE, 2, 1, 1).add(HARD_HAT, 3, 1, 1).add(WORK_BOOTS, 3, 1, 1).add(GOLF_CLUB, 3, 1, 1)
                        .add(HOCKEY_STICK, 3, 1, 1).add(BOTTLED_WATER, 4, 1, 3).into(t);
                new P(0, 1, 10).gun(HUNTING_RIFLE, 2).gun(DOUBLE_BARREL, 2).add(AMMO_SHELLS, 3, 2, 8).add(AMMO_RIFLE, 2, 2, 6).into(t);
            }
            case "shop" -> {
                food(2, 5, 2).into(t);
                new P(0, 1, 10).add(BACKPACK, 4, 1, 1).add(BANDAGE, 5, 1, 3).add(PAINKILLERS, 3, 1, 2).into(t);
                wardrobe(0, 2, 6).into(t);
                new P(0, 1, 6).add(DUCT_TAPE, 4, 1, 2).add(ENERGY_DRINK, 5, 1, 2).add(CANNED_TUNA, 4, 1, 2).add(ELECTRONICS, 1, 1, 1).into(t);
            }
            case "police" -> {
                new P(1, 2, 3).gun(PISTOL, 10).gun(SHOTGUN, 6).gun(SMG, 2).gun(ASSAULT_RIFLE, 1).gun(REVOLVER, 3).gun(SILENCED_PISTOL, 1)
                        .gun(SNIPER_RIFLE, 1).add(POLICE_VEST, 6, 1, 1).add(RIOT_HELMET, 4, 1, 1).add(POLICE_BATON, 6, 1, 1)
                        .add(GAS_MASK, 2, 1, 1).into(t);
                new P(1, 3, 2).add(AMMO_9MM, 12, 6, 20).add(AMMO_SHELLS, 8, 3, 10).add(AMMO_RIFLE, 3, 3, 10).add(AMMO_MAGNUM, 4, 3, 8).into(t);
                new P(0, 1, 8).add(GUN_PARTS, 5, 1, 2).add(KEVLAR_VEST, 1, 1, 1).add(TACTICAL_HELMET, 1, 1, 1).add(PIPE_BOMB, 1, 1, 2)
                        .add(NIGHT_VISION_GOGGLES, 1, 1, 1).into(t);
                meds(0, 1, 4).into(t);
                new P(0, 1, 2).add(FLASHLIGHT, 1, 1, 1).into(t);
            }
            case "hospital" -> {
                meds(2, 4, 2).into(t);
                new P(0, 1, 8).add(GAS_MASK, 3, 1, 1).add(KITCHEN_KNIFE, 1, 1, 1).add(RAG, 6, 2, 5).into(t);
            }
            case "fire_station" -> {
                meds(1, 2, 3).into(t);
                new P(0, 2, 5).add(CROWBAR, 6, 1, 1).add(SLEDGEHAMMER, 4, 1, 1).add(FIRE_AXE, 8, 1, 1).add(BOTTLED_WATER, 6, 2, 5)
                        .add(FIREFIGHTER_HELMET, 4, 1, 1).add(FIREFIGHTER_JACKET, 4, 1, 1).add(FIREFIGHTER_PANTS, 4, 1, 1)
                        .add(GAS_MASK, 3, 1, 1).into(t);
            }
            case "school" -> {
                food(1, 2, 4).into(t);
                new P(0, 1, 8).add(BASEBALL_BAT, 6, 1, 1).add(BACKPACK, 6, 1, 1).add(BANDAGE, 4, 1, 2).into(t);
            }
            case "office" -> {
                food(0, 2, 5).into(t);
                new P(0, 1, 14).add(PAINKILLERS, 5, 1, 2).add(BASEBALL_BAT, 2, 1, 1).gun(PISTOL, 1).add(BACKPACK, 2, 1, 1).into(t);
            }
            case "warehouse", "construction" -> {
                new P(0, 2, 6).add(CROWBAR, 6, 1, 1).add(SLEDGEHAMMER, 5, 1, 1).add(MACHETE, 3, 1, 1).add(SPIKED_BAT, 2, 1, 1)
                        .add(PIPE_WRENCH, 5, 1, 1).add(CLAW_HAMMER, 5, 1, 1).add(ENERGY_BAR, 5, 1, 3).add(BOTTLED_WATER, 5, 1, 4).into(t);
                new P(0, 1, 6).add(HARD_HAT, 6, 1, 1).add(HI_VIS_VEST, 6, 1, 1).add(WORK_BOOTS, 5, 1, 1).add(CARGO_PANTS, 3, 1, 1).into(t);
                mats(1, 3, 2).into(t);
            }
            default -> food(0, 2, 6).into(t);
        }
    }

    private static void vanilla(String path, LootTable.Builder t) {
        if (path.startsWith("chests/village/")) {
            food(0, 2, 6).into(t);
            meds(0, 1, 8).into(t);
            homeDefence(24).into(t);
        } else if (path.equals("chests/simple_dungeon") || path.equals("chests/abandoned_mineshaft")) {
            new P(0, 2, 8).gun(PISTOL, 4).gun(SHOTGUN, 3).gun(HUNTING_RIFLE, 2)
                    .add(AMMO_9MM, 6, 4, 14).add(AMMO_SHELLS, 5, 2, 8).add(AMMO_RIFLE, 4, 2, 6).add(FIRST_AID_KIT, 2, 1, 1).into(t);
        } else if (path.equals("chests/pillager_outpost") || path.equals("chests/woodland_mansion")) {
            new P(1, 2, 4).gun(ASSAULT_RIFLE, 3).gun(SMG, 4).gun(HUNTING_RIFLE, 3).gun(SNIPER_RIFLE, 2).gun(LIGHT_MACHINE_GUN, 1)
                    .add(MILITARY_VEST, 3, 1, 1).add(MILITARY_HELMET, 3, 1, 1).add(GUN_PARTS, 4, 1, 3).add(MRE, 5, 1, 3)
                    .add(KEVLAR_VEST, 1, 1, 1).add(TACTICAL_HELMET, 1, 1, 1).add(NIGHT_VISION_GOGGLES, 1, 1, 1).add(PIPE_BOMB, 2, 1, 3)
                    .add(AMMO_RIFLE, 8, 6, 20).add(AMMO_9MM, 6, 6, 20).into(t);
        } else if (path.startsWith("chests/shipwreck") || path.equals("chests/buried_treasure")) {
            food(0, 2, 4).into(t);
            new P(0, 1, 8).gun(PISTOL, 3).add(AMMO_9MM, 5, 4, 12).into(t);
        }
    }
}
