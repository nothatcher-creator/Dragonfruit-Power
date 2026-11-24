package com.nothatcher.vehicleforge.world;

import com.nothatcher.vehicleforge.ModRegistry;
import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.block.CityBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;

/**
 * The city layout. The world is a grid of 81x81 cells. Along the west and north edge of every cell runs a corridor:
 * a 3-wide raised sidewalk, a 23-wide road (parking lane, two travel lanes each way, double yellow centre line)
 * and another sidewalk. Inside is a 52x52 city block. Blocks near the origin are downtown towers,
 * then apartments, shops, parking lots and gas stations, then suburbs with houses. Every column is computed from
 * (x, z, seed) alone, so chunks line up perfectly no matter what order they generate in.
 */
public final class CityGen {
    static final int CELL = 81, C = 29, R0 = 3, R1 = 26, GROUND = 63, BASE = 64;
    /** with the Outbreak mod installed the city is a few weeks into the end of the world: wrecks everywhere */
    static final boolean OVERRUN = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("outbreak");

    enum Kind {
        TOWERS, APARTMENTS, PARKING, PARK, GAS, HOUSES,
        LANDMARK, PLAZA, HOSPITAL, FIRE, POLICE, SCHOOL, STADIUM, ROWHOUSES,
        WAREHOUSE, FACTORY, CONTAINERS, CONSTRUCTION, RIVER, CHURCH,
        AIRPORT, STATION, FARM, FOREST, MEADOW, ORCHARD,
        DINER, MOTEL, DEALERSHIP, MECHANIC, BANK,
        PHARMACY, GUN_STORE, HARDWARE, PRISON, TRAILER_PARK
    }

    // ------------------------------------------------- zones: airport, countryside, railway

    /** past this many cells from the centre, the city gives way to countryside */
    static final int COUNTRY = 16;
    /** the airport covers cells 12..15 east and 4..5 south of the centre, roads between them included */
    static final int AIR_BX = 12, AIR_BZ = 4, AIR_X0 = AIR_BX * CELL + C, AIR_Z0 = AIR_BZ * CELL + C, AIR_W = 4 * CELL - C, AIR_H = 2 * CELL - C;
    static final int RAIL_LZ = 40, RAIL_Y = BASE + 5;

    /**
     * The world is a grid of metro areas, METRO cells (~3.9 km) apart: each is a city out to COUNTRY cells from its
     * centre, then countryside until the next city. Railways run from each city's station across the countryside
     * to the next city's.
     */
    static final int METRO = 48;

    /** which metro area this cell index belongs to */
    static int metroOf(int b) {
        return Math.floorDiv(b + METRO / 2, METRO);
    }

    /** cell index relative to its metro's centre (-24..23) */
    static int local(int b) {
        return b - metroOf(b) * METRO;
    }

    /** how far a cell is from the centre of its city, in cells */
    static int metroD(int bx, int bz) {
        return Math.max(Math.abs(local(bx)), Math.abs(local(bz)));
    }

    /** the first city always has an airport; others about half the time (never where a river would cut it) */
    static boolean hasAirport(int mx, int mz) {
        if (riverRow(mz * METRO + AIR_BZ) || riverRow(mz * METRO + AIR_BZ + 1)) return false;
        if (mx == 0 && mz == 0) return true;
        int v = (mx * 0x9E3779B1) ^ (mz * 0x85EBCA77);
        v ^= v >>> 15; v *= 0x2C1B3C6D; v ^= v >>> 12;
        return (v & 1) == 0;
    }

    /** the airport's north-west corner for the metro containing (x, z), or null if there isn't one there */
    static int[] airportOrigin(int x, int z) {
        int mx = metroOf(Math.floorDiv(x, CELL)), mz = metroOf(Math.floorDiv(z, CELL));
        if (!hasAirport(mx, mz)) return null;
        return new int[]{(mx * METRO + AIR_BX) * CELL + C, (mz * METRO + AIR_BZ) * CELL + C};
    }

    static boolean inAirport(int x, int z) {
        int[] o = airportOrigin(x, z);
        return o != null && x >= o[0] && x < o[0] + AIR_W && z >= o[1] && z < o[1] + AIR_H;
    }

    static boolean airportCell(int bx, int bz) {
        int lbx = local(bx), lbz = local(bz);
        return lbx >= AIR_BX && lbx < AIR_BX + 4 && lbz >= AIR_BZ && lbz < AIR_BZ + 2 && hasAirport(metroOf(bx), metroOf(bz));
    }

    static boolean countryCell(int bx, int bz) {
        return metroD(bx, bz) >= COUNTRY;
    }

    static boolean railRow(int bz) {
        return Math.floorMod(bz, 9) == 6 && !riverRow(bz);
    }

    /** the first countryside cell on a railway row, east or west of the city: the line ends here */
    static boolean stationCell(int bx, int bz) {
        return railRow(bz) && Math.abs(local(bz)) < COUNTRY && Math.abs(local(bx)) == COUNTRY;
    }

    static boolean railCell(int bx, int bz) {
        return railRow(bz) && countryCell(bx, bz);
    }

    private enum Roof { PLAIN, HELIPAD, SPIRE, WATER_TOWER, GARDEN }

    /** river channel: water from y 46 to 58, quay walls, 4-block-high clearance under the bridges */
    static final int RIVER_EVERY = 7, RIVER_ROW = 3, WATER_TOP = 58, BED = 45;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState ASPHALT = CityBlocks.ASPHALT.getDefaultState();
    private static final BlockState LINE_W = Blocks.WHITE_CONCRETE.getDefaultState();
    private static final BlockState LINE_Y = Blocks.YELLOW_CONCRETE.getDefaultState();
    private static final BlockState WALK = CityBlocks.SIDEWALK.getDefaultState();
    private static final BlockState CURB = CityBlocks.CURB.getDefaultState();
    private static final BlockState WALK_SLAB = CityBlocks.SIDEWALK_SLAB.getDefaultState();
    private static final BlockState PLAZA = Blocks.POLISHED_ANDESITE.getDefaultState();
    private static final BlockState POLE = CityBlocks.LAMP_POST.getDefaultState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.getDefaultState();
    private static final BlockState CEILING_LIGHT = CityBlocks.ELECTRIC_LIGHT.getDefaultState()
            .with(net.minecraft.state.property.Properties.LIT, !com.nothatcher.vehicleforge.power.PowerGrid.MAINS_DOWN);
    private static final BlockState LADDER = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST);
    private static final BlockState PATH = Blocks.DIRT_PATH.getDefaultState();
    private static final BlockState WATER = Blocks.WATER.getDefaultState();
    private static final BlockState[] FLOWERS = {
            Blocks.POPPY.getDefaultState(), Blocks.DANDELION.getDefaultState(), Blocks.CORNFLOWER.getDefaultState(),
            Blocks.OXEYE_DAISY.getDefaultState(), Blocks.AZURE_BLUET.getDefaultState()
    };

    private static final BlockState CLEAR = CityBlocks.CLEAR_GLASS.getDefaultState();
    private static final BlockState REFLECT = CityBlocks.REFLECTIVE_GLASS.getDefaultState();
    private static final BlockState CURB_BLOCK = CityBlocks.CURB.getDefaultState().with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.DOUBLE);

    /** facade materials */
    private record Style(BlockState wall, BlockState window, BlockState frame, int bay, boolean tallWindows, boolean shopFront) {}

    private static final Style[] STYLES = {
            // downtown towers
            new Style(Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), REFLECT, Blocks.GRAY_CONCRETE.getDefaultState(), 4, true, false),
            new Style(Blocks.BLACK_CONCRETE.getDefaultState(), REFLECT, Blocks.BLACK_CONCRETE.getDefaultState(), 3, true, false),
            new Style(Blocks.QUARTZ_BLOCK.getDefaultState(), CLEAR, Blocks.SMOOTH_QUARTZ.getDefaultState(), 2, false, false),
            new Style(Blocks.STONE_BRICKS.getDefaultState(), CLEAR, Blocks.POLISHED_ANDESITE.getDefaultState(), 2, false, false),
            // midtown
            new Style(Blocks.BRICKS.getDefaultState(), CLEAR, Blocks.SMOOTH_STONE.getDefaultState(), 3, false, false),
            new Style(Blocks.SMOOTH_SANDSTONE.getDefaultState(), CLEAR, Blocks.CUT_SANDSTONE.getDefaultState(), 2, false, true),
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), 3, false, true),
            new Style(Blocks.TERRACOTTA.getDefaultState(), CLEAR, Blocks.POLISHED_DEEPSLATE.getDefaultState(), 2, false, true),
            // more towers (8-11)
            new Style(Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(), REFLECT, Blocks.WHITE_CONCRETE.getDefaultState(), 5, true, false),
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.GREEN_STAINED_GLASS.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(), 4, true, false),
            new Style(Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.BROWN_STAINED_GLASS.getDefaultState(), Blocks.CUT_SANDSTONE.getDefaultState(), 3, true, false),
            new Style(Blocks.POLISHED_GRANITE.getDefaultState(), Blocks.BLACK_STAINED_GLASS.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState(), 2, false, false),
            // more midtown (12-14)
            new Style(Blocks.MUD_BRICKS.getDefaultState(), CLEAR, Blocks.PACKED_MUD.getDefaultState(), 2, false, false),
            new Style(Blocks.CYAN_TERRACOTTA.getDefaultState(), Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState(), Blocks.STONE.getDefaultState(), 3, false, true),
            new Style(Blocks.DEEPSLATE_TILES.getDefaultState(), CLEAR, Blocks.POLISHED_BLACKSTONE.getDefaultState(), 3, false, true),
            // civic (15 hospital, 16 police, 17 red brick, 18 white terracotta rowhouse)
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.QUARTZ_BLOCK.getDefaultState(), 3, false, false),
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.BLUE_STAINED_GLASS.getDefaultState(), Blocks.BLUE_CONCRETE.getDefaultState(), 3, false, true),
            new Style(Blocks.RED_NETHER_BRICKS.getDefaultState(), CLEAR, Blocks.STONE_BRICKS.getDefaultState(), 2, false, false),
            new Style(Blocks.WHITE_TERRACOTTA.getDefaultState(), CLEAR, Blocks.BROWN_TERRACOTTA.getDefaultState(), 2, false, false),
            // roadside (19 diner, 20 motel, 21 car showroom, 22 bank, 23 mechanic)
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), CLEAR, Blocks.RED_CONCRETE.getDefaultState(), 2, false, false),
            new Style(Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), CLEAR, Blocks.WHITE_CONCRETE.getDefaultState(), 3, false, false),
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), CLEAR, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), 7, true, false),
            new Style(Blocks.SMOOTH_STONE.getDefaultState(), CLEAR, Blocks.QUARTZ_PILLAR.getDefaultState(), 3, false, false),
            new Style(Blocks.GRAY_CONCRETE.getDefaultState(), CLEAR, Blocks.YELLOW_CONCRETE.getDefaultState(), 4, false, false),
            // 24 trailer home
            new Style(Blocks.WHITE_TERRACOTTA.getDefaultState(), CLEAR, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), 3, false, false),
    };
    private static final int[] TOWER_STYLES = {0, 1, 2, 3, 8, 9, 10, 11};
    private static final int[] APT_STYLES = {4, 5, 6, 7, 12, 13, 14};
    private static final int[] ROW_STYLES = {4, 12, 17, 18, 5};
    private static final BlockState INNER_FLOOR = Blocks.POLISHED_ANDESITE.getDefaultState();

    private record HouseStyle(BlockState wall, BlockState floor, Block stairs, Block door) {}

    private static final HouseStyle[] HOUSES = {
            new HouseStyle(Blocks.OAK_PLANKS.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.DARK_OAK_STAIRS, Blocks.OAK_DOOR),
            new HouseStyle(Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState(), Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_DOOR),
            new HouseStyle(Blocks.BRICKS.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState(), Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DARK_OAK_DOOR),
            new HouseStyle(Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState(), Blocks.BRICK_STAIRS, Blocks.BIRCH_DOOR),
            new HouseStyle(Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.STONE_BRICK_STAIRS, Blocks.OAK_DOOR),
    };

    private static final String[] PARKED = {
            "sedan", "sedan", "hatchback", "hatchback", "suv", "suv", "taxi", "pickup_truck", "classic_cruiser",
            "muscle_car", "van", "sports_car", "police_car", "limousine", "vintage_roadster"
    };
    private static final String[] GARAGE_CARS = {
            "sedan", "sedan", "hatchback", "suv", "muscle_car", "sports_car", "classic_cruiser", "vintage_roadster", "hatchback"
    };
    private static final String[] WRECKS = {
            "sedan", "sedan", "hatchback", "hatchback", "suv", "taxi", "pickup_truck", "van", "police_car", "ambulance", "sports_car", "school_bus"
    };
    private static final int[] BURNT = {0x2A2622, 0x3A2A20, 0x5A3A26, 0x4A4440, 0x6B4A30};
    private static final int[] CAR_COLORS = {
            0xF2F2F2, 0x1A1A1E, 0x8E949A, 0xB7BCC2, 0xA01C1C, 0x1E3F8A, 0x2C5E3A, 0x6B4A2E, 0xC8A040, 0x5C6E80, 0x8A1E4A, 0xE07020
    };

    private final StructureWorldAccess w;
    private final long seed;
    /** which loot table the building being generated fills its cupboards from (vehicleforge:city/<loot>) */
    private String loot = "office";
    private final BlockPos.Mutable pos = new BlockPos.Mutable();

    CityGen(StructureWorldAccess world, long seed) {
        this.w = world;
        this.seed = seed;
    }

    // ------------------------------------------------------------ helpers

    private int hash(int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (int) (h & 0x7fffffff);
    }

    private float rnd(int a, int b, int salt) {
        return (hash(a, b, salt) % 10000) / 10000f;
    }

    private void set(int x, int y, int z, BlockState s) {
        pos.set(x, y, z);
        w.setBlockState(pos, s, Block.NOTIFY_LISTENERS);
    }

    /** a cupboard (barrel) or chest that fills itself from vehicleforge:city/<kind> the first time it's opened */
    private void container(int x, int y, int z, String kind, boolean chest) {
        BlockState st = chest ? Blocks.CHEST.getDefaultState() : Blocks.BARREL.getDefaultState();
        set(x, y, z, st);
        pos.set(x, y, z);
        BlockPos at = pos.toImmutable();
        LootableContainerBlockEntity.setLootTable(w, w.getRandom(), at, new net.minecraft.util.Identifier("vehicleforge", "city/" + kind));
    }

    private void lootAt(int x, int y, int z, String kind) {
        pos.set(x, y, z);
        LootableContainerBlockEntity.setLootTable(w, w.getRandom(), pos.toImmutable(), new net.minecraft.util.Identifier("vehicleforge", "city/" + kind));
    }

    private String propLoot(Block b) {
        if (b == CityBlocks.FRIDGE) return "fridge";
        if (b == CityBlocks.KITCHEN_COUNTER) return "kitchen";
        if (b == CityBlocks.STORE_SHELF || b == CityBlocks.CHECKOUT_COUNTER) return shopLoot();
        if (b == CityBlocks.MAILBOX || b == CityBlocks.NEWSPAPER_BOX) return "mail";
        if (b == CityBlocks.SUITCASE) return "luggage";
        return loot;
    }

    /** specialty shops stock their own shelves */
    private String shopLoot() {
        return loot.equals("pharmacy") || loot.equals("hardware") || loot.equals("gun_store") ? loot : "shop";
    }

    private boolean isAir(int x, int y, int z) {
        pos.set(x, y, z);
        return w.isAir(pos);
    }

    static boolean riverRow(int bz) {
        return Math.floorMod(bz, RIVER_EVERY) == RIVER_ROW;
    }

    Kind kind(int bx, int bz) {
        if (riverRow(bz)) return Kind.RIVER;
        if (local(bx) == 0 && local(bz) == 0) return Kind.PARK;            // central park (at spawn, for the first city)
        if (local(bx) == 1 && local(bz) == 1) return Kind.LANDMARK;        // the tallest tower in each city
        if (airportCell(bx, bz)) return Kind.AIRPORT;
        int d = metroD(bx, bz);
        float r = rnd(bx, bz, 1);
        if (stationCell(bx, bz)) return Kind.STATION;
        if (d >= COUNTRY) {
            return r < 0.38f ? Kind.FARM : r < 0.64f ? Kind.FOREST : r < 0.8f ? Kind.MEADOW : r < 0.93f ? Kind.ORCHARD : Kind.GAS;
        }
        boolean industrial = d >= 5 && rnd(Math.floorDiv(bx, 3), Math.floorDiv(bz, 3), 70) < 0.25f;
        if (industrial) {
            return r < 0.32f ? Kind.WAREHOUSE : r < 0.55f ? Kind.FACTORY : r < 0.75f ? Kind.CONTAINERS
                    : r < 0.88f ? Kind.CONSTRUCTION : Kind.PARKING;
        }
        if (d <= 4) {
            return r < 0.08f ? Kind.PARK : r < 0.14f ? Kind.PLAZA : r < 0.17f ? Kind.CHURCH : r < 0.21f ? Kind.PARKING
                    : r < 0.26f ? Kind.CONSTRUCTION : r < 0.29f ? Kind.HOSPITAL : r < 0.33f ? Kind.BANK : Kind.TOWERS;
        }
        if (d <= 10) {
            if (r < 0.09f) return Kind.PARK;
            if (r < 0.16f) return Kind.PARKING;
            if (r < 0.21f) return Kind.GAS;
            if (r < 0.24f) return d >= 5 ? Kind.STADIUM : Kind.HOSPITAL;
            if (r < 0.27f) return Kind.HOSPITAL;
            if (r < 0.31f) return Kind.FIRE;
            if (r < 0.35f) return Kind.POLICE;
            if (r < 0.39f) return Kind.SCHOOL;
            if (r < 0.42f) return Kind.CONSTRUCTION;
            if (r < 0.46f) return Kind.CHURCH;
            if (r < 0.49f) return Kind.DINER;
            if (r < 0.52f) return Kind.MECHANIC;
            if (r < 0.55f) return Kind.DEALERSHIP;
            if (r < 0.57f) return Kind.BANK;
            if (r < 0.60f) return Kind.MOTEL;
            if (r < 0.74f) return Kind.ROWHOUSES;
            if (r >= 0.92f) {
                float q = rnd(bx, bz, 3);
                if (q < 0.27f) return Kind.PHARMACY;
                if (q < 0.54f) return Kind.HARDWARE;
                if (q < 0.8f) return Kind.GUN_STORE;
                if (d >= 6) return Kind.PRISON;
            }
            return Kind.APARTMENTS;
        }
        return r < 0.12f ? Kind.PARK : r < 0.17f ? Kind.GAS : r < 0.22f ? Kind.SCHOOL : r < 0.25f ? Kind.FIRE : r < 0.30f ? Kind.CHURCH
                : r < 0.34f ? Kind.DINER : r < 0.38f ? Kind.MOTEL : r < 0.41f ? Kind.MECHANIC
                : r < 0.93f ? Kind.HOUSES : r < 0.965f ? Kind.TRAILER_PARK : r < 0.985f ? Kind.PHARMACY : Kind.HARDWARE;
    }

    // ------------------------------------------------------------- column

    void column(int x, int z) {
        if (inAirport(x, z)) {
            int[] o = airportOrigin(x, z);
            airport(x, z, x - o[0], z - o[1]);
            return;
        }
        int ux = Math.floorMod(x, CELL), uz = Math.floorMod(z, CELL);
        boolean rx = ux >= R0 && ux < R1, rz = uz >= R0 && uz < R1;
        int bx = Math.floorDiv(x, CELL), bz = Math.floorDiv(z, CELL);
        boolean bridge = riverRow(bz) && uz >= C && ux < C;                // a north-south road crossing the river
        boolean country = (ux < C || uz < C) && !bridge && countryCorridor(bx, bz, ux, uz);
        if (country) {
            countryRoad(x, z, ux, uz);
            if (ux < C && uz >= C && railCell(bx, bz) && railCell(bx - 1, bz) && Math.abs(uz - C - RAIL_LZ) <= 6) railBridge(x, z, ux, uz - C);
        } else if (rx || rz) {
            road(x, z, ux, uz, rx, rz);
        } else if (ux < C || uz < C) {
            sidewalk(x, z, ux, uz);
        } else {
            int lx = ux - C, lz = uz - C;
            switch (kind(bx, bz)) {
                case TOWERS -> towersBlock(x, z, bx, bz, lx, lz);
                case APARTMENTS -> lotBuilding(x, z, bx, bz, lx, lz, false);
                case PARKING -> parking(x, z, lx, lz);
                case PARK -> {
                    park(x, z, lx, lz);
                    parkTrees(x, z, bx * CELL + C, bz * CELL + C);
                }
                case GAS -> gas(x, z, bx, bz, lx, lz);
                case HOUSES -> houses(x, z, bx, bz, lx, lz);
                case LANDMARK -> landmark(x, z, bx, bz, lx, lz);
                case PLAZA -> plaza(x, z, lx, lz);
                case HOSPITAL -> hospital(x, z, bx, bz, lx, lz);
                case FIRE -> fireStation(x, z, bx, bz, lx, lz);
                case POLICE -> police(x, z, bx, bz, lx, lz);
                case SCHOOL -> school(x, z, bx, bz, lx, lz);
                case STADIUM -> stadium(x, z, lx, lz);
                case ROWHOUSES -> rowhouses(x, z, bx, bz, lx, lz);
                case WAREHOUSE -> warehouse(x, z, bx, bz, lx, lz);
                case FACTORY -> factory(x, z, bx, bz, lx, lz);
                case CONTAINERS -> containers(x, z, bx, bz, lx, lz);
                case CONSTRUCTION -> construction(x, z, bx, bz, lx, lz);
                case RIVER -> river(x, z, lx, lz);
                case CHURCH -> church(x, z, bx, bz, lx, lz);
                case STATION -> station(x, z, bx, lx, lz);
                case FARM -> farm(x, z, bx, bz, lx, lz);
                case FOREST -> forest(x, z, bx, bz, lx, lz);
                case MEADOW -> meadow(x, z, bx, bz, lx, lz);
                case ORCHARD -> orchard(x, z, bx, bz, lx, lz);
                case DINER -> diner(x, z, bx, bz, lx, lz);
                case MOTEL -> motel(x, z, bx, bz, lx, lz);
                case DEALERSHIP -> dealership(x, z, bx, bz, lx, lz);
                case MECHANIC -> mechanic(x, z, bx, bz, lx, lz);
                case BANK -> bank(x, z, bx, bz, lx, lz);
                case PHARMACY -> store(x, z, bx, bz, lx, lz, "pharmacy", STYLES[6], Blocks.LIME_CONCRETE.getDefaultState(), false);
                case HARDWARE -> store(x, z, bx, bz, lx, lz, "hardware", STYLES[7], Blocks.ORANGE_CONCRETE.getDefaultState(), false);
                case GUN_STORE -> store(x, z, bx, bz, lx, lz, "gun_store", STYLES[14], Blocks.BLACK_CONCRETE.getDefaultState(), true);
                case PRISON -> prison(x, z, bx, bz, lx, lz);
                case TRAILER_PARK -> trailerPark(x, z, bx, bz, lx, lz);
                default -> { }
            }
            if (railCell(bx, bz) && !stationCell(bx, bz) && Math.abs(lz - RAIL_LZ) <= 6) railLine(x, z, lz);
        }
        if (bridge) {
            bridge(x, z, ux, uz - C);
            if (brokenBridge(bx, bz)) breakBridge(x, z, ux, uz - C);
        }
        trees(x, z);
    }

    // -------------------------------------------------------------- roads

    /**
     * Road surface for a north-south road at across-coordinate u (0..28) and along-coordinate v. Lanes:
     * 3-4 parking, 5 edge line, 6-8 lane, 9 dashes, 10-13 lane, 14 double yellow, then mirrored.
     */
    private BlockState roadSurface(int u, int v, int along, Direction.Axis axis, int seedA, int seedB) {
        Direction.Axis across = axis == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
        if (v >= CELL - 3 || v < 3 || (v >= 26 && v < 32)) return CityBlocks.axis(CityBlocks.CROSSWALK, axis);   // zebra crossings
        // stop lines before each crossing, only across the lanes heading into the junction (traffic drives on the right)
        boolean lowHalf = u >= 5 && u < 14, highHalf = u >= 15 && u < 24;
        boolean ns = axis == Direction.Axis.Z;
        if (v == CELL - 4 && (ns ? lowHalf : highHalf)) return CityBlocks.axis(CityBlocks.CROSSWALK, across);
        if (v == 32 && (ns ? highHalf : lowHalf)) return CityBlocks.axis(CityBlocks.CROSSWALK, across);
        if (u == 14) return CityBlocks.axis(CityBlocks.LINE_DOUBLE_YELLOW, axis);
        if (u == 5 || u == 23) return CityBlocks.axis(CityBlocks.LINE_WHITE, axis);
        if ((u == 9 || u == 19) && Math.floorMod(along, 8) < 4) return CityBlocks.axis(CityBlocks.LINE_WHITE, axis);
        if ((u == 11 || u == 17) && Math.floorMod(along, 40) == 20 && rnd(seedA, seedB, 50) < 0.6f) return CityBlocks.MANHOLE.getDefaultState();
        if ((u == 3 || u == 25) && Math.floorMod(along, 24) == 12) return CityBlocks.STORM_DRAIN.getDefaultState();
        return ASPHALT;
    }

    private void road(int x, int z, int ux, int uz, boolean rx, boolean rz) {
        int bx = Math.floorDiv(x, CELL), bz = Math.floorDiv(z, CELL);
        BlockState s = ASPHALT;
        if (rx && !rz) {
            s = roadSurface(ux, uz, z, Direction.Axis.Z, bx, z);
            // road works: cones down a lane, barricades at each end
            if (rnd(bx, bz, 60) < 0.1f) {
                if (ux == 10 && uz >= 45 && uz <= 60 && uz % 3 == 0) set(x, BASE, z, CityBlocks.facing(CityBlocks.TRAFFIC_CONE, Direction.NORTH));
                if ((uz == 44 || uz == 61) && ux >= 10 && ux < 14) set(x, BASE, z, CityBlocks.facing(CityBlocks.ROADBLOCK, uz == 44 ? Direction.NORTH : Direction.SOUTH));
            }
        } else if (rz && !rx) {
            s = roadSurface(uz, ux, x, Direction.Axis.X, x, bz);
            if (rnd(bx, bz, 61) < 0.1f) {
                if (uz == 18 && ux >= 45 && ux <= 60 && ux % 3 == 0) set(x, BASE, z, CityBlocks.facing(CityBlocks.TRAFFIC_CONE, Direction.WEST));
                if ((ux == 44 || ux == 61) && uz >= 15 && uz < 19) set(x, BASE, z, CityBlocks.facing(CityBlocks.ROADBLOCK, ux == 44 ? Direction.WEST : Direction.EAST));
            }
        } else if (ux == 14 && uz == 14 && rnd(bx, bz, 62) < 0.5f) {
            s = CityBlocks.MANHOLE.getDefaultState();
        }
        set(x, GROUND, z, s);
        if (s.getBlock() == CityBlocks.MANHOLE && rnd(x, z, 171) < 0.2f) set(x, BASE, z, CityBlocks.STEAM_VENT.getDefaultState());
    }

    private void sidewalk(int x, int z, int ux, int uz) {
        set(x, GROUND, z, WALK);
        boolean cornerX = ux == R0 - 1 || ux == R1, cornerZ = uz == R0 - 1 || uz == R1;
        if (cornerX && cornerZ) {
            trafficLight(x, z, uz == R0 - 1 ? Direction.NORTH : Direction.SOUTH, ux == R0 - 1 ? Direction.WEST : Direction.EAST);
            return;
        }
        boolean alongZ = uz >= C + 4 && uz < CELL - 4, alongX = ux >= C + 4 && ux < CELL - 4;
        boolean onBridge = riverRow(Math.floorDiv(z, CELL)) && uz >= C;
        if (!onBridge) {
            if (streetTreeAt(x, z)) {
                set(x, GROUND, z, Blocks.GRASS_BLOCK.getDefaultState());
                return;
            }
            // bus stop: glass shelter, bench and sign
            if (alongZ && ux <= 1 && uz >= 50 && uz <= 54) {
                busStop(x, z, ux, uz - 50, Direction.EAST);
                return;
            }
            if (alongX && uz <= 1 && ux >= 50 && ux <= 54) {
                busStop(x, z, uz, ux - 50, Direction.SOUTH);
                return;
            }
        }
        // street lamps on the kerb, arms reaching over the road
        if (alongZ && ux == R0 - 1 && Math.floorMod(z, 18) == 0) { lamp(x, z, Direction.EAST); return; }
        if (alongZ && ux == R1 && Math.floorMod(z, 18) == 9) { lamp(x, z, Direction.WEST); return; }
        if (alongX && uz == R0 - 1 && Math.floorMod(x, 18) == 0) { lamp(x, z, Direction.SOUTH); return; }
        if (alongX && uz == R1 && Math.floorMod(x, 18) == 9) { lamp(x, z, Direction.NORTH); return; }
        // hydrants and bins
        if (onBridge && (ux == 0 || ux == C - 1)) {
            set(x, BASE, z, Blocks.STONE_BRICK_WALL.getDefaultState());       // bridge railing
            return;
        }
        if (onBridge) {
            if (ux == R0 - 1 || ux == R1) set(x, GROUND, z, CURB_BLOCK);
            return;
        }
        if (alongZ && ux == 1 && Math.floorMod(z, 45) == 40) { set(x, BASE, z, CityBlocks.facing(CityBlocks.FIRE_HYDRANT, Direction.EAST)); return; }
        if (alongZ && ux == C - 2 && Math.floorMod(z, 45) == 18) { set(x, BASE, z, CityBlocks.facing(CityBlocks.FIRE_HYDRANT, Direction.WEST)); return; }
        if (alongZ && ux == 0 && Math.floorMod(z, 30) == 15) { set(x, BASE, z, CityBlocks.facing(CityBlocks.TRASH_BIN, Direction.EAST)); return; }
        if (alongX && uz == C - 1 && Math.floorMod(x, 30) == 15) { set(x, BASE, z, CityBlocks.facing(CityBlocks.TRASH_BIN, Direction.NORTH)); return; }
        boolean kerb = ux == R0 - 1 || ux == R1 || uz == R0 - 1 || uz == R1;
        if (kerb) set(x, GROUND, z, CURB_BLOCK);                             // flush kerbstones, no step up
        streetFurniture(x, z, ux, uz, alongX, alongZ);
    }

    /** parking meters downtown, newspaper boxes and mailboxes near the corners, planters on the wide pavements */
    private void streetFurniture(int x, int z, int ux, int uz, boolean alongX, boolean alongZ) {
        int bx = Math.floorDiv(x, CELL), bz = Math.floorDiv(z, CELL);
        int d = metroD(bx, bz);
        if (d <= 10) {
            if (alongZ && (ux == R0 - 1 || ux == R1) && Math.floorMod(z, 6) == 3)
                set(x, BASE, z, CityBlocks.facing(CityBlocks.PARKING_METER, ux == R0 - 1 ? Direction.EAST : Direction.WEST));
            if (alongX && (uz == R0 - 1 || uz == R1) && Math.floorMod(x, 6) == 3)
                set(x, BASE, z, CityBlocks.facing(CityBlocks.PARKING_METER, uz == R0 - 1 ? Direction.SOUTH : Direction.NORTH));
        }
        if (d > 10 && ((alongZ && ux == C - 2 && Math.floorMod(z, 20) == 10) || (alongX && uz == C - 2 && Math.floorMod(x, 20) == 10))) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.POWER_POLE, alongZ ? Direction.EAST : Direction.NORTH));   // suburban power lines
            return;
        }
        if (alongZ && ux == C - 2 && (uz == C + 6 || uz == CELL - 7)) {
            set(x, BASE, z, CityBlocks.facing(uz == C + 6 ? CityBlocks.NEWSPAPER_BOX : CityBlocks.MAILBOX, Direction.WEST));
            lootAt(x, BASE, z, "mail");
        }
        if (alongZ && ux == 1 && (uz == C + 8 || uz == CELL - 9)) {
            set(x, BASE, z, CityBlocks.facing(uz == C + 8 ? CityBlocks.MAILBOX : CityBlocks.NEWSPAPER_BOX, Direction.EAST));
            lootAt(x, BASE, z, "mail");
        }
        if (alongX && uz == C - 2 && ux == C + 10) { set(x, BASE, z, CityBlocks.facing(CityBlocks.NEWSPAPER_BOX, Direction.NORTH)); lootAt(x, BASE, z, "mail"); }
        if (alongX && uz == 1 && ux == CELL - 11) { set(x, BASE, z, CityBlocks.facing(CityBlocks.MAILBOX, Direction.SOUTH)); lootAt(x, BASE, z, "mail"); }
        if (d <= 4 && alongX && uz == C - 2 && Math.floorMod(x, 14) == 7) set(x, BASE, z, CityBlocks.facing(CityBlocks.PLANTER, Direction.NORTH));
    }

    private void lamp(int x, int z, Direction arm) {
        for (int y = BASE; y <= BASE + 5; y++) set(x, y, z, POLE);
        set(x, BASE + 6, z, CityBlocks.facing(CityBlocks.STREET_LAMP, arm));
    }

    private void trafficLight(int x, int z, Direction a, Direction b) {
        int d = metroD(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        if (d > 10) {                                                        // quiet suburbs get a four-way stop
            set(x, BASE, z, CityBlocks.facing(CityBlocks.STOP_SIGN, a));
            return;
        }
        for (int y = BASE; y <= BASE + 3; y++) set(x, y, z, POLE);
        for (int i = 0; i < 2; i++) {
            set(x, BASE + 4 + i, z, CityBlocks.facing(CityBlocks.TRAFFIC_LIGHT, i == 0 ? a : b));
            pos.set(x, BASE + 4 + i, z);
            w.scheduleBlockTick(pos.toImmutable(), CityBlocks.TRAFFIC_LIGHT, 1 + i);
        }
        set(x, BASE + 6, z, POLE);
    }

    // ---------------------------------------------------------- buildings

    private void lotBuilding(int x, int z, int bx, int bz, int lx, int lz, boolean towers) {
        int li = lx / 26, lj = lz / 26, px = lx % 26, pz = lz % 26;
        int lotX = bx * 4 + li, lotZ = bz * 4 + lj;
        int ox = bx * CELL + C + li * 26, oz = bz * CELL + C + lj * 26;
        float r = rnd(lotX, lotZ, 3);
        int d = metroD(bx, bz);
        int floors = towers ? 10 + (int) (r * 28) + (d <= 1 ? 10 : 0) : 2 + (int) (r * 9);
        int[] pool = towers ? TOWER_STYLES : APT_STYLES;
        int style = pool[hash(lotX, lotZ, 4) % pool.length];
        Direction front = lj == 0 ? Direction.NORTH : Direction.SOUTH;
        Roof roof;
        float rr = rnd(lotX, lotZ, 6);
        if (towers) roof = floors >= 16 && rr < 0.35f ? Roof.HELIPAD : rr < 0.6f ? Roof.SPIRE : rr < 0.8f ? Roof.GARDEN : Roof.PLAIN;
        else roof = rr < 0.35f ? Roof.WATER_TOWER : rr < 0.55f ? Roof.GARDEN : Roof.PLAIN;
        int every = towers && rnd(lotX, lotZ, 7) < 0.5f ? 6 + hash(lotX, lotZ, 8) % 5 : 0;
        if (px < 2 || px > 23 || pz < 2 || pz > 23) {
            boolean walkway = (px == 12 || px == 13) && (front == Direction.NORTH ? pz < 2 : pz > 23);
            set(x, GROUND, z, towers || walkway ? PLAZA : Blocks.GRASS_BLOCK.getDefaultState());
            return;
        }
        loot = towers ? "office" : "apartment";
        building(x, z, ox + 2, oz + 2, ox + 23, oz + 23, floors, STYLES[style], front, roof, every, 2);
    }

    /** downtown block: four towers, or one huge tower that fills the whole block */
    private void towersBlock(int x, int z, int bx, int bz, int lx, int lz) {
        if (rnd(bx, bz, 5) < 0.3f) {
            int ox = bx * CELL + C, oz = bz * CELL + C;
            if (lx < 3 || lx > 48 || lz < 3 || lz > 48) {
                set(x, GROUND, z, (lx + lz) % 2 == 0 ? PLAZA : Blocks.POLISHED_DIORITE.getDefaultState());
                return;
            }
            int floors = 22 + hash(bx, bz, 9) % 24;
            loot = "office";
            int style = TOWER_STYLES[hash(bx, bz, 10) % TOWER_STYLES.length];
            building(x, z, ox + 3, oz + 3, ox + 48, oz + 48, floors, STYLES[style], Direction.NORTH,
                    rnd(bx, bz, 11) < 0.5f ? Roof.SPIRE : Roof.HELIPAD, 5 + hash(bx, bz, 12) % 4, 3);
            return;
        }
        lotBuilding(x, z, bx, bz, lx, lz, true);
    }

    private void landmark(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx < 2 || lx > 49 || lz < 2 || lz > 49) {
            set(x, GROUND, z, (lx + lz) % 2 == 0 ? PLAZA : Blocks.POLISHED_DIORITE.getDefaultState());
            return;
        }
        loot = "office";
        building(x, z, ox + 2, oz + 2, ox + 49, oz + 49, 55, STYLES[8], Direction.NORTH, Roof.SPIRE, 9, 4);
    }

    private void building(int x, int z, int x0, int z0, int x1, int z1, int floors, Style st, Direction front, boolean helipad) {
        building(x, z, x0, z0, x1, z1, floors, st, front, helipad ? Roof.HELIPAD : Roof.PLAIN, 0, 0);
    }

    /**
     * One column of a building: floors every 4 blocks with lights set into them, windows on every facade, a 2-high
     * entrance on the front, a ladder shaft up the middle to the roof, optional setbacks (the tower steps in by
     * `step` blocks every `every` floors, leaving terraces) and a roof: helipad, spire, water tower or garden.
     */
    private void building(int x, int z, int x0, int z0, int x1, int z1, int floors, Style st, Direction front, Roof roofType, int every, int step) {
        floors = Math.min(floors, (318 - BASE - 30) / 4);
        int top = BASE + floors * 4;
        set(x, GROUND, z, st.frame);
        int maxK = Math.max(0, (Math.min(x1 - x0, z1 - z0) - 10) / 2);
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2 + 2;                       // ladder shaft
        boolean ladderCol = x == mx && z == mz, shaftCol = x == mx - 1 && z == mz;
        boolean wasEdge = false;
        int k = 0;
        for (int y = BASE; y < top; y++) {
            int rel = y - BASE, floor = rel / 4;
            k = every > 0 ? Math.min(maxK, (floor / every) * step) : 0;
            int bx0 = x0 + k, bx1 = x1 - k, bz0 = z0 + k, bz1 = z1 - k;
            if (x < bx0 || x > bx1 || z < bz0 || z > bz1) {
                set(x, y, z, st.frame);                                      // setback terrace
                if (wasEdge) set(x, y + 1, z, st.frame);
                else if (rnd(x, z, 15) < 0.08f) set(x, y + 1, z, Blocks.FLOWERING_AZALEA.getDefaultState());
                return;
            }
            boolean ex = x == bx0 || x == bx1, ez = z == bz0 || z == bz1;
            boolean edge = ex || ez, corner = ex && ez;
            wasEdge = edge;
            int along = ex ? z : x;
            boolean frontFace = k == 0 && switch (front) {
                case NORTH -> z == bz0;
                case SOUTH -> z == bz1;
                case WEST -> x == bx0;
                default -> x == bx1;
            };
            int mid = front.getAxis() == Direction.Axis.Z ? (bx0 + bx1) / 2 : (bz0 + bz1) / 2;
            boolean doorCol = frontFace && !corner && (along == mid || along == mid + 1);
            boolean floorRow = rel % 4 == 0;
            BlockState s;
            if (edge) {
                if (corner || floorRow) s = st.frame;
                else if (doorCol && rel < 3) s = AIR;
                else if (rel < 4 && frontFace && st.shopFront) s = st.window;
                else if (rel % 4 == 3 && !st.tallWindows) s = st.wall;
                else s = Math.floorMod(along, st.bay) == 0 ? st.wall : st.window;
            } else if (ladderCol) {
                s = LADDER;
            } else if (shaftCol) {
                s = st.frame;
            } else if (floorRow) {
                s = Math.floorMod(x + z * 3, 7) == 0 ? CEILING_LIGHT : INNER_FLOOR;
            } else {
                BlockState f = furnish(x, y, z, rel, bx0, bx1, bz0, bz1, mx, mz, st, front, mid);
                if (f != null) {
                    if (f != AIR) {
                        if (f.getBlock() == CityBlocks.COMPUTER && !com.nothatcher.vehicleforge.power.PowerGrid.MAINS_DOWN && rnd(x, y * 13 + z, 174) < 0.4f)
                            f = f.with(net.minecraft.state.property.Properties.LIT, true);
                        set(x, y, z, f);
                        if (f.getBlock() instanceof com.nothatcher.vehicleforge.block.StorageProp) lootAt(x, y, z, propLoot(f.getBlock()));
                    }
                    continue;
                }
                // cupboards and chests against the walls on every floor
                boolean nearWall = x == bx0 + 1 || x == bx1 - 1 || z == bz0 + 1 || z == bz1 - 1;
                if (rel % 4 == 1 && nearWall && rnd(x * 7 + rel, z, 90) < 0.045f) {
                    container(x, y, z, rel < 4 && st.shopFront ? shopLoot() : loot, rnd(x, z * 3 + rel, 91) < 0.4f);
                }
                continue;
            }
            set(x, y, z, s);
        }
        // ---- roof
        int bx0 = x0 + k, bx1 = x1 - k, bz0 = z0 + k, bz1 = z1 - k;
        boolean edge = x == bx0 || x == bx1 || z == bz0 || z == bz1;
        if (ladderCol) {
            set(x, top, z, LADDER);
            return;
        }
        BlockState roof = st.frame;
        double cx = x - (bx0 + bx1) / 2.0, cz = z - (bz0 + bz1 - 6) / 2.0;
        double ax = Math.abs(cx), az = Math.abs(cz);
        if (roofType == Roof.HELIPAD && !edge && ax <= 5 && az <= 5) {
            if (Math.max(ax, az) > 4) roof = LINE_Y;
            else if ((ax >= 1.5 && ax <= 2.5 && az <= 2.5) || (az < 1 && ax <= 2)) roof = LINE_W;
            else roof = ASPHALT;
        }
        if (roofType == Roof.GARDEN && !edge) roof = Blocks.GRASS_BLOCK.getDefaultState();
        set(x, top, z, roof);
        if (edge) {
            set(x, top + 1, z, st.frame);
            return;
        }
        if (OVERRUN && roofType != Roof.SPIRE && rnd(x0, z0, 172) < 0.14f) {        // a fire still smouldering on the roof
            int fx = (bx0 + bx1) / 2 + 2, fz = (bz0 + bz1) / 2 - 4;
            int ddx = x - fx, ddz = z - fz;
            if (ddx * ddx + ddz * ddz <= 6) set(x, top, z, Blocks.BLACK_CONCRETE.getDefaultState());
            if ((ddx == 0 && ddz == 0) || (ddx == 1 && ddz == -1)) {
                set(x, top + 1, z, CityBlocks.SMOKE_SOURCE.getDefaultState());
                return;
            }
        }
        if (roofType == Roof.PLAIN && bx1 - bx0 >= 14 && rnd(x0, z0, 173) < 0.45f && x >= bx0 + 8 && x <= bx1 - 3 && z >= bz0 + 3 && z <= bz0 + 9
                && (z - bz0) % 3 == 0) {
            set(x, top + 1, z, CityBlocks.facing(CityBlocks.SOLAR_PANEL, Direction.NORTH));
            return;
        }
        switch (roofType) {
            case SPIRE -> {
                if (ax <= 1 && az <= 1) {
                    int h = Math.min(36, 316 - top);
                    for (int i = 1; i <= h / 3; i++) set(x, top + i, z, st.frame);
                    if (cx > 0 && cz > 0) {                                      // the mast rises from one corner of the base
                        for (int i = h / 3 + 1; i < h; i++) set(x, top + i, z, Blocks.IRON_BARS.getDefaultState());
                        set(x, top + h, z, Blocks.SHROOMLIGHT.getDefaultState());   // aviation light
                    }
                }
            }
            case WATER_TOWER -> {
                int tx = x - bx0, tz = z - bz0;
                if (tx >= 2 && tx <= 4 && tz >= 2 && tz <= 4) {
                    boolean leg = (tx == 2 || tx == 4) && (tz == 2 || tz == 4);
                    for (int i = 1; i <= 3; i++) if (leg) set(x, top + i, z, Blocks.SPRUCE_FENCE.getDefaultState());
                    for (int i = 4; i <= 7; i++) set(x, top + i, z, Blocks.SPRUCE_PLANKS.getDefaultState());
                    set(x, top + 8, z, Blocks.DARK_OAK_SLAB.getDefaultState());
                }
            }
            case GARDEN -> {
                if (rnd(x, z, 16) < 0.12f) set(x, top + 1, z, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
                else if (rnd(x, z, 17) < 0.1f) set(x, top + 1, z, FLOWERS[hash(x, z, 18) % FLOWERS.length]);
            }
            case PLAIN -> {
                if (x >= bx0 + 3 && x <= bx0 + 6 && z >= bz1 - 5 && z <= bz1 - 3 && (x + z) % 2 == 0) {
                    set(x, top + 1, z, CityBlocks.facing(CityBlocks.AC_UNIT, Direction.NORTH));   // rooftop air conditioning
                }
                if (x == bx1 - 3 && z == bz0 + 3) {
                    for (int i = 1; i <= 3; i++) set(x, top + i, z, Blocks.IRON_BARS.getDefaultState());   // vent stack
                }
            }
            default -> { }
        }
    }

    // ============================================================ countryside

    /** is this corridor (road strip) out in the countryside? Every block it touches must be country. */
    private boolean countryCorridor(int bx, int bz, int ux, int uz) {
        if (!countryCell(bx, bz)) return false;
        if (ux < C && !countryCell(bx - 1, bz)) return false;
        if (uz < C && !countryCell(bx, bz - 1)) return false;
        return !(ux < C && uz < C) || countryCell(bx - 1, bz - 1);
    }

    /** a two-lane dirt road: packed dirt with worn ruts and potholes, grassy shoulders, fences and power lines */
    private void countryRoad(int x, int z, int ux, int uz) {
        boolean ns = ux < C, ew = uz < C;
        set(x, GROUND, z, Blocks.GRASS_BLOCK.getDefaultState());
        if (ns && ew) {                                                      // crossroads
            if ((ux >= 9 && ux <= 19) || (uz >= 9 && uz <= 19)) dirtRoad(x, z, -1);
            else if ((ux >= 7 && ux <= 21) || (uz >= 7 && uz <= 21)) shoulder(x, z);
            if ((ux == 6 || ux == 22) && (uz == 6 || uz == 22)) set(x, BASE, z, CityBlocks.facing(CityBlocks.STOP_SIGN, uz == 6 ? Direction.NORTH : Direction.SOUTH));
            return;
        }
        int u = ns ? ux : uz;
        int v = ns ? uz : ux;
        int along = ns ? z : x;
        if (u >= 9 && u <= 19) {
            dirtRoad(x, z, u);
            return;
        }
        if (u == 7 || u == 8 || u == 20 || u == 21) {
            shoulder(x, z);
            return;
        }
        boolean nearEnd = v < C + 3 || v > CELL - 4;
        if ((u == 3 || u == 25) && !nearEnd && Math.floorMod(along, 40) > 3) {
            set(x, BASE, z, Blocks.OAK_FENCE.getDefaultState());
            return;
        }
        if (u == 5 && !nearEnd && Math.floorMod(along, 24) == 12) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.POWER_POLE, ns ? Direction.EAST : Direction.NORTH));
            return;
        }
        float f = rnd(x, z, 200);
        if (f < 0.12f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
        else if (f < 0.15f) set(x, BASE, z, FLOWERS[hash(x, z, 201) % FLOWERS.length]);
    }

    /**
     * One column of dirt road. u is the distance across the road (9 to 19), or -1 at a crossroads.
     * Packed dirt, with coarse dirt worn into the wheel ruts and the crown, and potholes: dry ones a block deep,
     * or full of muddy rainwater.
     */
    private void dirtRoad(int x, int z, int u) {
        int hole = pothole(x, z);
        if (hole == 1) {
            set(x, GROUND, z, AIR);
            set(x, GROUND - 1, z, Blocks.COARSE_DIRT.getDefaultState());
            return;
        }
        if (hole == 2) {
            set(x, GROUND, z, Blocks.WATER.getDefaultState());
            set(x, GROUND - 1, z, Blocks.MUD.getDefaultState());
            return;
        }
        float f = rnd(x, z, 310);
        boolean rut = u == 11 || u == 12 || u == 16 || u == 17;
        boolean crown = u == 14;
        boolean worn = hole == 3 || f < (rut ? 0.35f : crown ? 0.5f : 0.08f);
        set(x, GROUND, z, worn ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT_PATH.getDefaultState());
    }

    /** gravel and bare dirt mixed into the grass beside a dirt road */
    private void shoulder(int x, int z) {
        float f = rnd(x, z, 320);
        if (f < 0.12f) set(x, GROUND, z, Blocks.GRAVEL.getDefaultState());
        else if (f < 0.4f) set(x, GROUND, z, Blocks.COARSE_DIRT.getDefaultState());
    }

    /**
     * Potholes are scattered on a 5x5 grid: about one grid square in six holds one, a rough blob one to three
     * blocks across. 0 = none, 1 = a dry hole, 2 = a hole full of water, 3 = the broken rim around a hole.
     */
    private int pothole(int x, int z) {
        int gx = Math.floorDiv(x, 5), gz = Math.floorDiv(z, 5);
        if (rnd(gx, gz, 300) >= 0.17f) return 0;
        float cx = gx * 5 + 1.5f + rnd(gx, gz, 301) * 2f, cz = gz * 5 + 1.5f + rnd(gx, gz, 302) * 2f;
        float r = 0.6f + rnd(gx, gz, 303) * 1.0f;
        float dx = x + 0.5f - cx, dz = z + 0.5f - cz;
        float d2 = dx * dx + dz * dz + (rnd(x, z, 304) - 0.5f) * 0.8f;
        if (d2 <= r * r) return rnd(gx, gz, 305) < 0.4f ? 2 : 1;
        return d2 <= (r + 1f) * (r + 1f) ? 3 : 0;
    }

    /** the railway embankment and track running east-west through a countryside block */
    private void railLine(int x, int z, int lz) {
        int dz = Math.abs(lz - RAIL_LZ);
        int top = dz <= 2 ? RAIL_Y - 1 : RAIL_Y - 1 - (dz - 2);
        for (int y = BASE; y <= BASE + 16; y++) set(x, y, z, AIR);
        set(x, GROUND, z, Blocks.DIRT.getDefaultState());
        for (int y = BASE; y <= top; y++) {
            BlockState b;
            if (y == top) b = dz <= 2 ? Blocks.GRAVEL.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
            else b = dz <= 2 ? Blocks.STONE.getDefaultState() : Blocks.DIRT.getDefaultState();
            set(x, y, z, b);
        }
        if (dz <= 1 && Math.floorMod(x, 2) == 0) set(x, top, z, Blocks.SPRUCE_PLANKS.getDefaultState());     // sleepers
        if (dz == 0) set(x, RAIL_Y, z, Blocks.RAIL.getDefaultState().with(net.minecraft.state.property.Properties.RAIL_SHAPE, net.minecraft.block.enums.RailShape.EAST_WEST));
        if (dz == 2 && Math.floorMod(x, 48) == 24) set(x, RAIL_Y, z, Blocks.REDSTONE_TORCH.getDefaultState());   // signal light
    }

    /** a steel girder bridge carrying the railway over a north-south road */
    private void railBridge(int x, int z, int ux, int lz) {
        int dz = Math.abs(lz - RAIL_LZ);
        if (ux <= 2 || ux >= 26) {                                           // abutments
            railLine(x, z, lz);
            if (dz <= 3) for (int y = BASE; y < RAIL_Y - 1; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            return;
        }
        if (dz > 3) return;
        for (int y = BASE; y <= BASE + 10; y++) set(x, y, z, AIR);
        if (dz <= 2) {
            set(x, RAIL_Y - 1, z, dz == 2 ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.POLISHED_BLACKSTONE.getDefaultState());
            if (Math.floorMod(x, 5) == 0) set(x, RAIL_Y - 2, z, Blocks.IRON_BLOCK.getDefaultState());    // cross beams
            if (dz == 0) set(x, RAIL_Y, z, Blocks.RAIL.getDefaultState().with(net.minecraft.state.property.Properties.RAIL_SHAPE, net.minecraft.block.enums.RailShape.EAST_WEST));
            return;
        }
        // side girders: a truss of iron with bars between the verticals
        set(x, RAIL_Y - 2, z, Blocks.IRON_BLOCK.getDefaultState());
        set(x, RAIL_Y - 1, z, Blocks.IRON_BLOCK.getDefaultState());
        for (int y = RAIL_Y; y <= RAIL_Y + 1; y++) set(x, y, z, Math.floorMod(x, 4) == 0 ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.IRON_BARS.getDefaultState()
                .with(net.minecraft.block.HorizontalConnectingBlock.EAST, true).with(net.minecraft.block.HorizontalConnectingBlock.WEST, true));
        set(x, RAIL_Y + 2, z, Blocks.IRON_BLOCK.getDefaultState());
    }

    /**
     * The train station at the edge of the city: a brick hall with a clock tower, ticket counters, benches and
     * departure boards; stairs up to two covered platforms either side of the track, which ends at a buffer stop.
     */
    private void station(int x, int z, int bx, int lx, int lz) {
        boolean east = local(bx) > 0;
        int s = east ? lx : 51 - lx;                                         // 0 on the city side, 51 where the line comes in
        Direction out = east ? Direction.EAST : Direction.WEST;
        int dz = Math.abs(lz - RAIL_LZ);
        set(x, GROUND, z, lz <= 5 ? PLAZA : Blocks.GRASS_BLOCK.getDefaultState());
        // embankment where the line comes in past the platforms
        if (s >= 49 && dz <= 6) {
            railLine(x, z, lz);
            return;
        }
        // track bed and buffer stop
        if (dz <= 1 && s >= 10) {
            for (int y = BASE; y < RAIL_Y - 1; y++) set(x, y, z, Blocks.STONE.getDefaultState());
            set(x, RAIL_Y - 1, z, s == 10 ? Blocks.SMOOTH_STONE.getDefaultState() : dz == 0 && s % 2 == 0 ? Blocks.SPRUCE_PLANKS.getDefaultState() : Blocks.GRAVEL.getDefaultState());
            if (s == 10) set(x, RAIL_Y, z, AIR);
            else if (dz == 0 && s == 11) {
                set(x, RAIL_Y, z, Blocks.IRON_BLOCK.getDefaultState());       // buffer stop
                set(x, RAIL_Y + 1, z, Blocks.RED_CONCRETE.getDefaultState());
            } else if (dz == 0) set(x, RAIL_Y, z, Blocks.RAIL.getDefaultState().with(net.minecraft.state.property.Properties.RAIL_SHAPE, net.minecraft.block.enums.RailShape.EAST_WEST));
            if (s == 10) set(x, RAIL_Y - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
            canopy(x, z, s, lz);
            return;
        }
        // platforms
        if (dz >= 2 && dz <= 4 && s >= 10 && s <= 48) {
            for (int y = BASE; y < RAIL_Y; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            set(x, RAIL_Y, z, dz == 2 ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.SMOOTH_STONE.getDefaultState());
            if (dz == 4 && s % 8 == 4) set(x, RAIL_Y + 1, z, CityBlocks.facing(CityBlocks.BENCH, lz < RAIL_LZ ? Direction.SOUTH : Direction.NORTH));
            if (dz == 3 && s == 30) set(x, RAIL_Y + 1, z, CityBlocks.facing(CityBlocks.VENDING_MACHINE, lz < RAIL_LZ ? Direction.SOUTH : Direction.NORTH));
            canopy(x, z, s, lz);
            return;
        }
        if (dz <= 6 && s >= 10) {                                            // embankment sides beside the platforms
            for (int y = BASE; y < RAIL_Y - (dz - 4); y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            return;
        }
        // stairs up from the back of the hall to the north platform
        if (s >= 20 && s <= 22 && lz >= 29 && lz <= 35) {
            int k = lz - 29;
            int top = Math.min(BASE + k, RAIL_Y);
            for (int y = BASE; y < top; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            set(x, top, z, k >= 6 ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
            return;
        }
        if ((s == 19 || s == 23) && lz >= 29 && lz <= 35) {
            for (int y = BASE; y <= Math.min(BASE + lz - 29, RAIL_Y) + 1; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            return;
        }
        // clock tower in front of the hall
        if (s >= 28 && s <= 32 && lz >= 2 && lz <= 6) {
            stationTower(x, z, s, lz, out);
            return;
        }
        // the hall
        if (s >= 8 && s <= 40 && lz >= 7 && lz <= 28) {
            stationHall(x, z, s, lz, out);
            return;
        }
        // car park and taxi rank
        if (s >= 43 && lz <= 30) {
            set(x, GROUND, z, (lz % 6 == 0 && s % 3 != 0) ? CityBlocks.axis(CityBlocks.LINE_WHITE, out.getAxis()) : ASPHALT);
            return;
        }
        if (lz == 4 && s == 14) set(x, BASE, z, CityBlocks.facing(CityBlocks.BUS_STOP_SIGN, Direction.NORTH));
        if (lz == 4 && (s == 6 || s == 40)) lamp(x, z, Direction.NORTH);
        if (lz == 5 && s % 9 == 3) set(x, BASE, z, CityBlocks.facing(CityBlocks.PLANTER, Direction.NORTH));
    }

    private void canopy(int x, int z, int s, int lz) {
        if (s < 14 || s > 46) return;
        int dz = Math.abs(lz - RAIL_LZ);
        if (dz == 3 && s % 6 == 2) for (int y = RAIL_Y + 1; y <= RAIL_Y + 4; y++) set(x, y, z, Blocks.BLACK_CONCRETE.getDefaultState());
        if (dz <= 4) set(x, RAIL_Y + 5, z, dz <= 1 ? CLEAR : Blocks.DEEPSLATE_TILE_SLAB.getDefaultState());
        if (dz == 3 && s % 6 == 5) set(x, RAIL_Y + 4, z, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
    }

    private void stationTower(int x, int z, int s, int lz, Direction out) {
        boolean wall = s == 28 || s == 32 || lz == 2 || lz == 6;
        boolean corner = (s == 28 || s == 32) && (lz == 2 || lz == 6);
        int top = BASE + 20;
        set(x, GROUND, z, Blocks.POLISHED_ANDESITE.getDefaultState());
        if (!wall) {
            set(x, top, z, Blocks.BRICKS.getDefaultState());
        } else {
            for (int y = BASE; y <= top; y++) {
                BlockState b = corner ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.BRICKS.getDefaultState();
                if (lz == 2 && s >= 29 && s <= 31 && y >= BASE + 14 && y <= BASE + 16) {   // the clock face
                    boolean centre = s == 30 && y == BASE + 15, hand = s == 30 && y == BASE + 16 || s == 31 && y == BASE + 15;
                    b = centre || hand ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.QUARTZ_BLOCK.getDefaultState();
                }
                if (lz == 2 && s == 30 && y >= BASE && y <= BASE + 2) b = y == BASE + 2 ? Blocks.STONE_BRICKS.getDefaultState() : AIR;   // way in
                set(x, y, z, b);
            }
        }
        for (int k = 0; k < 4; k++) {                                        // pyramid roof
            int y = top + 1 + k;
            if (s >= 28 + k && s <= 32 - k && lz >= 2 + k && lz <= 6 - k) set(x, y, z, Blocks.DEEPSLATE_TILES.getDefaultState());
        }
        if (s == 30 && lz == 4) {
            set(x, top + 5, z, Blocks.IRON_BARS.getDefaultState());
            set(x, top + 6, z, Blocks.LIGHTNING_ROD.getDefaultState());
        }
    }

    private void stationHall(int x, int z, int s, int lz, Direction out) {
        boolean wall = s == 8 || s == 40 || lz == 7 || lz == 28;
        int top = BASE + 8;
        set(x, GROUND, z, (s + lz) % 2 == 0 ? Blocks.POLISHED_ANDESITE.getDefaultState() : Blocks.POLISHED_DIORITE.getDefaultState());
        set(x, top + 1, z, Blocks.STONE_BRICK_SLAB.getDefaultState());
        if (wall) {
            for (int y = BASE; y <= top; y++) {
                BlockState b = Blocks.BRICKS.getDefaultState();
                boolean corner = (s == 8 || s == 40) && (lz == 7 || lz == 28);
                if (corner || y == top) b = Blocks.STONE_BRICKS.getDefaultState();
                if (!corner && y >= BASE + 1 && y <= BASE + 5 && ((lz == 7 && s % 4 == 2) || ((s == 8 || s == 40) && lz % 4 == 1))) b = CLEAR;   // tall windows
                if (lz == 7 && s >= 17 && s <= 19 && y <= BASE + 3) b = AIR;                                         // front doors
                if (lz == 28 && s >= 20 && s <= 22 && y <= BASE + 3) b = AIR;                                        // to the platforms
                set(x, y, z, b);
            }
            if (lz == 28 && s >= 22 && s <= 26) set(x, BASE + 4, z, CityBlocks.facing(CityBlocks.TV, Direction.NORTH).with(net.minecraft.state.property.Properties.LIT,
                    !com.nothatcher.vehicleforge.power.PowerGrid.MAINS_DOWN));   // departure board
            return;
        }
        set(x, top, z, (s % 5 == 2 && lz % 5 == 2) ? CEILING_LIGHT : Blocks.WHITE_CONCRETE.getDefaultState());
        if (lz == 25 && s >= 11 && s <= 19 && s % 2 == 1) { set(x, BASE, z, f(CityBlocks.CHECKOUT_COUNTER, Direction.NORTH)); lootAt(x, BASE, z, "shop"); }
        else if (lz >= 11 && lz <= 21 && lz % 3 == 2 && ((s >= 11 && s <= 15) || (s >= 25 && s <= 37))) set(x, BASE, z, f(CityBlocks.PEW, Direction.NORTH));
        else if (lz == 9 && s == 10) set(x, BASE, z, f(CityBlocks.VENDING_MACHINE, Direction.EAST));
        else if (lz == 9 && s == 38) set(x, BASE, z, f(CityBlocks.ATM, Direction.WEST));
        else if ((s == 9 || s == 39) && (lz == 8 || lz == 27)) set(x, BASE, z, f(CityBlocks.POTTED_PLANT, Direction.NORTH));
    }

    /** a farm: farmhouse, red barn, silo, and either crop fields with irrigation channels or a fenced pasture */
    private void farm(int x, int z, int bx, int bz, int lx, int lz) {
        if (lx < 17 && lz < 17) {                                            // farmhouse on its own lot
            Lot l = lot(bx, bz, 0, 0);
            HouseStyle hs = HOUSES[hash(bx, bz, 202) % HOUSES.length];
            if (bunker(l)) bunkerColumn(x, z, l);
            if (x >= l.hx0 - 1 && x <= l.hx1 + 1 && z >= l.hz0 - 1 && z <= l.hz1 + 1) houseColumn(x, z, l, hs);
            else if (lx >= 7 && lx <= 9 && lz < 4) set(x, GROUND, z, Blocks.GRAVEL.getDefaultState());
            return;
        }
        if (lx >= 22 && lx <= 37 && lz >= 2 && lz <= 16) {
            barn(x, z, bx, bz, lx - 22, lz - 2);
            return;
        }
        double sdx = lx - 44.5, sdz = lz - 8.5, sr = Math.sqrt(sdx * sdx + sdz * sdz);
        if (sr < 4) {
            silo(x, z, sr, lx, lz);
            return;
        }
        if (lz >= 0 && lz < 19 && lx >= 18 && lx <= 20) {                    // farm track
            set(x, GROUND, z, Blocks.DIRT_PATH.getDefaultState());
            return;
        }
        if (lz < 19) {
            if (rnd(x, z, 203) < 0.02f) set(x, BASE, z, Blocks.HAY_BLOCK.getDefaultState());
            return;
        }
        boolean pasture = hash(bx, bz, 204) % 3 == 0;
        if (pasture) {
            boolean edge = lx == 1 || lx == 50 || lz == 19 || lz == 50;
            if (edge) {
                if (!(lz == 19 && lx >= 18 && lx <= 20)) set(x, BASE, z, Blocks.OAK_FENCE.getDefaultState());
                else set(x, GROUND, z, Blocks.DIRT_PATH.getDefaultState());
                return;
            }
            if (lx == 25 && lz == 30) set(x, BASE, z, Blocks.WATER_CAULDRON.getDefaultState().with(net.minecraft.block.LeveledCauldronBlock.LEVEL, 3));
            else if (rnd(x, z, 205) < 0.015f) set(x, BASE, z, Blocks.HAY_BLOCK.getDefaultState());
            else if (rnd(x, z, 206) < 0.2f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
            return;
        }
        // crops in four strips, irrigation every 9 rows, a scarecrow in each strip
        if (lx == 0 || lx == 51) return;
        if ((lz - 19) % 9 == 4) {
            set(x, GROUND, z, WATER);
            return;
        }
        int strip = lx / 13;
        if (lx % 13 == 6 && lz == 33) {
            set(x, GROUND, z, Blocks.DIRT.getDefaultState());
            set(x, BASE, z, Blocks.OAK_FENCE.getDefaultState());
            set(x, BASE + 1, z, Blocks.HAY_BLOCK.getDefaultState());
            set(x, BASE + 2, z, Blocks.CARVED_PUMPKIN.getDefaultState().with(net.minecraft.block.CarvedPumpkinBlock.FACING, Direction.NORTH));
            return;
        }
        set(x, GROUND, z, Blocks.FARMLAND.getDefaultState().with(net.minecraft.block.FarmlandBlock.MOISTURE, 7));
        BlockState crop = switch ((strip + hash(bx, bz, 207)) % 4) {
            case 0 -> Blocks.WHEAT.getDefaultState().with(net.minecraft.block.CropBlock.AGE, 7);
            case 1 -> Blocks.CARROTS.getDefaultState().with(net.minecraft.block.CropBlock.AGE, 7);
            case 2 -> Blocks.POTATOES.getDefaultState().with(net.minecraft.block.CropBlock.AGE, 7);
            default -> Blocks.BEETROOTS.getDefaultState().with(net.minecraft.block.BeetrootsBlock.AGE, 3);
        };
        if (rnd(x, z, 208) < (OVERRUN ? 0.55f : 0.95f)) set(x, BASE, z, crop);   // fields going to ruin in an outbreak
    }

    /** red barn with a big door, hay inside and a gambrel-ish roof; bx/bz give each barn its own look */
    private void barn(int x, int z, int bx, int bz, int px, int pz) {
        // px 0..15 along x, pz 0..14 along z; door on the south side
        boolean wall = px == 0 || px == 15 || pz == 0 || pz == 14;
        boolean corner = (px == 0 || px == 15) && (pz == 0 || pz == 14);
        set(x, GROUND, z, Blocks.SPRUCE_PLANKS.getDefaultState());
        int e = Math.min(pz, 14 - pz);                                       // 0 at the eaves, 7 at the ridge
        int roofY = BASE + 7 + (e <= 3 ? e * 2 : 6 + (e - 3));
        if (wall) {
            for (int y = BASE; y < BASE + 7; y++) {
                BlockState b = corner ? Blocks.DARK_OAK_LOG.getDefaultState() : Blocks.RED_TERRACOTTA.getDefaultState();
                if (pz == 14 && px >= 5 && px <= 10 && y <= BASE + 4) b = (px == 5 || px == 10) ? Blocks.WHITE_TERRACOTTA.getDefaultState() : AIR;
                if ((px == 0 || px == 15) && pz % 4 == 2 && y == BASE + 3) b = Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState();
                set(x, y, z, b);
            }
            if (px == 0 || px == 15) for (int y = BASE + 7; y < roofY; y++) set(x, y, z, Blocks.RED_TERRACOTTA.getDefaultState());   // gable ends
        } else {
            if (rnd(x, z, 209) < 0.25f && (px <= 3 || px >= 12)) {
                int h = 1 + hash(x, z, 210) % 3;
                for (int y = BASE; y < BASE + h; y++) set(x, y, z, Blocks.HAY_BLOCK.getDefaultState());
            }
            if (px == 1 && pz == 1) container(x, BASE, z, "farm", true);
            if (pz == 2 && px >= 5 && px <= 10) set(x, BASE, z, Blocks.OAK_FENCE.getDefaultState());   // stalls
        }
        BlockState stair = Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, pz < 7 ? Direction.SOUTH : Direction.NORTH);
        set(x, roofY, z, e >= 7 ? Blocks.DARK_OAK_PLANKS.getDefaultState() : stair);
        if (e <= 3) set(x, roofY - 1, z, Blocks.DARK_OAK_PLANKS.getDefaultState());
    }

    private void silo(int x, int z, double r, int lx, int lz) {
        set(x, GROUND, z, Blocks.SMOOTH_STONE.getDefaultState());
        int h = 16;
        if (r >= 3) for (int y = BASE; y < BASE + h; y++) set(x, y, z, (y - BASE) % 4 == 3 ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
        else for (int y = BASE; y < BASE + 3; y++) set(x, y, z, Blocks.HAY_BLOCK.getDefaultState());
        double dome = Math.sqrt(Math.max(0, 16 - r * r));
        for (int y = BASE + h; y <= BASE + h + (int) dome; y++) set(x, y, z, Blocks.OXIDIZED_COPPER.getDefaultState());
        if (lx == 41 && lz == 8) for (int y = BASE; y < BASE + h; y++) set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST));
    }

    /** a seed for tree() that picks the species and varies the trunk height */
    private static int treeSeed(int species, int h) {
        return ((species + 5 * Math.floorMod(h, 40)) << 8) | (h & 0xff);
    }

    /** woodland: dense mixed trees, undergrowth, a winding trail and sometimes a hunter's cabin */
    private void forest(int x, int z, int bx, int bz, int lx, int lz) {
        double trail = 25 + 9 * Math.sin(lx / 7.0 + bz);
        boolean onTrail = Math.abs(lz - trail) < 1.2;
        boolean cabin = hash(bx, bz, 211) % 2 == 0;
        int cx = lx - 37, cz = lz - 9;
        if (cabin && Math.abs(cx) <= 4 && Math.abs(cz) <= 4) {
            cabin(x, z, cx, cz);
            return;
        }
        float g = rnd(x, z, 212);
        set(x, GROUND, z, onTrail ? Blocks.DIRT_PATH.getDefaultState() : g < 0.15f ? Blocks.PODZOL.getDefaultState() : g < 0.22f ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
        if (!onTrail) {
            float u = rnd(x, z, 213);
            if (u < 0.14f) set(x, BASE, z, Blocks.FERN.getDefaultState());
            else if (u < 0.3f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
            else if (u < 0.32f) set(x, BASE, z, Blocks.SWEET_BERRY_BUSH.getDefaultState().with(net.minecraft.block.SweetBerryBushBlock.AGE, 3));
            else if (u < 0.335f) set(x, BASE, z, u < 0.3275f ? Blocks.BROWN_MUSHROOM.getDefaultState() : Blocks.RED_MUSHROOM.getDefaultState());
            else if (u < 0.34f) set(x, BASE, z, Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }
        // trees on a 5-block grid
        int ox = bx * CELL + C, oz = bz * CELL + C;
        for (int gx = Math.floorDiv(lx - 4, 5); gx <= Math.floorDiv(lx + 4, 5); gx++) {
            for (int gz = Math.floorDiv(lz - 4, 5); gz <= Math.floorDiv(lz + 4, 5); gz++) {
                int h = hash(ox + gx, oz + gz, 214);
                int tlx = gx * 5 + 1 + h % 3, tlz = gz * 5 + 1 + (h >>> 3) % 3;
                if (tlx < 1 || tlz < 1 || tlx > 50 || tlz > 50) continue;
                if (Math.abs(tlz - (25 + 9 * Math.sin(tlx / 7.0 + bz))) < 2.5) continue;
                if (cabin && Math.abs(tlx - 37) <= 6 && Math.abs(tlz - 9) <= 6) continue;
                if (rnd(ox + gx, oz + gz, 215) > 0.8f) continue;
                int species = new int[]{2, 2, 1, 0, 2, 1}[(h >>> 6) % 6];
                tree(x, z, ox + tlx, oz + tlz, treeSeed(species, h));
            }
        }
    }

    private void cabin(int x, int z, int cx, int cz) {
        int ax = Math.abs(cx), az = Math.abs(cz);
        set(x, GROUND, z, Blocks.SPRUCE_PLANKS.getDefaultState());
        if (ax == 4 || az == 4) {
            if (az == 4 && cz > 0 && ax <= 4 && cx == 0) {                   // door
                set(x, BASE, z, Blocks.SPRUCE_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH).with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                set(x, BASE + 1, z, Blocks.SPRUCE_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH).with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                set(x, BASE + 2, z, Blocks.SPRUCE_LOG.getDefaultState());
            } else {
                for (int y = BASE; y <= BASE + 2; y++) {
                    BlockState log = Blocks.SPRUCE_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, ax == 4 && az == 4 ? Direction.Axis.Y : ax == 4 ? Direction.Axis.Z : Direction.Axis.X);
                    set(x, y, z, y == BASE + 1 && (ax == 0 || az == 0) && !(az == 4 && cz > 0) ? Blocks.GLASS_PANE.getDefaultState() : log);
                }
            }
        } else {
            if (cx == -3 && cz == -3) container(x, BASE, z, "cabin", true);
            if (cx == 3 && cz == -3) set(x, BASE, z, Blocks.BROWN_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD));
            if (cx == 3 && cz == -2) set(x, BASE, z, Blocks.BROWN_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT));
            if (cx == -3 && cz == 2) set(x, BASE, z, Blocks.CRAFTING_TABLE.getDefaultState());
            if (cx == 0 && cz == 0) set(x, BASE + 2, z, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
        }
        int roof = BASE + 3 + Math.max(0, 4 - ax) / 2;
        set(x, roof, z, ax <= 1 ? Blocks.SPRUCE_PLANKS.getDefaultState() : Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, cx < 0 ? Direction.EAST : Direction.WEST));
        if (cx == 2 && cz == 4) {}                                           // (porch side)
        if (cx == 0 && cz == -4) for (int y = BASE + 3; y <= BASE + 6; y++) set(x, y, z, Blocks.COBBLESTONE.getDefaultState());   // chimney
        if (cx == 0 && cz == -4) set(x, BASE + 7, z, Blocks.CAMPFIRE.getDefaultState());
    }

    /** open meadow: long grass, wildflowers, a pond with reeds, scattered trees and hay bales */
    private void meadow(int x, int z, int bx, int bz, int lx, int lz) {
        double px = (lx - 30) / 6.0, pz = (lz - 30) / 4.0, pe = px * px + pz * pz;
        if (pe < 1) {
            set(x, GROUND, z, WATER);
            set(x, GROUND - 1, z, pe < 0.5 ? WATER : Blocks.CLAY.getDefaultState());
            if (pe < 0.5) set(x, GROUND - 2, z, Blocks.CLAY.getDefaultState());
            if (rnd(x, z, 216) < 0.07f) set(x, BASE, z, Blocks.LILY_PAD.getDefaultState());
            return;
        }
        if (pe < 1.3 && rnd(x, z, 217) < 0.35f) {
            for (int y = BASE; y < BASE + 2; y++) set(x, y, z, Blocks.SUGAR_CANE.getDefaultState());
            return;
        }
        if (lx == 22 && lz == 26) { set(x, BASE, z, CityBlocks.facing(CityBlocks.PICNIC_TABLE, Direction.NORTH)); return; }
        float f = rnd(x, z, 218);
        if (f < 0.22f) {
            set(x, BASE, z, Blocks.TALL_GRASS.getDefaultState().with(net.minecraft.block.TallPlantBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER));
            set(x, BASE + 1, z, Blocks.TALL_GRASS.getDefaultState().with(net.minecraft.block.TallPlantBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        } else if (f < 0.45f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
        else if (f < 0.56f) set(x, BASE, z, FLOWERS[hash(x, z, 219) % FLOWERS.length]);
        else if (f < 0.565f) set(x, BASE, z, Blocks.HAY_BLOCK.getDefaultState());
        int ox = bx * CELL + C, oz = bz * CELL + C;
        for (int gx = Math.floorDiv(lx - 4, 13); gx <= Math.floorDiv(lx + 4, 13); gx++) {
            for (int gz = Math.floorDiv(lz - 4, 13); gz <= Math.floorDiv(lz + 4, 13); gz++) {
                int h = hash(ox + gx, oz + gz, 220);
                int tlx = gx * 13 + 3 + h % 7, tlz = gz * 13 + 3 + (h >>> 3) % 7;
                if (tlx < 4 || tlz < 4 || tlx > 47 || tlz > 47 || rnd(ox + gx, oz + gz, 221) > 0.45f) continue;
                double qx = (tlx - 30) / 9.0, qz = (tlz - 30) / 7.0;
                if (qx * qx + qz * qz < 1) continue;
                tree(x, z, ox + tlx, oz + tlz, treeSeed(new int[]{0, 1, 4}[h % 3], h));
            }
        }
    }

    /** orchard: rows of blossoming fruit trees with mown paths, and a farm stand by the road */
    private void orchard(int x, int z, int bx, int bz, int lx, int lz) {
        if (lz <= 4 && lx >= 2 && lx <= 9) {                                 // farm stand
            set(x, GROUND, z, Blocks.SPRUCE_PLANKS.getDefaultState());
            if (lz == 2 && lx >= 3 && lx <= 8) {
                set(x, BASE, z, lx % 2 == 0 ? Blocks.BARREL.getDefaultState() : Blocks.SPRUCE_SLAB.getDefaultState()
                        .with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.TOP));
                if (lx % 2 == 0) lootAt(x, BASE, z, "farm");
            }
            if ((lx == 2 || lx == 9) && (lz == 1 || lz == 4)) for (int y = BASE; y < BASE + 3; y++) set(x, y, z, Blocks.SPRUCE_FENCE.getDefaultState());
            set(x, BASE + 3, z, (lx + lz) % 2 == 0 ? Blocks.RED_WOOL.getDefaultState() : Blocks.WHITE_WOOL.getDefaultState());
            return;
        }
        if (lx % 6 == 0) {
            set(x, GROUND, z, Blocks.DIRT_PATH.getDefaultState());
            return;
        }
        if (rnd(x, z, 222) < 0.15f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
        // fruit trees on a 6x6 grid
        for (int gx = Math.floorDiv(lx - 3, 6); gx <= Math.floorDiv(lx + 3, 6); gx++) {
            for (int gz = Math.floorDiv(lz - 3, 6); gz <= Math.floorDiv(lz + 3, 6); gz++) {
                int tlx = gx * 6 + 3, tlz = gz * 6 + 3;
                if (tlx < 3 || tlx > 48 || tlz < 9 || tlz > 48) continue;
                int dx = lx - tlx, dz = lz - tlz;
                double d = Math.sqrt(dx * dx + dz * dz);
                boolean blossom = hash(bx * 31 + gx, bz * 31 + gz, 223) % 3 == 0;
                if (dx == 0 && dz == 0) for (int y = BASE; y < BASE + 3; y++) set(x, y, z, Blocks.OAK_LOG.getDefaultState());
                for (int y = BASE + 2; y <= BASE + 5; y++) {
                    double ry = (y - (BASE + 3.5)) * 1.2;
                    if (Math.sqrt(d * d + ry * ry) <= 2.5 && isAir(x, y, z))
                        set(x, y, z, (blossom ? Blocks.CHERRY_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES).getDefaultState().with(LeavesBlock.PERSISTENT, true));
                }
            }
        }
    }

    /** cows and sheep, tractors and pickups, trains at the stations */
    private void countryEntities(int bx, int bz, int x0, int z0) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        Kind k = kind(bx, bz);
        if (k == Kind.FARM) {
            parkAt("tractor", 33, 19, BASE, 0f, 0.8, bx, bz, 1, x0, z0);
            parkAt("pickup_truck", 12, 2.5, BASE, 90f, 0.6, bx, bz, 2, x0, z0);
            if (hash(bx, bz, 204) % 3 == 0) {                                // pasture animals
                for (int i = 0; i < 10; i++) {
                    double ax = ox + 4 + rnd(bx * 13 + i, bz, 224) * 44, az = oz + 22 + rnd(bx, bz * 13 + i, 225) * 26;
                    if (!inChunk(ax, az, x0, z0)) continue;
                    var type = i % 3 == 0 ? EntityType.SHEEP : EntityType.COW;
                    var a = type.create(w.toServerWorld());
                    if (a == null) continue;
                    a.refreshPositionAndAngles(ax, BASE, az, rnd(bx, i, 226) * 360f, 0);
                    a.setPersistent();
                    w.spawnEntity(a);
                }
            }
        } else if (k == Kind.STATION) {
            boolean east = local(bx) > 0;
            double s = 30, tx = east ? ox + s + 0.5 : ox + 51 - s + 0.5, tz = oz + RAIL_LZ + 0.5;
            String id = new String[]{"diesel_locomotive", "steam_train", "bullet_train"}[hash(bx, bz, 227) % 3];
            if (inChunk(tx, tz, x0, z0)) spawnType(tx, RAIL_Y, tz, east ? -90f : 90f, id, hash(bx, bz, 228), false);
            for (int i = 0; i < 3; i++) {
                double cx = east ? ox + 45 + i * 2.5 : ox + 6 - i * 2.5, cz = oz + 3.5;
                if (inChunk(cx, cz, x0, z0) && rnd(bx, bz * 5 + i, 229) < 0.7f) spawnType(cx, BASE, cz, 0f, "taxi", hash(bx * 5 + i, bz, 230));
            }
        }
    }

    // ============================================================ airport

    private static final BlockState MARKER = CityBlocks.AIRPORT_MARKER.getDefaultState();

    /**
     * The airport, in its own coordinates (ax 0..294 west to east, az 0..132 north to south): a glass terminal with
     * check-in, security, baggage claim, seating and shops; a control tower; a hangar (with a secret armoury
     * underneath); fuel tanks; an apron with stands and jet bridges; a taxiway; and a 290-block runway.
     */
    private void airport(int x, int z, int ax, int az) {
        loot = "shop";
        set(x, GROUND - 1, z, MARKER);
        set(x, GROUND, z, Blocks.GRASS_BLOCK.getDefaultState());
        if (ax >= 244 && ax <= 260 && az >= 5 && az <= 15) armoryColumn(x, z, ax, az);
        // perimeter fence (open along the front of the terminal and car park)
        boolean fence = ax == 0 || ax == AIR_W - 1 || az == AIR_H - 1 || (az == 0 && ax >= 155 && !(ax >= 178 && ax <= 186));
        if (fence) {
            boolean post = (ax + az) % 6 == 0;
            for (int y = BASE; y <= BASE + 2; y++) set(x, y, z, post ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.IRON_BARS.getDefaultState()
                    .with(net.minecraft.block.HorizontalConnectingBlock.EAST, az == 0 || az == AIR_H - 1).with(net.minecraft.block.HorizontalConnectingBlock.WEST, az == 0 || az == AIR_H - 1)
                    .with(net.minecraft.block.HorizontalConnectingBlock.NORTH, ax == 0 || ax == AIR_W - 1).with(net.minecraft.block.HorizontalConnectingBlock.SOUTH, ax == 0 || ax == AIR_W - 1));
            return;
        }
        if (ax >= 30 && ax <= 150 && az >= 4 && az <= 38) { terminal(x, z, ax - 30, az - 4); return; }
        if (ax >= 162 && ax <= 172 && az >= 7 && az <= 17) { tower(x, z, ax - 167, az - 12); return; }
        if (ax >= 196 && ax <= 262 && az >= 4 && az <= 40) { hangar(x, z, ax, az); return; }
        for (int[] t : new int[][]{{278, 14}, {278, 31}}) {
            double dx = ax - t[0] + 0.5, dz = az - t[1] + 0.5, r = Math.sqrt(dx * dx + dz * dz);
            if (r < 6) { fuelTank(x, z, r, dx, dz); return; }
        }
        // forecourt and drop-off lane
        if (az <= 3 && ax <= 154) {
            set(x, GROUND, z, az <= 1 ? WALK : ASPHALT);
            if (az == 1 && ax % 20 == 10) lamp(x, z, Direction.SOUTH);
            if (az == 0 && ax % 12 == 6) set(x, BASE, z, CityBlocks.facing(CityBlocks.PLANTER, Direction.NORTH));
            if (az == 1 && ax == 24) set(x, BASE, z, CityBlocks.facing(CityBlocks.BUS_STOP_SIGN, Direction.NORTH));
            return;
        }
        // car park
        if (ax >= 2 && ax <= 27 && az >= 4 && az <= 38) {
            boolean line = (az == 11 || az == 18 || az == 25 || az == 32) ? false : ax % 3 == 2 && ((az >= 5 && az <= 9) || (az >= 13 && az <= 17) || (az >= 20 && az <= 24) || (az >= 27 && az <= 31));
            set(x, GROUND, z, line ? CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z) : ASPHALT);
            if (ax == 27 && az % 10 == 5) lamp(x, z, Direction.WEST);
            return;
        }
        // jet bridges from the terminal out to the stands
        for (int g : new int[]{51, 91, 131}) {
            if (Math.abs(ax - g) <= 1 && az >= 39 && az <= 47) {
                boolean side = Math.abs(ax - g) == 1;
                set(x, GROUND, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
                for (int y = BASE; y <= BASE + 2; y++) set(x, y, z, side ? CLEAR : AIR);   // covered walkway out to the plane
                set(x, BASE + 3, z, Blocks.WHITE_CONCRETE.getDefaultState());
                return;
            }
        }
        // apron
        if (az >= 39 && az <= 76 && ax >= 20 && ax <= 285) {
            BlockState sfc = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
            for (int g : new int[]{51, 91, 131, 171, 211}) {
                if (ax == g && az >= 48 && az <= 72) sfc = CityBlocks.axis(CityBlocks.LINE_DOUBLE_YELLOW, Direction.Axis.Z);   // lead-in lines
                if (Math.abs(ax - g) == 12 && az >= 48 && az <= 72) sfc = CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z);
            }
            if (az == 76) sfc = CityBlocks.axis(CityBlocks.LINE_DOUBLE_YELLOW, Direction.Axis.X);
            set(x, GROUND, z, sfc);
            if (az == 44 && ax % 40 == 0) set(x, BASE, z, CityBlocks.facing(CityBlocks.SUITCASE, Direction.SOUTH));
            return;
        }
        // taxiway, with links to the runway at both ends
        boolean link = (ax >= 12 && ax <= 18) || (ax >= 276 && ax <= 282);
        if ((az >= 79 && az <= 85 && ax >= 10 && ax <= 285) || (link && az >= 77 && az <= 93)) {
            boolean centre = (az == 82 && !link) || (link && (ax == 15 || ax == 279) && az != 82);
            set(x, GROUND, z, centre ? CityBlocks.axis(CityBlocks.LINE_DOUBLE_YELLOW, az == 82 ? Direction.Axis.X : Direction.Axis.Z) : ASPHALT);
            if ((az == 78 || az == 86) && ax % 10 == 0 && !link) set(x, BASE, z, Blocks.END_ROD.getDefaultState());
            return;
        }
        // runway
        if (az >= 94 && az <= 120 && ax >= 4 && ax <= 290) {
            BlockState sfc = ASPHALT;
            boolean threshold = (ax >= 8 && ax <= 18) || (ax >= 276 && ax <= 286);
            if (az == 95 || az == 119) sfc = CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.X);
            else if (threshold && az >= 98 && az <= 116 && (az - 98) % 3 < 2) sfc = CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.X);
            else if (az == 107 && !threshold && Math.floorMod(ax, 14) < 7) sfc = CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.X);
            else if ((ax == 30 || ax == 264) && az >= 100 && az <= 114 && az != 107) sfc = CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.X);   // touchdown zone
            set(x, GROUND, z, sfc);
            return;
        }
        if ((az == 93 || az == 121) && ax >= 4 && ax <= 290 && ax % 10 == 4) {
            set(x, BASE, z, Blocks.END_ROD.getDefaultState());               // runway edge lights
            return;
        }
        if (az >= 100 && az <= 114 && az % 3 == 1 && (ax == 1 || ax == 2 || ax == AIR_W - 2 || ax == AIR_W - 3)) {
            set(x, BASE, z, Blocks.SEA_LANTERN.getDefaultState());            // approach lights
            return;
        }
        if (ax == 150 && az == 89) {                                         // windsock
            for (int y = BASE; y < BASE + 5; y++) set(x, y, z, Blocks.IRON_BARS.getDefaultState());
            set(x, BASE + 5, z, Blocks.ORANGE_WOOL.getDefaultState());
            return;
        }
        if (rnd(x, z, 240) < 0.1f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
    }

    private void terminal(int x, int z, int tx, int tz) {
        int W = 120, D = 34, top = BASE + 11;
        set(x, GROUND, z, (tx / 3 + tz / 3) % 2 == 0 ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.POLISHED_DIORITE.getDefaultState());
        boolean wallX = tx == 0 || tx == W, wallZ = tz == 0 || tz == D;
        if (wallX || wallZ) {
            for (int y = BASE; y < top; y++) {
                BlockState b = wallZ ? (tx % 6 == 0 ? Blocks.WHITE_CONCRETE.getDefaultState() : REFLECT)
                        : (y >= BASE + 3 && y <= BASE + 8 && tz % 4 != 0 ? REFLECT : Blocks.WHITE_CONCRETE.getDefaultState());
                if (tz == 0 && y <= BASE + 3 && (tx % 40 >= 18 && tx % 40 <= 21)) b = AIR;            // entrances
                if (tz == D && y <= BASE + 3 && (tx == 20 || tx == 21 || tx == 60 || tx == 61 || tx == 100 || tx == 101)) b = AIR;   // to the jet bridges
                set(x, y, z, b);
            }
            set(x, top, z, Blocks.WHITE_CONCRETE.getDefaultState());
            set(x, top + 1, z, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
            return;
        }
        set(x, top, z, tz % 8 == 4 ? CLEAR : (tx % 5 == 2 && tz % 5 == 2) ? CEILING_LIGHT : Blocks.WHITE_CONCRETE.getDefaultState());
        if (tx % 20 == 10 && tz % 12 == 6) {                                 // columns
            for (int y = BASE; y < top; y++) set(x, y, z, Blocks.WHITE_CONCRETE.getDefaultState());
            return;
        }
        if ((tx % 20 == 9 || tx % 20 == 11) && tz % 12 == 6) { set(x, BASE, z, f(CityBlocks.POTTED_PLANT, Direction.NORTH)); return; }
        // departure boards over the hall
        if (tz == 1 && tx >= 50 && tx <= 70) {
            set(x, BASE + 5, z, CityBlocks.facing(CityBlocks.TV, Direction.SOUTH).with(net.minecraft.state.property.Properties.LIT, !com.nothatcher.vehicleforge.power.PowerGrid.MAINS_DOWN));
        }
        // check-in counters (west half)
        if (tz == 9 && tx >= 4 && tx <= 56 && tx % 4 != 0) { set(x, BASE, z, f(CityBlocks.CHECKOUT_COUNTER, Direction.NORTH)); lootAt(x, BASE, z, "shop"); return; }
        if (tz == 11 && tx >= 4 && tx <= 56 && tx % 4 == 2) { set(x, BASE, z, f(CityBlocks.OFFICE_CHAIR, Direction.NORTH)); return; }
        // security arches
        if (tz == 15 && tx >= 4 && tx <= 56) {
            int m = tx % 5;
            if (m == 1 || m == 4) for (int y = BASE; y <= BASE + 2; y++) set(x, y, z, Blocks.IRON_BARS.getDefaultState());
            else if (m == 0) set(x, BASE, z, f(CityBlocks.DESK, Direction.SOUTH));
            else set(x, BASE + 2, z, Blocks.IRON_BARS.getDefaultState());
            return;
        }
        // baggage claim carousels (east half)
        for (int cx : new int[]{78, 102}) {
            int dx = Math.abs(tx - cx), dz = Math.abs(tz - 9);
            if (dx <= 7 && dz <= 3 && (dx == 7 || dz == 3 || (dz == 0 && dx <= 5))) {
                set(x, BASE, z, Blocks.BLACK_CONCRETE.getDefaultState());
                if (rnd(x, z, 241) < 0.35f) {
                    set(x, BASE + 1, z, f(CityBlocks.SUITCASE, Direction.fromHorizontal(hash(x, z, 242) % 4)));
                    lootAt(x, BASE + 1, z, "luggage");
                }
                return;
            }
        }
        // gate seating
        if (tz >= 21 && tz <= 30 && tz % 3 == 0 && tx >= 4 && tx <= 116 && tx % 7 != 0) { set(x, BASE, z, f(CityBlocks.SOFA, Direction.SOUTH)); return; }
        // shops and cafes along the back
        if (tz == 33 && tx >= 64 && tx <= 116) {
            if (tx % 8 < 5) { set(x, BASE, z, f(CityBlocks.STORE_SHELF, Direction.NORTH)); lootAt(x, BASE, z, "shop"); }
            else if (tx % 8 == 6) set(x, BASE, z, f(CityBlocks.VENDING_MACHINE, Direction.NORTH));
            return;
        }
        if (tz == 31 && tx >= 64 && tx <= 116 && tx % 8 == 2) { set(x, BASE, z, f(CityBlocks.CHECKOUT_COUNTER, Direction.SOUTH)); lootAt(x, BASE, z, "shop"); return; }
        if (tz == 2 && (tx == 3 || tx == 117)) set(x, BASE, z, f(CityBlocks.ATM, Direction.SOUTH));
    }

    /** control tower: a shaft with a ladder up to a glass cab, (ox, oz) from the centre */
    private void tower(int x, int z, int ox, int oz) {
        int cabFloor = BASE + 30;
        int a = Math.max(Math.abs(ox), Math.abs(oz));
        set(x, GROUND, z, Blocks.POLISHED_ANDESITE.getDefaultState());
        if (a <= 3) {
            for (int y = BASE; y < cabFloor; y++) {
                if (a == 3) {
                    BlockState b = Blocks.WHITE_CONCRETE.getDefaultState();
                    if ((ox == 0 || oz == 0) && (y - BASE) % 6 == 3) b = REFLECT;
                    if (oz == -3 && ox == 0 && y <= BASE + 2) b = y == BASE + 2 ? Blocks.WHITE_CONCRETE.getDefaultState() : AIR;
                    set(x, y, z, b);
                } else if (ox == -2 && oz == 0) {
                    set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST));
                }
            }
        }
        if (a <= 5) {
            set(x, cabFloor, z, ox == -2 && oz == 0 ? Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST) : Blocks.SMOOTH_STONE.getDefaultState());
            for (int y = cabFloor + 1; y <= cabFloor + 3; y++) set(x, y, z, a == 5 ? REFLECT : AIR);
            set(x, cabFloor + 4, z, a == 5 ? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
            if (a == 4 && (ox + oz) % 2 == 0 && oz != 0) {
                set(x, cabFloor + 1, z, f(CityBlocks.DESK, Direction.fromHorizontal(Math.abs(ox) > Math.abs(oz) ? (ox > 0 ? 3 : 1) : (oz > 0 ? 0 : 2)).getOpposite()));
                set(x, cabFloor + 2, z, f(CityBlocks.COMPUTER, Direction.fromHorizontal(Math.abs(ox) > Math.abs(oz) ? (ox > 0 ? 3 : 1) : (oz > 0 ? 0 : 2)).getOpposite())
                        .with(net.minecraft.state.property.Properties.LIT, !com.nothatcher.vehicleforge.power.PowerGrid.MAINS_DOWN));
            }
        }
        if (ox == 0 && oz == 0) {
            for (int y = cabFloor + 5; y <= cabFloor + 9; y++) set(x, y, z, Blocks.IRON_BARS.getDefaultState());
            set(x, cabFloor + 10, z, Blocks.REDSTONE_BLOCK.getDefaultState());
            set(x, cabFloor + 11, z, Blocks.SEA_LANTERN.getDefaultState());
        }
    }

    /** arched hangar, open to the south */
    private void hangar(int x, int z, int ax, int az) {
        double dx = ax - 229, r = 33;
        int h = (int) Math.round(Math.sqrt(Math.max(0, r * r - dx * dx)) * 0.62);
        set(x, GROUND, z, ax == 258 && az == 8 ? CityBlocks.SECRET_STONE.getDefaultState() : Blocks.SMOOTH_STONE.getDefaultState());
        int roof = BASE + h;
        if (h <= 1) {                                                        // the arch meets the ground: side walls
            for (int y = BASE; y <= BASE + 1; y++) set(x, y, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
            return;
        }
        set(x, roof, z, az % 4 == 0 ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
        if (az == 4) {                                                       // back wall
            for (int y = BASE; y < roof; y++) set(x, y, z, (y - BASE) % 5 == 3 && Math.abs(dx) < 20 ? CLEAR : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
            return;
        }
        if (az == 40 && h > 2) set(x, roof - 1, z, Blocks.YELLOW_CONCRETE.getDefaultState());   // door header
        if (az % 9 == 4 && ax % 12 == 1) set(x, roof - 1, z, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
        if (az == 6 && ax % 6 == 0 && ax < 240) { container(x, BASE, z, "garage", true); return; }
        if (az == 6 && ax == 243) { set(x, BASE, z, f(CityBlocks.GENERATOR, Direction.SOUTH)); return; }
        if (az == 7 && (ax == 202 || ax == 203)) set(x, BASE, z, f(CityBlocks.DESK, Direction.SOUTH));
    }

    private void fuelTank(int x, int z, double r, double dx, double dz) {
        set(x, GROUND, z, Blocks.SMOOTH_STONE.getDefaultState());
        if (r < 5) {
            for (int y = BASE; y <= BASE + 8; y++) set(x, y, z, r >= 4 || y == BASE + 8 ? Blocks.SMOOTH_QUARTZ.getDefaultState() : AIR);
            if (r < 3) set(x, BASE + 9, z, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
            if (r >= 4 && Math.abs(dz) < 0.6 && dx < 0) for (int y = BASE; y <= BASE + 8; y++) set(x, y, z, Blocks.QUARTZ_PILLAR.getDefaultState());
        } else if (Math.abs(dz) < 0.6 && dx < 0) {
            for (int y = BASE; y <= BASE + 8; y++) set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST));
        } else if (r >= 5.6) {
            set(x, BASE, z, Blocks.IRON_BARS.getDefaultState());
        }
    }

    /** under the hangar: a sealed military armoury, reached through a stone floor panel that slides away */
    private void armoryColumn(int x, int z, int ax, int az) {
        int floor = GROUND - 13, ceil = GROUND - 7;
        boolean wall = ax == 244 || ax == 260 || az == 5 || az == 15;
        for (int y = floor; y <= ceil; y++) set(x, y, z, wall || y == floor || y == ceil ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : AIR);
        if (wall) return;
        set(x, floor, z, Blocks.POLISHED_ANDESITE.getDefaultState());
        if (ax == 258 && az == 8) {                                          // the way down
            for (int y = floor + 1; y < GROUND; y++) set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
            return;
        }
        if (ax == 258 && az == 9) {
            for (int y = floor + 1; y < ceil; y++) set(x, y, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            return;
        }
        if ((ax + az) % 4 == 0) set(x, ceil, z, CEILING_LIGHT.with(net.minecraft.state.property.Properties.LIT, true));
        int y = floor + 1;
        if (az == 6 && ax % 2 == 0 && ax <= 256) container(x, y, z, "armory", true);
        else if (az == 14 && ax >= 246 && ax <= 250) { set(x, y, z, f(CityBlocks.STORE_SHELF, Direction.NORTH)); lootAt(x, y, z, "armory"); }
        else if (az == 14 && ax == 245) { set(x, y, z, f(CityBlocks.GENERATOR, Direction.EAST)); fuel(x, y, z); }
        else if ((ax == 253 || ax == 255) && az == 14) set(x, y, z, Blocks.GREEN_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.HEAD));
        else if ((ax == 253 || ax == 255) && az == 13) set(x, y, z, Blocks.GREEN_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.FOOT));
        else if (ax == 250 && az == 10) {
            set(x, y, z, f(CityBlocks.DESK, Direction.NORTH));
            set(x, y + 1, z, f(CityBlocks.COMPUTER, Direction.NORTH).with(net.minecraft.state.property.Properties.LIT, true));
        } else if (ax == 252 && az == 10) set(x, y, z, Blocks.CARTOGRAPHY_TABLE.getDefaultState());
    }

    private void fuel(int x, int y, int z) {
        pos.set(x, y, z);
        if (w.getBlockEntity(pos) instanceof com.nothatcher.vehicleforge.power.PowerBlockEntity pbe) pbe.setFuel(36000);
    }

    /** planes on the stands, a plane and a helicopter in the hangar, cars in the car park, and (with Outbreak) the dead */
    private void airportEntities(int x0, int z0) {
        int[] o = airportOrigin(x0, z0);
        if (o == null || x0 + 15 < o[0] || x0 >= o[0] + AIR_W || z0 + 15 < o[1] || z0 >= o[1] + AIR_H) return;
        double X = o[0], Z = o[1];
        Object[][] craft = {
                {"airliner", 51.5, 60.0, 180f}, {"airliner", 91.5, 60.0, 180f}, {"airliner", 131.5, 60.0, 180f},
                {"jet", 171.5, 60.0, 180f}, {"jet", 211.5, 60.0, 180f}, {"prop_plane", 215.0, 22.0, 0f}, {"helicopter", 245.0, 26.0, 0f},
                {"airliner", 90.0, 82.5, -90f}, {"prop_plane", 30.0, 107.5, -90f}};
        for (int i = 0; i < craft.length; i++) {
            double cx = X + (double) craft[i][1], cz = Z + (double) craft[i][2];
            if (!inChunk(cx, cz, x0, z0)) continue;
            int h = hash(i, 777, 243);
            boolean wreck = OVERRUN && i != 5 && i != 6 && (h >>> 9) % 3 == 0;
            spawnType(cx, BASE, cz, (float) craft[i][3], (String) craft[i][0], h, wreck);
        }
        if (OVERRUN) {
            double cx = X + 150.5, cz = Z + 108.5;                           // the one that didn't make it
            if (inChunk(cx, cz, x0, z0)) spawnType(cx, BASE, cz, 35f, "airliner", 4242, true);
        }
        for (int row : new int[]{7, 15, 22, 29}) {
            for (int s = 0; s < 8; s++) {
                double cx = X + 3.5 + s * 3, cz = Z + row + 0.5;
                if (inChunk(cx, cz, x0, z0) && rnd(s, row, 244) < 0.55f) spawn(cx, cz, row % 2 == 0 ? 0f : 180f, hash(s, row, 245));
            }
        }
        if (!OVERRUN) return;
        // the airport was where everyone tried to get out: a dense, finite crowd of the dead
        for (int i = 0; i < 4; i++) {                                        // ~200 across the whole airport
            int lx = hash(x0, z0 * 7 + i, 246) & 15, lz = hash(x0 * 7 + i, z0, 247) & 15;
            int x = x0 + lx, z = z0 + lz, ax = x - o[0], az = z - o[1];
            if (!inAirport(x, z)) continue;
            float density = ax >= 30 && ax <= 150 && az >= 4 && az <= 38 ? 0.9f          // terminal
                    : az >= 39 && az <= 76 ? 0.5f                                         // apron
                    : ax >= 196 && ax <= 262 && az <= 40 ? 0.5f                            // hangar
                    : ax <= 27 && az <= 38 ? 0.45f                                         // car park
                    : az <= 3 ? 0.6f                                                      // forecourt
                    : 0.15f;
            if (rnd(x, z, 248) > density) continue;
            if (!isAir(x, BASE, z) || !isAir(x, BASE + 1, z)) continue;
            var zed = EntityType.ZOMBIE.create(w.toServerWorld());
            if (zed == null) continue;
            zed.refreshPositionAndAngles(x + 0.5, BASE, z + 0.5, rnd(x, z, 249) * 360f, 0);
            pos.set(x, BASE, z);
            zed.initialize(w, w.getLocalDifficulty(pos), net.minecraft.entity.SpawnReason.STRUCTURE, null, null);
            zed.setPersistent();
            zed.addCommandTag("outbreak_horde");
            w.spawnEntity(zed);
        }
    }

    // ============================================================ secrets

    /** one garaged house in seven hides a doomsday bunker */
    private boolean bunker(Lot l) {
        return hasGarage(l) && hash(l.ox, l.oz, 190) % 7 == 0;
    }

    /** where the bunker hatch is: the back corner of the garage, under a rug */
    private int[] hatch(Lot l) {
        boolean frontLow = l.face == Direction.NORTH || l.face == Direction.WEST;
        int a1 = l.alongX ? l.hx1 : l.hz1, c0 = l.alongX ? l.hz0 : l.hx0, c1 = l.alongX ? l.hz1 : l.hx1;
        int a = a1 - 3, c = frontLow ? c1 - 1 : c0 + 1;
        return l.alongX ? new int[]{a, c} : new int[]{c, a};
    }

    /**
     * The bunker: an 11x11 concrete room 13 blocks down with bunks, a radio, a fuelled generator, water, shelves of
     * supplies... and a bookshelf wall, one shelf of which slides aside into a vault.
     */
    private void bunkerColumn(int x, int z, Lot l) {
        int px = x - l.ox, pz0 = z - l.oz;
        if (px < 3 || px > 13 || pz0 < 3 || pz0 > 13) return;
        int floor = GROUND - 13, ceil = GROUND - 7;
        int[] h = hatch(l);
        // lay the room out so the hatch always comes down into the main room, never the vault
        boolean flip = h[1] - l.oz < 8;
        int pz = flip ? 16 - pz0 : pz0;
        Direction n = flip ? Direction.SOUTH : Direction.NORTH;
        boolean wall = px == 3 || px == 13 || pz0 == 3 || pz0 == 13;
        for (int y = floor; y <= ceil; y++) set(x, y, z, wall || y == floor || y == ceil ? Blocks.POLISHED_DEEPSLATE.getDefaultState() : AIR);
        if (wall) return;
        set(x, floor, z, Blocks.POLISHED_ANDESITE.getDefaultState());
        if (x == h[0] && z == h[1]) {
            for (int y = floor + 1; y < GROUND; y++) set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
            return;
        }
        if (x == h[0] && z == h[1] + 1) {
            for (int y = floor + 1; y < ceil; y++) set(x, y, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            return;
        }
        if ((px + pz) % 4 == 0) set(x, ceil, z, CEILING_LIGHT.with(net.minecraft.state.property.Properties.LIT, true));
        int y = floor + 1;
        if (pz == 7) {                                                       // the bookshelf wall, with one secret shelf
            for (int k = y; k < ceil; k++) {
                BlockState b = k <= y + 2 ? Blocks.BOOKSHELF.getDefaultState() : Blocks.POLISHED_DEEPSLATE.getDefaultState();
                if (px == 8 && k <= y + 1) b = CityBlocks.SECRET_BOOKSHELF.getDefaultState();
                set(x, k, z, b);
            }
            return;
        }
        if (pz <= 6) {                                                       // the vault
            if (pz == 4 && px % 2 == 1 && px >= 5 && px <= 11) container(x, y, z, "bunker_vault", true);
            if (pz == 4 && px == 12) set(x, y, z, Blocks.GOLD_BLOCK.getDefaultState());
            if (pz == 4 && px == 4) set(x, y, z, Blocks.TNT.getDefaultState());
            return;
        }
        if ((px == 4 || px == 5) && pz == 8) set(x, y, z, Blocks.GRAY_BED.getDefaultState().with(BedBlock.FACING, n).with(BedBlock.PART, BedPart.HEAD));
        else if ((px == 4 || px == 5) && pz == 9) set(x, y, z, Blocks.GRAY_BED.getDefaultState().with(BedBlock.FACING, n).with(BedBlock.PART, BedPart.FOOT));
        else if (px == 12 && pz >= 8 && pz <= 10) { set(x, y, z, f(CityBlocks.STORE_SHELF, Direction.WEST)); set(x, y + 1, z, f(CityBlocks.STORE_SHELF, Direction.WEST)); lootAt(x, y, z, "bunker"); }
        else if (px == 12 && pz == 12) { set(x, y, z, f(CityBlocks.GENERATOR, Direction.WEST)); fuel(x, y, z); }
        else if (px == 7 && pz == 12) { set(x, y, z, f(CityBlocks.DESK, n)); set(x, y + 1, z, f(CityBlocks.COMPUTER, n).with(net.minecraft.state.property.Properties.LIT, true)); }
        else if (px == 7 && pz == 11) set(x, y, z, f(CityBlocks.OFFICE_CHAIR, n.getOpposite()));
        else if (px == 4 && pz == 12) set(x, y, z, Blocks.WATER_CAULDRON.getDefaultState().with(net.minecraft.block.LeveledCauldronBlock.LEVEL, 3));
        else if (px == 5 && pz == 12) set(x, y, z, Blocks.CRAFTING_TABLE.getDefaultState());
        else if (px == 10 && pz == 12) { set(x, y, z, f(CityBlocks.FILING_CABINET, n)); lootAt(x, y, z, "bunker"); }
        else if (px == 9 && pz == 12) set(x, y, z, Blocks.CARTOGRAPHY_TABLE.getDefaultState());
    }

    /**
     * Beneath the church: a vaulted crypt of stone coffins, candles, skulls and cobwebs with a reliquary at the far
     * end. The way down is the quartz floor tile just behind the altar.
     */
    private void cryptColumn(int x, int z, int lx, int lz) {
        if (lx < 18 || lx > 33 || lz < 21 || lz > 44) return;
        int floor = GROUND - 8, ceil = GROUND - 2;
        boolean wall = lx == 18 || lx == 33 || lz == 21 || lz == 44;
        for (int y = floor; y <= ceil; y++) set(x, y, z, wall || y == floor || y == ceil ? churchStone(x, y, z) : AIR);
        if (wall) return;
        int y = floor + 1;
        if (lx == 25 && lz == 43) {
            for (int k = y; k <= GROUND; k++) set(x, k, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
            return;
        }
        if ((lx == 19 || lx == 32 || lz == 22) && (lx + lz) % 3 == 0) set(x, ceil - 1, z, Blocks.COBWEB.getDefaultState());
        boolean coffinX = lx == 21 || lx == 22 || lx == 29 || lx == 30;
        boolean coffinZ = lz == 25 || lz == 26 || lz == 30 || lz == 31 || lz == 35 || lz == 36;
        if (coffinX && coffinZ) {
            set(x, y, z, Blocks.SMOOTH_STONE.getDefaultState());
            set(x, y + 1, z, Blocks.STONE_BRICK_SLAB.getDefaultState());
            return;
        }
        if ((lx == 23 || lx == 28) && (lz == 27 || lz == 32 || lz == 37)) {
            set(x, y, z, Blocks.CANDLE.getDefaultState().with(net.minecraft.block.CandleBlock.CANDLES, 2).with(net.minecraft.block.CandleBlock.LIT, true));
            return;
        }
        if (lx == 25 && (lz == 26 || lz == 33 || lz == 40))
            set(x, ceil - 1, z, Blocks.SOUL_LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
        if (lz == 22) {
            if (lx == 25 || lx == 26) container(x, y, z, "crypt", true);
            else if (lx == 24 || lx == 27) {
                set(x, y, z, Blocks.GOLD_BLOCK.getDefaultState());
                set(x, y + 1, z, Blocks.CANDLE.getDefaultState().with(net.minecraft.block.CandleBlock.CANDLES, 4).with(net.minecraft.block.CandleBlock.LIT, true));
            } else if (lx % 3 == 1) set(x, y, z, Blocks.SKELETON_SKULL.getDefaultState().with(net.minecraft.block.SkullBlock.ROTATION, 8));
            return;
        }
        if ((lx == 19 || lx == 32) && lz % 4 == 0) set(x, y, z, Blocks.BONE_BLOCK.getDefaultState());
        else if ((lx == 19 || lx == 32) && lz % 4 == 2) set(x, y, z, CityBlocks.facing(CityBlocks.GRAVESTONE, lx == 19 ? Direction.EAST : Direction.WEST));
    }

    // ------------------------------------------------------- interiors

    private static BlockState f(Block b, Direction d) {
        return CityBlocks.facing(b, d);
    }

    private static final Block[] BEDS = {Blocks.RED_BED, Blocks.BLUE_BED, Blocks.GRAY_BED, Blocks.GREEN_BED, Blocks.BROWN_BED, Blocks.LIGHT_BLUE_BED, Blocks.PURPLE_BED};
    private static final Block[] CARPETS = {Blocks.RED_CARPET, Blocks.GRAY_CARPET, Blocks.BROWN_CARPET, Blocks.LIGHT_GRAY_CARPET, Blocks.BLUE_CARPET, Blocks.GREEN_CARPET};
    private static final BlockState PARTITION = Blocks.WHITE_CONCRETE.getDefaultState();

    /**
     * What stands at (x, y, z) inside a building: AIR for "deliberately empty", null for "nothing here, cupboards
     * allowed". Each floor is laid out from the building's purpose: office desks and cubicles, lobbies, apartments
     * split into bedrooms/living rooms/kitchens, shop aisles and tills, hospital wards, police offices with cells,
     * classrooms. A walkway around the ladder shaft and from the front door is always kept clear.
     */
    private BlockState furnish(int x, int y, int z, int rel, int bx0, int bx1, int bz0, int bz1, int mx, int mz, Style st, Direction front, int doorMid) {
        int r = rel % 4, floor = rel / 4;
        int ix = x - bx0, iz = z - bz0, w = bx1 - bx0, d = bz1 - bz0;
        boolean big = w >= 14 && d >= 14;
        int dx = x - mx, dz = z - mz;
        // keep the way up clear: around the ladder, and a cross of corridors through the middle of big floors
        if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return AIR;
        if (dx == 1 && Math.abs(dz) <= 2) return AIR;
        if (big && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)) return null;
        // and the way in from the front door on the ground floor
        boolean zAxisFront = front.getAxis() == Direction.Axis.Z;
        if (floor == 0 && (zAxisFront ? (x >= doorMid - 1 && x <= doorMid + 2) : (z >= doorMid - 1 && z <= doorMid + 2))) return null;
        boolean nearWall = ix == 1 || ix == w - 1 || iz == 1 || iz == d - 1;
        // the side of each cell facing into the room, for things standing against a wall
        Direction inward = ix == 1 ? Direction.EAST : ix == w - 1 ? Direction.WEST : iz == 1 ? Direction.SOUTH : Direction.NORTH;
        String kind = floor == 0 && st.shopFront() ? "shop" : loot;
        if (floor == 0 && r == 1 && ix == w - 2 && iz == d - 2 && (loot.equals("hospital") || loot.equals("police") || loot.equals("fire_station")))
            return f(CityBlocks.GENERATOR, Direction.WEST);                  // backup generator
        if (floor == 0 && kind.equals("office")) kind = "lobby";
        switch (kind) {
            case "office", "fire_station" -> {
                if (kind.equals("fire_station") && floor == 0) {
                    if (r == 1 && nearWall && iz == d - 1 && ix % 2 == 0) return f(CityBlocks.FILING_CABINET, Direction.NORTH);  // lockers
                    return null;
                }
                return office(ix, iz, w, d, r, nearWall, inward, x, z, floor);
            }
            case "police" -> {
                if (floor == 0 && iz >= d - 6) {                             // holding cells along the back
                    if (iz == d - 6 && r <= 2) return ix % 5 == 2 ? AIR : Blocks.IRON_BARS.getDefaultState();
                    if (ix % 5 == 0 && r <= 3) return Blocks.IRON_BARS.getDefaultState();
                    if (r == 1 && iz == d - 1 && ix % 5 == 3) return Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.HEAD);
                    if (r == 1 && iz == d - 2 && ix % 5 == 3) return Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.FOOT);
                    return AIR;
                }
                return office(ix, iz, w, d, r, nearWall, inward, x, z, floor);
            }
            case "lobby" -> {
                if (r != 1) return null;
                if (nearWall && (ix + iz) % 4 == 0) return f(CityBlocks.POTTED_PLANT, inward);
                if (iz == 4 && ix >= w / 2 - 4 && ix <= w / 2 + 4) return f(CityBlocks.CHECKOUT_COUNTER, Direction.NORTH);  // reception
                if ((iz == 3 || iz == d - 3) && ix % 6 == 3) return f(CityBlocks.SOFA, iz == 3 ? Direction.SOUTH : Direction.NORTH);
                if (ix == 2 && iz == d / 2) return f(CityBlocks.ATM, Direction.EAST);
                if (ix == w - 2 && iz == d / 2) return f(CityBlocks.VENDING_MACHINE, Direction.WEST);
                return null;
            }
            case "shop" -> {
                if (r == 3) return null;
                int fz = front == Direction.NORTH ? iz : front == Direction.SOUTH ? d - iz : front == Direction.WEST ? ix : w - ix;   // depth from the shop window
                int fx = zAxisFront ? ix : iz, fw = zAxisFront ? w : d;
                Direction toFront = front;
                if (fz == 3 && fx % 5 == 1 && r == 1) return f(CityBlocks.CHECKOUT_COUNTER, toFront);
                if (fz >= 6 && fz <= (zAxisFront ? d : w) - 3 && fz % 4 == 2 && fx >= 2 && fx <= fw - 2)
                    return f(CityBlocks.STORE_SHELF, toFront);
                if (r == 1 && nearWall && fz > 5 && (fx + fz) % 3 == 0) return f(CityBlocks.FRIDGE, inward);
                if (r == 1 && fz == 2 && fx == fw - 2) return f(CityBlocks.VENDING_MACHINE, toFront.getOpposite());
                return null;
            }
            case "apartment", "house" -> {
                return home(ix, iz, w, d, r, floor, x, z, bx0, bz0, inward, nearWall);
            }
            case "prison" -> {
                if (floor == 0 && iz <= 3) return r == 1 && iz == 2 && ix % 6 == 3 ? f(CityBlocks.DESK, Direction.SOUTH) : null;   // guard post by the door
                int mid = d / 2, cx = ix % 4;
                if (Math.abs(iz - mid) <= 1) return null;                                                        // the corridor down the middle
                boolean barsRow = iz == mid - 2 || iz == mid + 2;
                if (barsRow) return cx == 2 && r <= 2 ? AIR : r <= 3 ? Blocks.IRON_BARS.getDefaultState() : null; // cell bars, doors open
                if (cx == 0) return r <= 3 ? PARTITION : null;
                if (r != 1) return null;
                boolean north = iz < mid;
                int back = north ? 4 : d - 1, next = north ? 5 : d - 2;
                if (cx == 1 && iz == back) return Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, north ? Direction.NORTH : Direction.SOUTH).with(BedBlock.PART, BedPart.HEAD);
                if (cx == 1 && iz == next) return Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, north ? Direction.NORTH : Direction.SOUTH).with(BedBlock.PART, BedPart.FOOT);
                if (cx == 3 && iz == back) return Blocks.CAULDRON.getDefaultState();
                return null;
            }
            case "trailer" -> {
                if (r != 1) return null;
                if (ix == w - 1 && iz == 1) return Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD);
                if (ix == w - 1 && iz == 2) return Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT);
                if (ix == 1 && iz == 1) return f(CityBlocks.STOVE, Direction.EAST);
                if (ix == 1 && iz == 2) return f(CityBlocks.KITCHEN_COUNTER, Direction.EAST);
                if (ix == 1 && iz == d - 1) return f(CityBlocks.FRIDGE, Direction.EAST);
                if (ix == 3 && iz == d - 1) return f(CityBlocks.SOFA, Direction.NORTH);
                if (ix == 3 && iz == 1) return f(CityBlocks.TV, Direction.SOUTH);
                return null;
            }
            case "diner" -> {
                if (r != 1) return null;
                if (iz == d - 3 && ix >= 3 && ix <= w - 3) return f(CityBlocks.KITCHEN_COUNTER, Direction.NORTH);    // the counter
                if (iz == d - 4 && ix >= 3 && ix <= w - 3 && ix % 2 == 1) return f(CityBlocks.OFFICE_CHAIR, Direction.SOUTH);  // stools
                if (iz == d - 1 && ix >= 2 && ix <= w - 2) return ix % 3 == 0 ? f(CityBlocks.STOVE, Direction.NORTH)
                        : ix % 3 == 1 ? f(CityBlocks.FRIDGE, Direction.NORTH) : f(CityBlocks.KITCHEN_COUNTER, Direction.NORTH);
                if (iz >= 2 && iz <= d - 7 && (ix == 1 || ix == w - 1)) {                                     // window booths
                    int k = (iz - 2) % 4;
                    if (k == 0) return f(CityBlocks.SOFA, Direction.SOUTH);
                    if (k == 1) return f(CityBlocks.PICNIC_TABLE, Direction.SOUTH);
                    if (k == 2) return f(CityBlocks.SOFA, Direction.NORTH);
                }
                if (ix == w - 2 && iz == d - 5) return f(CityBlocks.VENDING_MACHINE, Direction.WEST);
                return null;
            }
            case "motel" -> {
                int rx = ix % 6;
                if (rx == 0) return r <= 3 ? PARTITION : null;                                                // a wall between rooms
                if (r != 1) return null;
                if (rx == 2 && iz == d - 1) return Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.HEAD);
                if (rx == 2 && iz == d - 2) return Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.FOOT);
                if (rx == 1 && iz == d - 1) return f(CityBlocks.DESK, Direction.NORTH);
                if (rx == 5 && iz == d - 3) return f(CityBlocks.TV, Direction.WEST);
                if (rx == 5 && iz == 2) return f(CityBlocks.POTTED_PLANT, Direction.WEST);
                return null;
            }
            case "dealership" -> {
                if (r != 1 || floor > 0) return null;
                if (iz == d - 2 && ix % 5 == 2) return f(CityBlocks.DESK, Direction.SOUTH);
                if (iz == d - 3 && ix % 5 == 2) return f(CityBlocks.OFFICE_CHAIR, Direction.NORTH);
                if (iz == d - 1 && ix % 5 == 3) return f(CityBlocks.FILING_CABINET, Direction.NORTH);
                if (nearWall && iz < d - 4 && (ix + iz) % 7 == 0) return f(CityBlocks.POTTED_PLANT, inward);
                if (ix == 2 && iz == 2) return f(CityBlocks.WATER_COOLER, Direction.EAST);
                return null;                                                                                    // the rest is showroom floor
            }
            case "mechanic" -> {
                if (r != 1) return null;
                if (iz == d - 1 && ix % 2 == 0) return ix % 4 == 0 ? f(CityBlocks.FILING_CABINET, Direction.NORTH) : f(CityBlocks.STORE_SHELF, Direction.NORTH);
                if (ix == 1 && iz % 3 == 1 && iz < d - 2) return f(CityBlocks.KITCHEN_COUNTER, Direction.EAST);  // workbenches
                return null;
            }
            case "bank" -> {
                if (floor > 0) return office(ix, iz, w, d, r, nearWall, inward, x, z, floor);
                if (iz == d - 6) return (ix == w / 2 || ix == w / 2 + 1) && r <= 2 ? AIR : Blocks.IRON_BLOCK.getDefaultState();   // vault wall
                if (iz > d - 6) return r == 1 && iz == d - 1 && ix % 2 == 1 ? Blocks.GOLD_BLOCK.getDefaultState() : AIR;
                if (r != 1) return null;
                if (iz == d - 9 && ix >= 3 && ix <= w - 3) return ix % 4 == 1 ? f(CityBlocks.COMPUTER, Direction.NORTH) : f(CityBlocks.CHECKOUT_COUNTER, Direction.NORTH);
                if (iz == 3 && (ix == 2 || ix == w - 2)) return f(CityBlocks.ATM, ix == 2 ? Direction.EAST : Direction.WEST);
                if (nearWall && iz < d - 9 && (ix + iz) % 5 == 0) return f(CityBlocks.POTTED_PLANT, inward);
                if ((iz == 5 || iz == 7) && ix % 6 == 3) return f(CityBlocks.PEW, Direction.NORTH);
                return null;
            }
            case "hospital" -> {
                if (floor == 0) {
                    if (r != 1) return null;
                    if (iz == 4 && ix >= w / 2 - 3 && ix <= w / 2 + 3) return f(CityBlocks.CHECKOUT_COUNTER, Direction.NORTH);
                    if (iz >= 7 && iz <= 11 && ix % 2 == 0 && (ix < w / 2 - 3 || ix > w / 2 + 3)) return f(CityBlocks.PEW, Direction.NORTH);  // waiting room
                    if (nearWall && (ix + iz) % 5 == 0) return f(CityBlocks.POTTED_PLANT, inward);
                    if (ix == w - 2 && iz == 2) return f(CityBlocks.VENDING_MACHINE, Direction.WEST);
                    return null;
                }
                // wards: rooms of four beds
                int rx = ix % 8, rz = iz % 8;
                if (rx == 0 || rz == 0) return partition(rx, rz, r);
                if (r != 1) return null;
                if ((rx == 2 || rx == 6) && (rz == 1 || rz == 5)) return Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD);
                if ((rx == 2 || rx == 6) && (rz == 2 || rz == 6)) return Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT);
                if (rx == 4 && rz == 1) return f(CityBlocks.WATER_COOLER, Direction.SOUTH);
                return null;
            }
            case "school" -> {
                int rx = ix % 8, rz = iz % 8;
                if (rx == 0 || rz == 0) {
                    if (rz == 0 && rx >= 2 && rx <= 6 && r == 2 && rx != 0) return Blocks.BLACK_CONCRETE.getDefaultState();   // blackboard
                    return partition(rx, rz, r);
                }
                if (r == 2 && rx == 4 && rz == 2) return f(CityBlocks.COMPUTER, Direction.SOUTH);
                if (r != 1) return null;
                if (rx == 4 && rz == 2) return f(CityBlocks.DESK, Direction.SOUTH);               // teacher
                if (rz >= 4 && rz <= 6 && rx % 2 == 1 && rx <= 7) return f(CityBlocks.DESK, Direction.SOUTH);
                if (rz == 7 && rx % 2 == 1) return f(CityBlocks.OFFICE_CHAIR, Direction.NORTH);
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    /** interior wall with doorways at positions 3-4 of each 8-block run */
    private BlockState partition(int rx, int rz, int r) {
        boolean doorway = (rx == 0 && (rz == 3 || rz == 4)) || (rz == 0 && (rx == 3 || rx == 4));
        if (doorway && r <= 2) return AIR;
        return PARTITION;
    }

    private BlockState office(int ix, int iz, int w, int d, int r, boolean nearWall, Direction inward, int x, int z, int floor) {
        int cx = ix % 6, cz = iz % 5;
        if (r == 2) {
            if (cz == 2 && (cx == 1 || cx == 4)) return f(CityBlocks.COMPUTER, Direction.NORTH);
            if (cz == 3 && (cx == 2 || cx == 5)) return f(CityBlocks.COMPUTER, Direction.SOUTH);
            return null;
        }
        if (r != 1) return null;
        if (nearWall) {
            if ((ix + iz) % 7 == 0) return f(CityBlocks.FILING_CABINET, inward);
            if ((ix + iz) % 11 == 3) return f(CityBlocks.POTTED_PLANT, inward);
            if (ix == w - 1 && iz == d / 2) return f(CityBlocks.WATER_COOLER, Direction.WEST);
            return null;
        }
        if (cx == 0 || cx == 3 || ix < 2 || ix > w - 2 || iz < 2 || iz > d - 2) return null;
        if (cz == 2) return f(CityBlocks.DESK, Direction.NORTH);
        if (cz == 1) return f(CityBlocks.OFFICE_CHAIR, Direction.SOUTH);
        if (cz == 3) return f(CityBlocks.DESK, Direction.SOUTH);
        if (cz == 4) return f(CityBlocks.OFFICE_CHAIR, Direction.NORTH);
        return null;
    }

    /** homes: 8x8 rooms, each a bedroom, living room or kitchen */
    private BlockState home(int ix, int iz, int w, int d, int r, int floor, int x, int z, int bx0, int bz0, Direction inward, boolean nearWall) {
        int rx = ix % 8, rz = iz % 8;
        if (w >= 10 && d >= 10 && (rx == 0 || rz == 0)) return partition(rx, rz, r);
        int roomX = ix / 8, roomZ = iz / 8;
        int type = hash(bx0 + roomX * 31 + floor * 7, bz0 + roomZ * 17, 120) % 3;
        if (w < 10) type = floor % 3;                                        // narrow rowhouses: a different room on each floor
        if (r == 3) return null;
        switch (type) {
            case 0 -> {                                                      // bedroom
                Block bed = BEDS[hash(bx0 + roomX, bz0 + roomZ + floor, 121) % BEDS.length];
                if (r == 1 && rx == 3 && rz == 1) return bed.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD);
                if (r == 1 && rx == 3 && rz == 2) return bed.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT);
                if (r == 1 && rx == 5 && rz == 1) return f(CityBlocks.FILING_CABINET, Direction.SOUTH);   // dresser
                if (r == 1 && rx == 1 && rz == 1) return f(CityBlocks.POTTED_PLANT, Direction.SOUTH);
                if (r == 1 && rx >= 2 && rx <= 4 && rz >= 3 && rz <= 5) return CARPETS[hash(bx0 + roomX, bz0 + roomZ, 122) % CARPETS.length].getDefaultState();
            }
            case 1 -> {                                                      // living room
                if (r == 1 && rz == 6 && rx >= 2 && rx <= 4) return f(CityBlocks.SOFA, Direction.NORTH);
                if (r == 1 && rz == 2 && rx == 3) return f(CityBlocks.TV, Direction.SOUTH);
                if (r == 1 && rx >= 2 && rx <= 4 && rz >= 3 && rz <= 5) return CARPETS[hash(bx0 + roomX, bz0 + roomZ, 123) % CARPETS.length].getDefaultState();
                if (r == 1 && rx == 6 && rz == 6) return f(CityBlocks.POTTED_PLANT, Direction.NORTH);
            }
            default -> {                                                     // kitchen and dining table
                if (rz == 1 && rx >= 1 && rx <= 6) {
                    if (r == 2) return null;
                    if (rx == 3) return f(CityBlocks.STOVE, Direction.SOUTH);
                    if (rx == 6) return f(CityBlocks.FRIDGE, Direction.SOUTH);
                    return f(CityBlocks.KITCHEN_COUNTER, Direction.SOUTH);
                }
                if (r == 1 && rx >= 3 && rx <= 4 && rz == 5) return f(CityBlocks.DESK, Direction.NORTH);
                if (r == 1 && rx >= 3 && rx <= 4 && (rz == 4 || rz == 6))
                    return Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, rz == 4 ? Direction.NORTH : Direction.SOUTH);
            }
        }
        return null;
    }

    // --------------------------------------------------- parks and parking

    /**
     * City park: a ring path with diagonals to the corners, a tiered fountain on a paved circle in the middle, a pond
     * with lily pads and reeds, a gazebo, a playground, picnic tables, benches, lamps, flower beds, a hedge round
     * the edge and plenty of trees (maple, linden, pine, blossom and willow).
     */
    private void park(int x, int z, int lx, int lz) {
        double dx = lx - 25.5, dz = lz - 25.5, rr = Math.sqrt(dx * dx + dz * dz);
        double ang = Math.toDegrees(Math.atan2(dz, dx)) + 180;               // 0..360
        // hedge round the outside with gaps where the paths come in
        boolean edge = lx == 0 || lx == 51 || lz == 0 || lz == 51;
        boolean entrance = (lx >= 24 && lx <= 27) || (lz >= 24 && lz <= 27) || Math.abs(Math.abs(dx) - Math.abs(dz)) < 1.5;
        if (edge) {
            if (!entrance) set(x, BASE, z, CityBlocks.LINDEN_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
            else set(x, GROUND, z, PATH);
            return;
        }
        if (rr < 9) {                                                        // paved fountain circle
            set(x, GROUND, z, (int) (rr * 1.3 + ang / 30) % 2 == 0 ? PLAZA : Blocks.POLISHED_DIORITE.getDefaultState());
            if (rr < 5.2) fountain(x, z, rr);
            else if (rr > 7.6 && rr < 8.6 && Math.floorMod((int) (ang / 15), 3) == 0)
                set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, dirTo(-dx, -dz)));
            return;
        }
        if (rr < 11) {                                                       // flower ring
            set(x, GROUND, z, Blocks.GRASS_BLOCK.getDefaultState());
            set(x, BASE, z, FLOWERS[(int) (ang / 20) % FLOWERS.length]);
            return;
        }
        boolean ring = rr >= 17 && rr < 20;
        boolean spoke = rr >= 11 && (Math.abs(Math.abs(dx) - Math.abs(dz)) < 1.3 || Math.abs(dx) < 1.6 || Math.abs(dz) < 1.6);
        if (ring || spoke) {
            set(x, GROUND, z, ring && (int) (ang * 3) % 7 == 0 ? Blocks.COARSE_DIRT.getDefaultState() : PATH);
            return;
        }
        // benches and lamps along the ring path
        if (Math.abs(rr - 16.4) < 0.5 && Math.floorMod((int) (ang / 6), 6) == 2) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, dirTo(dx, dz)));
            return;
        }
        if (Math.abs(rr - 20.6) < 0.5 && Math.floorMod((int) (ang / 4), 11) == 0) {
            lamp(x, z, dirTo(-dx, -dz));
            return;
        }
        // pond in the north-east quarter
        double px = (lx - 39) / 6.5, pz = (lz - 11) / 4.2, pe = px * px + pz * pz;
        if (pe < 1) {
            set(x, GROUND - 1, z, Blocks.CLAY.getDefaultState());
            set(x, GROUND, z, WATER);
            if (pe < 0.6) set(x, GROUND - 1, z, WATER);
            if (rnd(x, z, 130) < 0.08f) set(x, BASE, z, Blocks.LILY_PAD.getDefaultState());
            return;
        }
        if (pe < 1.35) {
            set(x, GROUND, z, rnd(x, z, 131) < 0.5f ? Blocks.SAND.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
            if (pe < 1.15 && rnd(x, z, 132) < 0.3f) {
                set(x, GROUND, z, Blocks.GRASS_BLOCK.getDefaultState());
                for (int y = BASE; y < BASE + 2 + hash(x, z, 133) % 2; y++) set(x, y, z, Blocks.SUGAR_CANE.getDefaultState());
            }
            return;
        }
        // gazebo in the south-west quarter
        int gx = lx - 11, gz = lz - 40;
        if (Math.abs(gx) <= 4 && Math.abs(gz) <= 4) {
            gazebo(x, z, gx, gz);
            return;
        }
        // playground in the south-east quarter
        int qx = lx - 40, qz = lz - 40;
        if (Math.abs(qx) <= 5 && Math.abs(qz) <= 5) {
            playground(x, z, qx, qz);
            return;
        }
        // picnic area in the north-west quarter
        int tx = lx - 11, tz = lz - 11;
        if (Math.abs(tx) <= 5 && Math.abs(tz) <= 4 && Math.floorMod(tx, 4) == 1 && Math.floorMod(tz, 4) == 1) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.PICNIC_TABLE, Direction.NORTH));
            return;
        }
        if (rnd(x, z, 11) < 0.04f) set(x, BASE, z, FLOWERS[hash(x, z, 12) % FLOWERS.length]);
        else if (rnd(x, z, 13) < 0.12f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
    }

    private static Direction dirTo(double dx, double dz) {
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /** tiered fountain, radius ~5, centred on this column's distance rr from the middle */
    private void fountain(int x, int z, double rr) {
        BlockState stone = Blocks.STONE_BRICKS.getDefaultState();
        set(x, GROUND - 1, z, stone);
        if (rr >= 4.3) {                                                     // outer rim to sit on
            set(x, GROUND, z, stone);
            set(x, BASE, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
            return;
        }
        if (rr >= 2.4) {                                                     // lower basin
            set(x, GROUND, z, WATER);
            return;
        }
        set(x, GROUND, z, stone);
        set(x, BASE, z, rr < 0.8 ? Blocks.CHISELED_STONE_BRICKS.getDefaultState() : stone);
        set(x, BASE + 1, z, rr < 0.8 ? Blocks.CHISELED_STONE_BRICKS.getDefaultState() : Blocks.SMOOTH_STONE.getDefaultState());
        if (rr >= 1.8) set(x, BASE + 2, z, Blocks.STONE_BRICK_WALL.getDefaultState());     // upper bowl rim
        else if (rr >= 0.8) set(x, BASE + 2, z, WATER);
        else {
            set(x, BASE + 2, z, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
            set(x, BASE + 3, z, CityBlocks.FOUNTAIN_SPOUT.getDefaultState());
        }
    }

    /** octagonal gazebo, 9x9, on a plank floor with a pointed roof */
    private void gazebo(int x, int z, int gx, int gz) {
        int ax = Math.abs(gx), az = Math.abs(gz), m = Math.max(ax, az);
        if (ax + az > 7) return;                                             // cut the corners: octagon
        set(x, GROUND, z, Blocks.SPRUCE_PLANKS.getDefaultState());
        boolean post = (m == 4 && (ax == 0 || az == 0 || ax == 3 || az == 3)) || (ax + az == 7);
        boolean open = (ax <= 1 && az == 4) || (az <= 1 && ax == 4);
        if (m == 4 || ax + az == 7) {
            if (post && !open) for (int y = BASE; y < BASE + 3; y++) set(x, y, z, Blocks.SPRUCE_FENCE.getDefaultState());
            else if (!open) set(x, BASE, z, Blocks.SPRUCE_FENCE.getDefaultState());
        } else if (m == 3 && !(ax <= 1 || az <= 1)) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, dirTo(gx, gz)));
        }
        if (gx == 0 && gz == 0) set(x, BASE + 2, z, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
        int roof = BASE + 3 + Math.max(0, 4 - m);
        set(x, roof, z, m >= 1 ? Blocks.DARK_OAK_PLANKS.getDefaultState() : Blocks.DARK_OAK_FENCE.getDefaultState());
        if (m == 0) set(x, roof + 1, z, Blocks.DARK_OAK_FENCE.getDefaultState());
    }

    /** sandpit with a slide, swings and a climbing frame */
    private void playground(int x, int z, int qx, int qz) {
        set(x, GROUND, z, Blocks.SAND.getDefaultState());
        // slide: a ladder up to a 3-high platform, then a run of quartz stairs down to the east
        if (qz == -3 && qx >= -4 && qx <= -1) {
            int n = qx + 4;                                                  // 0 = platform, 1..3 = the slide
            int topY = BASE + 3 - n;
            for (int y = BASE; y < topY; y++) set(x, y, z, Blocks.QUARTZ_BLOCK.getDefaultState());
            set(x, topY, z, n == 0 ? Blocks.QUARTZ_SLAB.getDefaultState() : Blocks.QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
            return;
        }
        if (qz == -2 && qx == -4) {
            for (int y = BASE; y < BASE + 4; y++) set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
            return;
        }
        // swings
        if (qx >= 1 && qx <= 4 && qz == 2) {
            if (qx == 1 || qx == 4) for (int y = BASE; y < BASE + 4; y++) set(x, y, z, Blocks.OAK_FENCE.getDefaultState());
            set(x, BASE + 4, z, Blocks.OAK_PLANKS.getDefaultState());
            if (qx == 2 || qx == 3) {
                set(x, BASE + 3, z, Blocks.CHAIN.getDefaultState());
                set(x, BASE + 2, z, Blocks.CHAIN.getDefaultState());
                set(x, BASE + 1, z, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
            }
            return;
        }
        // climbing frame
        if (qx >= -3 && qx <= -1 && qz >= 1 && qz <= 3) {
            boolean frame = qx == -3 || qx == -1 || qz == 1 || qz == 3;
            if (frame) for (int y = BASE; y < BASE + 3; y++) if ((y + qx + qz) % 2 == 0 || qx * qz == 3 || Math.abs(qx * qz) == 1) set(x, y, z, Blocks.RED_CONCRETE.getDefaultState());
            return;
        }
        if (Math.abs(qx) == 5 || Math.abs(qz) == 5) {
            if (!(qx == 0 && qz == -5)) set(x, BASE, z, Blocks.OAK_FENCE.getDefaultState());
            else set(x, GROUND, z, PATH);
        }
    }

    private static final BlockState[] STAINED = {
            Blocks.RED_STAINED_GLASS.getDefaultState(), Blocks.BLUE_STAINED_GLASS.getDefaultState(), Blocks.YELLOW_STAINED_GLASS.getDefaultState(),
            Blocks.PURPLE_STAINED_GLASS.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState(), Blocks.ORANGE_STAINED_GLASS.getDefaultState(),
            Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.MAGENTA_STAINED_GLASS.getDefaultState()
    };

    private BlockState churchStone(int x, int y, int z) {
        float v = rnd(x * 7 + y, z, 160);
        return v < 0.12f ? Blocks.MOSSY_STONE_BRICKS.getDefaultState() : v < 0.2f ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState();
    }

    /**
     * Stone church: a bell tower and spire at the front, a nave with tall stained-glass windows, buttresses, a steep
     * slate roof and a rose window over the altar; pews either side of a red-carpet aisle, chandeliers and candles;
     * a graveyard round the sides and back behind an iron fence.
     */
    private void church(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        loot = "house";
        // trees on the lawn and in the graveyard
        int[][] trees = {{6, 6, 0}, {45, 6, 1}, {4, 47, 2}, {47, 47, 4}, {10, 30, 2}, {41, 30, 2}};
        for (int[] t : trees) {
            if (Math.abs(lx - t[0]) <= 4 && Math.abs(lz - t[1]) <= 4) tree(x, z, ox + t[0], oz + t[1], (t[2] << 8) | (hash(bx, bz, 161 + t[0]) & 0xff));
        }
        // fence and gate
        if (lx == 0 || lx == 51 || lz == 0 || lz == 51) {
            boolean gate = lz == 0 && lx >= 22 && lx <= 29;
            if (gate) {
                set(x, GROUND, z, Blocks.STONE_BRICKS.getDefaultState());
                return;
            }
            boolean post = (lx % 6 == 0) && (lz == 0 || lz == 51) || (lz % 6 == 0) && (lx == 0 || lx == 51);
            if (post) {
                set(x, BASE, z, Blocks.STONE_BRICKS.getDefaultState());
                set(x, BASE + 1, z, Blocks.STONE_BRICK_SLAB.getDefaultState());
            } else {
                boolean alongX = lz == 0 || lz == 51;
                set(x, BASE, z, Blocks.IRON_BARS.getDefaultState()
                        .with(net.minecraft.block.HorizontalConnectingBlock.EAST, alongX).with(net.minecraft.block.HorizontalConnectingBlock.WEST, alongX)
                        .with(net.minecraft.block.HorizontalConnectingBlock.NORTH, !alongX).with(net.minecraft.block.HorizontalConnectingBlock.SOUTH, !alongX));
            }
            return;
        }
        boolean inTower = lx >= 22 && lx <= 29 && lz >= 6 && lz <= 13;
        boolean inNave = lx >= 17 && lx <= 34 && lz >= 14 && lz <= 44;
        boolean roofArea = lx >= 16 && lx <= 35 && lz >= 13 && lz <= 45;
        // paths
        if (!inTower && !inNave) {
            boolean path = (lx >= 23 && lx <= 28 && lz < 6) || ((lx == 15 || lx == 36) && lz >= 12 && lz <= 46) || (lz == 46 && lx >= 15 && lx <= 36);
            if (path) set(x, GROUND, z, lz < 6 ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.GRAVEL.getDefaultState());
        }
        if (inTower) {
            churchTower(x, z, lx, lz);
            return;
        }
        if (inNave) {
            churchNave(x, z, lx, lz);
        } else if ((lx == 16 || lx == 35) && lz >= 14 && lz <= 44 && (lz - 14) % 5 == 0) {
            for (int y = BASE; y <= BASE + 8; y++) set(x, y, z, churchStone(x, y, z));          // buttress
            set(x, BASE + 9, z, Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, lx == 16 ? Direction.EAST : Direction.WEST));
        }
        if (roofArea && !(lz <= 13 && lx >= 22 && lx <= 29)) {
            int e = Math.min(lx - 16, 35 - lx);                              // 0 at the eaves, 9 at the ridge
            int ry = BASE + 12 + e;
            if (e >= 9) set(x, ry, z, Blocks.DEEPSLATE_TILES.getDefaultState());
            else {
                set(x, ry, z, Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState().with(StairsBlock.FACING, lx < 26 ? Direction.EAST : Direction.WEST));
                if (e > 0) set(x, ry - 1, z, Blocks.DEEPSLATE_TILES.getDefaultState());
            }
            if ((lz == 14 || lz == 44) && lx >= 17 && lx <= 34) {          // gable walls
                for (int y = BASE + 12; y < ry - 1; y++) set(x, y, z, rose(lx, y, lz) ? roseGlass(lx, y) : churchStone(x, y, z));
            }
        }
        // the graveyard
        if (!inNave && !roofArea) {
            if (grave(lx, lz, x, z, trees)) set(x, BASE, z, CityBlocks.facing(CityBlocks.GRAVESTONE, Direction.NORTH));
            else if (grave(lx, lz + 1, x, z + 1, trees) && rnd(x, z + 1, 163) < 0.25f) set(x, BASE, z, FLOWERS[hash(x, z, 164) % FLOWERS.length]);
        }
    }

    private boolean grave(int lx, int lz, int x, int z, int[][] trees) {
        boolean yard = (lz >= 47 && lz <= 50 && lx >= 2 && lx <= 49) || (lz >= 16 && lz <= 44 && ((lx >= 2 && lx <= 13) || (lx >= 38 && lx <= 49)));
        if (!yard || lx % 3 != 1 || lz % 4 != 2) return false;
        for (int[] t : trees) if (Math.abs(lx - t[0]) <= 2 && Math.abs(lz - t[1]) <= 2) return false;
        return rnd(x, z, 162) < 0.75f;
    }

    private boolean rose(int lx, int y, int lz) {
        if (lz != 44) return false;
        double dx = lx - 25.5, dy = y - (BASE + 8.5);
        return dx * dx + dy * dy < 12.5;
    }

    private BlockState roseGlass(int lx, int y) {
        double dx = lx - 25.5, dy = y - (BASE + 8.5), r = Math.sqrt(dx * dx + dy * dy);
        double a = Math.toDegrees(Math.atan2(dy, dx)) + 360;
        if (r > 1.2 && Math.abs((a % 45) - 22.5) > 19) return Blocks.STONE_BRICKS.getDefaultState();         // tracery spokes
        return r < 1.2 ? STAINED[2] : r < 2.4 ? STAINED[0] : STAINED[1];
    }

    private void churchNave(int x, int z, int lx, int lz) {
        boolean wallX = lx == 17 || lx == 34, wallZ = lz == 14 || lz == 44;
        cryptColumn(x, z, lx, lz);
        set(x, GROUND, z, Blocks.POLISHED_ANDESITE.getDefaultState());
        if (wallX || wallZ) {
            boolean towerArch = lz == 14 && lx >= 24 && lx <= 27;
            int win = (lz - 14) % 5;
            for (int y = BASE; y <= BASE + 11; y++) {
                if (towerArch && y <= BASE + 3 + (lx == 25 || lx == 26 ? 1 : 0)) continue;
                BlockState b = churchStone(x, y, z);
                if (wallX && !wallZ && (win == 2 || win == 3) && y >= BASE + 3 && y <= BASE + 9 && !(y == BASE + 9 && win == 3))
                    b = STAINED[Math.floorMod(lz / 5 + (y - BASE) / 3 + (lx == 17 ? 0 : 4), STAINED.length)];
                if (rose(lx, y, lz)) b = roseGlass(lx, y);
                set(x, y, z, b);
            }
            return;
        }
        // chancel: a step up, the altar, candles and a lectern
        if (lz >= 38) {
            if (lx == 25 && lz == 43) {
                set(x, GROUND, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
                set(x, BASE, z, CityBlocks.SECRET_QUARTZ.getDefaultState());
                return;
            }
            set(x, BASE, z, Blocks.SMOOTH_QUARTZ.getDefaultState());
            if ((lx == 25 || lx == 26) && lz == 41) {
                set(x, BASE + 1, z, CityBlocks.facing(CityBlocks.ALTAR, Direction.NORTH));
                set(x, BASE + 2, z, Blocks.CANDLE.getDefaultState().with(net.minecraft.block.CandleBlock.CANDLES, 3).with(net.minecraft.block.CandleBlock.LIT, true));
            }
            if ((lx == 19 || lx == 32) && lz == 39) {
                set(x, BASE + 1, z, Blocks.STONE_BRICK_WALL.getDefaultState());
                set(x, BASE + 2, z, Blocks.CANDLE.getDefaultState().with(net.minecraft.block.CandleBlock.CANDLES, 4).with(net.minecraft.block.CandleBlock.LIT, true));
            }
            if (lx == 22 && lz == 39) set(x, BASE + 1, z, Blocks.LECTERN.getDefaultState().with(net.minecraft.block.LecternBlock.FACING, Direction.NORTH));
            if (lz == 43 && lx >= 20 && lx <= 31 && lx % 2 == 0) set(x, BASE + 1, z, CityBlocks.facing(CityBlocks.PEW, Direction.NORTH));   // choir
            return;
        }
        if (lx == 25 || lx == 26) {
            set(x, BASE, z, Blocks.RED_CARPET.getDefaultState());
        } else if (lz >= 17 && lz <= 35 && lz % 2 == 1 && ((lx >= 19 && lx <= 23) || (lx >= 28 && lx <= 32))) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.PEW, Direction.SOUTH));
        }
        // chandeliers down the middle
        if (lx == 25 && (lz == 20 || lz == 27 || lz == 34)) {
            for (int y = BASE + 9; y <= BASE + 19; y++) set(x, y, z, Blocks.CHAIN.getDefaultState());
            set(x, BASE + 8, z, Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
        }
        if (lx == 24 && lz == 15) set(x, BASE, z, f(CityBlocks.POTTED_PLANT, Direction.SOUTH));
    }

    private void churchTower(int x, int z, int lx, int lz) {
        boolean wall = lx == 22 || lx == 29 || lz == 6 || lz == 13;
        boolean corner = (lx == 22 || lx == 29) && (lz == 6 || lz == 13);
        int top = BASE + 26;
        set(x, GROUND, z, Blocks.POLISHED_ANDESITE.getDefaultState());
        for (int y = BASE; y <= top; y++) {
            if (!wall) {
                if (y == BASE + 19 && !(lx == 23 && lz == 12)) set(x, y, z, Blocks.SPRUCE_PLANKS.getDefaultState());   // belfry floor
                if (lx == 23 && lz == 12 && y <= BASE + 19) set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST));
                continue;
            }
            boolean faceMid = (lz == 6 || lz == 13) ? (lx >= 24 && lx <= 27) : (lz >= 8 && lz <= 11);
            if (corner) { set(x, y, z, Blocks.POLISHED_ANDESITE.getDefaultState()); continue; }
            if (lz == 6 && (lx == 25 || lx == 26) && y <= BASE + 1) {        // front doors
                set(x, y, z, Blocks.DARK_OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH)
                        .with(DoorBlock.HINGE, lx == 25 ? net.minecraft.block.enums.DoorHinge.LEFT : net.minecraft.block.enums.DoorHinge.RIGHT)
                        .with(DoorBlock.HALF, y == BASE ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER));
                continue;
            }
            if (lz == 6 && (lx == 24 || lx == 27) && y <= BASE + 2) { set(x, y, z, Blocks.CHISELED_STONE_BRICKS.getDefaultState()); continue; }
            if (lz == 13 && lx >= 24 && lx <= 27 && y <= BASE + 3 + (lx == 25 || lx == 26 ? 1 : 0)) continue;   // arch into the nave
            if (faceMid && y >= BASE + 21 && y <= BASE + 24 && !(y == BASE + 24 && (lx == 24 || lx == 27 || lz == 8 || lz == 11))) continue;   // belfry arches
            if (lz == 6 && (lx == 25 || lx == 26) && (y == BASE + 8 || y == BASE + 9 || y == BASE + 14 || y == BASE + 15)) {
                set(x, y, z, STAINED[(y - BASE) % STAINED.length]);
                continue;
            }
            set(x, y, z, y == top || y == BASE + 20 ? Blocks.POLISHED_ANDESITE.getDefaultState() : churchStone(x, y, z));
        }
        if (lx == 25 && lz == 9) set(x, BASE + 20, z, Blocks.BELL.getDefaultState());
        // spire
        for (int k = 0; k < 12; k++) {
            int inset = k / 3, y = top + 1 + k;
            if (lx >= 22 + inset && lx <= 29 - inset && lz >= 6 + inset && lz <= 13 - inset) set(x, y, z, Blocks.DEEPSLATE_TILES.getDefaultState());
        }
        // gold cross on top
        int ct = top + 13;
        if (lx == 25 && lz == 9) for (int y = ct; y <= ct + 4; y++) set(x, y, z, Blocks.GOLD_BLOCK.getDefaultState());
        if ((lx == 24 || lx == 26) && lz == 9) set(x, ct + 3, z, Blocks.GOLD_BLOCK.getDefaultState());
    }

    /** park trees: one per 7x7 patch, kept off the paths and attractions */
    private void parkTrees(int x, int z, int ox, int oz) {
        for (int gx = Math.floorDiv(x - ox - 4, 7); gx <= Math.floorDiv(x - ox + 4, 7); gx++) {
            for (int gz = Math.floorDiv(z - oz - 4, 7); gz <= Math.floorDiv(z - oz + 4, 7); gz++) {
                if (gx < 0 || gz < 0 || gx > 7 || gz > 7) continue;
                int h = hash(ox + gx, oz + gz, 140);
                int tlx = gx * 7 + 2 + h % 4, tlz = gz * 7 + 2 + (h >>> 4) % 4;
                if (tlx < 3 || tlz < 3 || tlx > 48 || tlz > 48) continue;
                if (!treeSpot(tlx, tlz)) continue;
                if (rnd(ox + gx, oz + gz, 141) > 0.75f) continue;
                tree(x, z, ox + tlx, oz + tlz, h);
            }
        }
    }

    /** is this park position clear of paths, the fountain, the pond and the play areas? */
    private boolean treeSpot(int lx, int lz) {
        double dx = lx - 25.5, dz = lz - 25.5, rr = Math.sqrt(dx * dx + dz * dz);
        if (rr < 14 || (rr >= 15.5 && rr < 22)) return false;
        if (Math.abs(Math.abs(dx) - Math.abs(dz)) < 3.5 || Math.abs(dx) < 3.5 || Math.abs(dz) < 3.5) return false;
        double px = (lx - 39) / 8.5, pz = (lz - 11) / 6.2;
        if (px * px + pz * pz < 1) return false;
        if (Math.abs(lx - 11) <= 6 && Math.abs(lz - 40) <= 6) return false;
        if (Math.abs(lx - 40) <= 7 && Math.abs(lz - 40) <= 7) return false;
        return !(Math.abs(lx - 11) <= 7 && Math.abs(lz - 11) <= 6);
    }

    /**
     * One column of a tree whose trunk stands at (tx, tz). Species by hash: maple (round red/orange crown),
     * linden (tall rounded green), pine (cone), blossom (cherry) and willow (drooping green with hanging vines).
     */
    private void tree(int x, int z, int tx, int tz, int h) {
        int species = (h >>> 8) % 5;
        int dx = x - tx, dz = z - tz;
        double d = Math.sqrt(dx * dx + dz * dz);
        BlockState log, leaves;
        switch (species) {
            case 0 -> { log = Blocks.DARK_OAK_LOG.getDefaultState(); leaves = CityBlocks.MAPLE_LEAVES.getDefaultState(); }
            case 1 -> { log = Blocks.OAK_LOG.getDefaultState(); leaves = CityBlocks.LINDEN_LEAVES.getDefaultState(); }
            case 2 -> { log = Blocks.SPRUCE_LOG.getDefaultState(); leaves = Blocks.SPRUCE_LEAVES.getDefaultState(); }
            case 3 -> { log = Blocks.CHERRY_LOG.getDefaultState(); leaves = Blocks.CHERRY_LEAVES.getDefaultState(); }
            default -> { log = Blocks.OAK_LOG.getDefaultState(); leaves = CityBlocks.LINDEN_LEAVES.getDefaultState(); }
        }
        leaves = leaves.with(LeavesBlock.PERSISTENT, true);
        int trunk = species == 2 ? 2 : 4 + (h >>> 12) % 3;
        if (dx == 0 && dz == 0) for (int y = BASE; y < BASE + trunk + (species == 2 ? 7 : 1); y++) set(x, y, z, log);
        switch (species) {
            case 2 -> {                                                      // pine: stacked cone
                for (int i = 0; i < 9; i++) {
                    int y = BASE + trunk + i;
                    double rad = (9 - i) * 0.38 + (i % 2 == 0 ? 0.5 : 0);
                    if (d <= rad && d > 0 && isAir(x, y, z)) set(x, y, z, leaves);
                }
                if (d == 0) set(x, BASE + trunk + 9, z, leaves);
            }
            case 4 -> {                                                      // willow: wide dome, curtains of vines
                for (int y = BASE + trunk - 1; y <= BASE + trunk + 3; y++) {
                    double ry = y - (BASE + trunk + 1);
                    double rad = Math.sqrt(Math.max(0, 16 - ry * ry * 2.5));
                    if (d <= rad && isAir(x, y, z) && !(d == 0 && y < BASE + trunk + 1)) set(x, y, z, leaves);
                }
                if (d > 2.6 && d <= 4 && rnd(x, z, 142) < 0.6f) {
                    int len = 2 + hash(x, z, 143) % 3;
                    for (int y = BASE + trunk - 2; y > BASE + trunk - 2 - len && y > BASE; y--) {
                        if (isAir(x, y, z)) set(x, y, z, leaves);
                    }
                }
            }
            default -> {                                                     // round crown
                double cy = BASE + trunk + 1.5, rad = species == 1 ? 2.6 : 3.2;
                double sy = species == 1 ? 1.4 : 1.0;                         // lindens are taller than wide
                for (int y = BASE + trunk - 1; y <= BASE + trunk + 5; y++) {
                    double ry = (y - cy) / sy;
                    double e = Math.sqrt(d * d + ry * ry);
                    if (e <= rad + (rnd(x, y * 31 + z, 144) - 0.5) * 0.6 && isAir(x, y, z)) set(x, y, z, leaves);
                }
            }
        }
    }

    private static final int[] ROWS = {6, 18, 30, 42};

    private void parking(int x, int z, int lx, int lz) {
        set(x, GROUND, z, ASPHALT);
        boolean margin = lx < 1 || lx > 50 || lz < 1 || lz > 50;
        if (margin) {
            boolean entrance = (lx >= 22 && lx < 30) || (lz >= 22 && lz < 30);
            if (!entrance) {
                set(x, GROUND, z, Blocks.GRASS_BLOCK.getDefaultState());
                set(x, BASE, z, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
            }
            return;
        }
        for (int row : ROWS) {
            if (lz >= row - 2 && lz < row + 3 && lx % 3 == 0 && lx > 1 && lx < 50) {
                set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z));
                return;
            }
        }
        if ((lz == 12 || lz == 36) && lx % 12 == 6) lamp(x, z, Direction.SOUTH);
    }

    private void gas(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 30 && lx <= 43 && lz >= 28 && lz <= 39) {                  // shop
            loot = "shop";
            building(x, z, ox + 30, oz + 28, ox + 43, oz + 39, 1, STYLES[6], Direction.NORTH, false);
            if (lx == 32 && lz == 30) {
                set(x, BASE + 1, z, Blocks.BARREL.getDefaultState());
                pos.set(x, BASE + 1, z);
                if (w.getBlockEntity(pos) instanceof LootableContainerBlockEntity be) {
                    be.setStack(0, new ItemStack(ModRegistry.FUEL_CAN, 4));
                    be.setStack(1, new ItemStack(ModRegistry.FUEL_CAN, 2));
                }
            }
            return;
        }
        set(x, GROUND, z, ASPHALT);
        boolean canopy = lx >= 10 && lx < 30 && lz >= 8 && lz < 22;
        if (canopy) {
            boolean rim = lx == 10 || lx == 29 || lz == 8 || lz == 21;
            set(x, BASE + 5, z, rim ? Blocks.RED_CONCRETE.getDefaultState() : lx % 4 == 0 && lz % 4 == 0 ? LIGHT : Blocks.WHITE_CONCRETE.getDefaultState());
            if ((lx == 11 || lx == 28) && (lz == 9 || lz == 20)) {
                for (int y = BASE; y < BASE + 5; y++) set(x, y, z, Blocks.STONE_BRICK_WALL.getDefaultState());
            }
            if ((lz == 14 || lz == 15) && lx >= 13 && lx <= 26) {            // pump islands
                if ((lx == 15 || lx == 20 || lx == 25) && lz == 14) {
                    set(x, BASE, z, com.nothatcher.vehicleforge.block.RoadBlocks.FUEL_PUMP.getDefaultState()
                            .with(net.minecraft.block.HorizontalFacingBlock.FACING, Direction.SOUTH));   // pumps with a limited supply
                } else {
                    set(x, BASE, z, CURB);
                }
            }
            return;
        }
        if (lx == 4 && lz == 4) {                                            // price sign
            for (int y = BASE; y < BASE + 7; y++) set(x, y, z, Blocks.ANDESITE_WALL.getDefaultState());
            set(x, BASE + 7, z, Blocks.RED_CONCRETE.getDefaultState());
            set(x, BASE + 8, z, Blocks.OCHRE_FROGLIGHT.getDefaultState());
        }
    }

    // -------------------------------------------------------------- houses

    private record Lot(int ox, int oz, int hx0, int hx1, int hz0, int hz1, Direction face, boolean alongX) {}

    private Lot lot(int bx, int bz, int li, int lj) {
        int ox = bx * CELL + C + li * 17, oz = bz * CELL + C + lj * 17;
        Direction face = lj == 0 ? Direction.NORTH : lj == 2 ? Direction.SOUTH : li == 0 ? Direction.WEST : Direction.EAST;
        boolean alongX = face.getAxis() == Direction.Axis.Z;
        return alongX ? new Lot(ox, oz, ox + 3, ox + 12, oz + 4, oz + 11, face, true)
                : new Lot(ox, oz, ox + 4, ox + 11, oz + 3, oz + 12, face, false);
    }

    private void houses(int x, int z, int bx, int bz, int lx, int lz) {
        if (lx >= 51 || lz >= 51) return;
        int li = lx / 17, lj = lz / 17, px = lx % 17, pz = lz % 17;
        if (li == 1 && lj == 1) {                                            // shared backyard with a pool
            if (px >= 5 && px <= 10 && pz >= 5 && pz <= 10) {
                boolean rim = px == 5 || px == 10 || pz == 5 || pz == 10;
                set(x, GROUND, z, rim ? WALK : WATER);
                if (!rim) set(x, GROUND - 1, z, Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState());
            }
            return;
        }
        Lot l = lot(bx, bz, li, lj);
        HouseStyle hs = HOUSES[hash(bx * 3 + li, bz * 3 + lj, 21) % HOUSES.length];
        if (bunker(l)) bunkerColumn(x, z, l);
        if (x >= l.hx0 - 1 && x <= l.hx1 + 1 && z >= l.hz0 - 1 && z <= l.hz1 + 1) {
            houseColumn(x, z, l, hs);
            return;
        }
        // front walk and driveway
        int amid = l.alongX ? (l.hx0 + l.hx1) / 2 : (l.hz0 + l.hz1) / 2;
        boolean walk = switch (l.face) {
            case NORTH -> x == amid && z < l.hz0;
            case SOUTH -> x == amid && z > l.hz1;
            case WEST -> z == amid && x < l.hx0;
            default -> z == amid && x > l.hx1;
        };
        boolean drive;
        if (hasGarage(l)) {
            int g0 = (l.alongX ? l.hx1 : l.hz1) - 3, g1 = g0 + 2;
            drive = switch (l.face) {
                case NORTH -> x >= g0 && x <= g1 && z < l.hz0;
                case SOUTH -> x >= g0 && x <= g1 && z > l.hz1;
                case WEST -> z >= g0 && z <= g1 && x < l.hx0;
                default -> z >= g0 && z <= g1 && x > l.hx1;
            };
        } else {
            drive = l.alongX ? (px >= 14 && (l.face == Direction.NORTH ? z <= l.hz1 : z >= l.hz0))
                    : (pz >= 14 && (l.face == Direction.WEST ? x <= l.hx1 : x >= l.hx0));
        }
        if (walk) set(x, GROUND, z, WALK);
        else if (drive) set(x, GROUND, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
        else if (rnd(x, z, 13) < 0.03f) set(x, BASE, z, FLOWERS[hash(x, z, 14) % FLOWERS.length]);
    }

    /** A house with a gable roof (stairs) along its long side, windows, a front door and a lit room. */
    private void houseColumn(int x, int z, Lot l, HouseStyle hs) {
        int a = l.alongX ? x : z, a0 = l.alongX ? l.hx0 : l.hz0, a1 = l.alongX ? l.hx1 : l.hz1;
        int c = l.alongX ? z : x, c0 = l.alongX ? l.hz0 : l.hx0, c1 = l.alongX ? l.hz1 : l.hx1;
        int fromLow = c - (c0 - 1), fromHigh = (c1 + 1) - c;
        int k = Math.min(fromLow, fromHigh);
        int roofY = BASE + 4 + k;
        Direction up = fromLow <= fromHigh ? (l.alongX ? Direction.SOUTH : Direction.EAST) : (l.alongX ? Direction.NORTH : Direction.WEST);
        set(x, roofY, z, hs.stairs.getDefaultState().with(StairsBlock.FACING, up));
        boolean inA = a >= a0 && a <= a1, inC = c >= c0 && c <= c1;
        // attached garage: a 3-wide bay at the high-a end with a roll-up door facing the street
        boolean garage = hasGarage(l);
        int g0 = a1 - 3, g1 = a1 - 1, gw = a1 - 4;
        boolean frontLow = l.face == Direction.NORTH || l.face == Direction.WEST;
        int back = frontLow ? c1 - 1 : c0 + 1;
        boolean bay = garage && a >= g0 && a <= g1;
        if (bay && c == (frontLow ? c0 - 1 : c1 + 1)) set(x, GROUND, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
        if (!(inA && inC)) return;
        boolean edgeA = a == a0 || a == a1, edgeC = c == c0 || c == c1;
        set(x, GROUND, z, bay ? Blocks.SMOOTH_STONE.getDefaultState() : hs.floor);
        set(x, BASE, z, bay && !edgeC ? AIR : hs.floor);
        if (edgeA || edgeC) {
            boolean frontWall = switch (l.face) {
                case NORTH, WEST -> c == c0;
                default -> c == c1;
            };
            if (bay && frontWall) {
                Direction.Axis axis = l.alongX ? Direction.Axis.X : Direction.Axis.Z;
                for (int r = 0; r < 3; r++) {
                    set(x, BASE + r, z, CityBlocks.GARAGE_DOOR.getDefaultState().with(com.nothatcher.vehicleforge.block.GarageDoorBlock.AXIS, axis)
                            .with(com.nothatcher.vehicleforge.block.GarageDoorBlock.PART, (a - g0) + 3 * r));
                }
                set(x, BASE + 3, z, hs.wall);
                set(x, BASE + 4, z, hs.wall);
                return;
            }
            int amid = (a0 + a1) / 2;
            boolean door = frontWall && a == amid && !edgeA;
            int wallAlong = edgeC ? a : c;
            boolean corner = edgeA && edgeC;
            for (int y = BASE + 1; y <= BASE + 3; y++) {
                if (door && y <= BASE + 2) {
                    BlockState d = hs.door.getDefaultState().with(DoorBlock.FACING, l.face)
                            .with(DoorBlock.HALF, y == BASE + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                    set(x, y, z, d);
                } else if (!corner && y <= BASE + 2 && Math.floorMod(wallAlong, 3) == 1) {
                    set(x, y, z, Blocks.GLASS.getDefaultState());
                } else {
                    set(x, y, z, hs.wall);
                }
            }
            if (edgeA) for (int y = BASE + 4; y < roofY; y++) set(x, y, z, hs.wall);   // gable ends
            else set(x, BASE + 4, z, hs.wall);
        } else if (garage && a == gw) {
            // wall between the house and the garage, with a door through it (one step down into the garage)
            int cm = (c0 + c1) / 2;
            Direction toGarage = l.alongX ? Direction.EAST : Direction.SOUTH;
            for (int y = BASE; y <= BASE + 3; y++) {
                if (c == cm && y == BASE) continue;
                if (c == cm && y <= BASE + 2) {
                    set(x, y, z, hs.door.getDefaultState().with(DoorBlock.FACING, toGarage)
                            .with(DoorBlock.HALF, y == BASE + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER));
                } else {
                    set(x, y, z, hs.wall);
                }
            }
            set(x, BASE + 4, z, hs.floor);
        } else if (bay) {
            int cm = (c0 + c1) / 2;
            set(x, BASE + 4, z, a == g0 + 1 && c == cm ? Blocks.GLOWSTONE.getDefaultState() : hs.floor);
            if (a == g1 && c == back) container(x, BASE, z, "garage", true);
            if (a == g0 && c == back) {
                if (bunker(l)) {                                             // a hatch under the rug
                    set(x, GROUND, z, Blocks.SPRUCE_TRAPDOOR.getDefaultState().with(net.minecraft.block.TrapdoorBlock.HALF, net.minecraft.block.enums.BlockHalf.TOP)
                            .with(net.minecraft.block.TrapdoorBlock.FACING, Direction.NORTH));
                    set(x, BASE, z, Blocks.GRAY_CARPET.getDefaultState());
                } else set(x, BASE, z, Blocks.CRAFTING_TABLE.getDefaultState());
            }
        } else {
            set(x, BASE + 4, z, a == (a0 + a1) / 2 && c == (c0 + c1) / 2 ? Blocks.GLOWSTONE.getDefaultState() : hs.floor);
            if (a == a0 + 1 && c == c1 - 1) set(x, BASE + 1, z, Blocks.CRAFTING_TABLE.getDefaultState());
            if (a == a0 + 1 && c == c0 + 1) container(x, BASE + 1, z, "house", false);
            if (a == a0 + 2 && c == c1 - 1) container(x, BASE + 1, z, "house", false);
            if (garage) {
                if (a == gw - 1 && c == back) container(x, BASE + 1, z, "house", true);
            } else {
                if (a == a1 - 1 && c == c0 + 1) container(x, BASE + 1, z, "house", true);
                if (a == a1 - 1 && c == c1 - 1) set(x, BASE + 1, z, Blocks.BOOKSHELF.getDefaultState());
            }
        }
    }

    /** three houses in four have an attached garage */
    private boolean hasGarage(Lot l) {
        return hash(l.ox, l.oz, 61) % 4 != 0;
    }


    // ============================================================ wave 4: a bigger city

    private BlockState concrete(int i) {
        BlockState[] c = {Blocks.RED_CONCRETE.getDefaultState(), Blocks.BLUE_CONCRETE.getDefaultState(), Blocks.GREEN_CONCRETE.getDefaultState(),
                Blocks.ORANGE_CONCRETE.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.GRAY_CONCRETE.getDefaultState(),
                Blocks.BROWN_CONCRETE.getDefaultState(), Blocks.CYAN_CONCRETE.getDefaultState(), Blocks.YELLOW_CONCRETE.getDefaultState()};
        return c[Math.floorMod(i, c.length)];
    }

    /** street trees in planters along the sidewalks, between the lamps */
    boolean streetTreeAt(int x, int z) {
        int ux = Math.floorMod(x, CELL), uz = Math.floorMod(z, CELL);
        if (riverRow(Math.floorDiv(z, CELL)) && uz >= C) return false;
        boolean alongZ = uz >= C + 4 && uz < CELL - 4, alongX = ux >= C + 4 && ux < CELL - 4;
        boolean stop = (uz >= 48 && uz <= 56) || (ux >= 48 && ux <= 56);
        if (alongZ && ux == 1 && Math.floorMod(z, 18) == 9 && !(uz >= 48 && uz <= 56)) return true;
        if (alongZ && ux == C - 2 && Math.floorMod(z, 18) == 4) return true;
        if (alongX && uz == 1 && Math.floorMod(x, 18) == 9 && !(ux >= 48 && ux <= 56)) return true;
        return alongX && uz == C - 2 && Math.floorMod(x, 18) == 4 && !stop;
    }

    /** bus shelter: glass back wall and roof at u = 0, bench and sign at u = 1 (facing the road) */
    private void busStop(int x, int z, int u, int along, Direction road) {
        if (u == 0) {
            for (int y = BASE; y <= BASE + 2; y++) set(x, y, z, CLEAR);
        } else if (along == 4) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.BUS_STOP_SIGN, road));
            set(x, BASE + 1, z, POLE);
            return;
        } else if (along >= 1 && along <= 3) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, road));
        }
        if (along <= 3) set(x, BASE + 3, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
    }

    /** underneath a road bridge: deck girders, water, quay walls and piers */
    private void bridge(int x, int z, int ux, int lz) {
        if (lz < 5 || lz > 46) return;
        if (lz == 5 || lz == 46) {
            for (int y = BED; y <= GROUND - 1; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            return;
        }
        boolean pier = (lz == 16 || lz == 17 || lz == 34 || lz == 35) && (ux == 4 || ux == 5 || ux == 23 || ux == 24);
        set(x, BED, z, Blocks.GRAVEL.getDefaultState());
        for (int y = BED + 1; y <= GROUND - 2; y++) {
            set(x, y, z, pier ? Blocks.STONE_BRICKS.getDefaultState() : y <= WATER_TOP ? WATER : AIR);
        }
        set(x, GROUND - 1, z, ux % 4 == 0 ? Blocks.POLISHED_ANDESITE.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState());
    }

    /**
     * A river: quay walls with stone support pillars, promenades with benches, lamps and a row of wind turbines,
     * wooden docks on posts reaching out into the water (ladders down from the promenade), and 40 blocks of water.
     */
    private void river(int x, int z, int lx, int lz) {
        int dock = Math.floorMod(x, 48);
        boolean dockX = dock >= 20 && dock <= 22;
        boolean north = lz <= 25;
        if (lz <= 4 || lz >= 47) {                                          // promenade
            boolean grass = lz <= 1 || lz >= 50;
            set(x, GROUND, z, grass ? Blocks.GRASS_BLOCK.getDefaultState() : WALK);
            if (lz == 0 && Math.floorMod(x, 40) == 20) {
                set(x, BASE, z, CityBlocks.facing(CityBlocks.WIND_TURBINE, Direction.SOUTH));
                return;
            }
            if (lz == 4 || lz == 47) {
                if (Math.floorMod(x, 16) == 8) lamp(x, z, lz == 4 ? Direction.SOUTH : Direction.NORTH);
                else if (Math.floorMod(x, 16) == 2 || Math.floorMod(x, 16) == 3) set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, lz == 4 ? Direction.SOUTH : Direction.NORTH));
            }
            return;
        }
        if (lz == 5 || lz == 46) {                                           // quay wall with a railing (gap at the docks)
            for (int y = BED; y <= GROUND; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            if (!(dockX && dock == 21)) set(x, BASE, z, Blocks.STONE_BRICK_WALL.getDefaultState());
            return;
        }
        set(x, BED, z, rnd(x, z, 19) < 0.2f ? Blocks.CLAY.getDefaultState() : Blocks.GRAVEL.getDefaultState());
        for (int y = BED + 1; y <= GROUND; y++) set(x, y, z, y <= WATER_TOP ? WATER : AIR);
        // support pillars buttressing the quay walls
        int pil = Math.floorMod(x, 12);
        if ((lz == 6 || lz == 45) && (pil == 5 || pil == 6) && !dockX) {
            for (int y = BED + 1; y <= GROUND; y++) set(x, y, z, y == WATER_TOP + 1 ? Blocks.CHISELED_STONE_BRICKS.getDefaultState()
                    : y == GROUND ? Blocks.POLISHED_ANDESITE.getDefaultState() : churchStone(x, y, z));
            if (pil == 5) set(x, BASE, z, Blocks.LANTERN.getDefaultState());
            return;
        }
        // docks: a 3-wide plank walk on posts, 9 blocks out from each quay
        boolean onDock = dockX && (north ? lz <= 14 : lz >= 37);
        if (onDock) {
            int out = north ? lz - 6 : 45 - lz;                              // 0 at the quay
            boolean edge = dock == 20 || dock == 22;
            boolean rotten = OVERRUN && rnd(x, z, 175) < 0.15f;
            if (!rotten || out == 0) set(x, WATER_TOP + 1, z, Blocks.SPRUCE_PLANKS.getDefaultState());
            if (edge && out % 3 == 2) {
                for (int y = BED + 1; y <= WATER_TOP + 1; y++) set(x, y, z, Blocks.SPRUCE_LOG.getDefaultState());
                set(x, WATER_TOP + 2, z, Blocks.SPRUCE_FENCE.getDefaultState());
                if (out == 8) set(x, WATER_TOP + 3, z, Blocks.LANTERN.getDefaultState());
            }
            if (dock == 21 && out == 0) {                                    // ladder up the quay wall
                Direction toWall = north ? Direction.SOUTH : Direction.NORTH;
                for (int y = WATER_TOP + 2; y <= GROUND; y++) set(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, toWall));
            }
            return;
        }
        if (lz == 6 && rnd(x, 0, 20) < 0.06f) set(x, WATER_TOP + 1, z, Blocks.LILY_PAD.getDefaultState());
    }

    /** with Outbreak, some bridges have collapsed into the river */
    private boolean brokenBridge(int bx, int bz) {
        return OVERRUN && rnd(bx, bz, 170) < 0.4f;
    }

    /** the missing middle of a broken bridge: a ragged gap, rebar, slabs of deck slumped into the water, rubble, barricades */
    private void breakBridge(int x, int z, int ux, int lz) {
        int jag0 = hash(x, z >> 6, 176) % 3, jag1 = hash(x, z >> 6, 177) % 3;
        int g0 = 19 + jag0, g1 = 31 - jag1;
        if (lz >= g0 && lz <= g1) {
            for (int y = GROUND - 1; y <= BASE + 8; y++) set(x, y, z, y <= WATER_TOP ? WATER : AIR);
            int fromEdge = Math.min(lz - g0, g1 - lz);
            if (fromEdge <= 3) {                                             // deck slumped down from each broken edge
                int y = GROUND - 2 - fromEdge * 3;
                if (y > BED && rnd(x, lz, 178) < 0.7f) set(x, y, z, fromEdge == 0 ? ASPHALT : Blocks.CRACKED_STONE_BRICKS.getDefaultState());
            }
            if (fromEdge == 0 && rnd(x, lz, 179) < 0.4f) set(x, GROUND - 1, z, Blocks.IRON_BARS.getDefaultState());   // rebar
            int pile = hash(x, lz, 180) % 4;
            for (int y = BED + 1; y <= BED + pile; y++)
                set(x, y, z, rnd(x, y * 7 + lz, 181) < 0.5f ? Blocks.COBBLESTONE.getDefaultState() : Blocks.CRACKED_STONE_BRICKS.getDefaultState());
            return;
        }
        if ((lz == g0 - 1 || lz == g1 + 1) && rnd(x, lz, 182) < 0.35f) set(x, BASE, z, Blocks.IRON_BARS.getDefaultState());
        if ((lz == g0 - 4 || lz == g1 + 4) && ux >= 4 && ux <= 24 && ux % 3 == 0)
            set(x, BASE, z, CityBlocks.facing(CityBlocks.ROADBLOCK, lz < 25 ? Direction.NORTH : Direction.SOUTH));
        if (lz == g0 - 2 && ux == 12) set(x, BASE, z, CityBlocks.SMOKE_SOURCE.getDefaultState());
    }

    private void plaza(int x, int z, int lx, int lz) {
        set(x, GROUND, z, (lx + lz) % 2 == 0 ? PLAZA : Blocks.POLISHED_DIORITE.getDefaultState());
        double dx = lx - 25.5, dz = lz - 25.5, ax = Math.abs(dx), az = Math.abs(dz);
        int pbx = Math.floorDiv(x, CELL), pbz = Math.floorDiv(z, CELL);
        if (rnd(pbx, pbz, 150) < 0.6f) {                                     // fountain square
            double rr = Math.sqrt(dx * dx + dz * dz);
            if (rr < 5.2) {
                fountain(x, z, rr);
                return;
            }
            if (rr < 6.2) return;
            if (Math.abs(rr - 9) < 0.5 && ((int) Math.toDegrees(Math.atan2(dz, dx)) + 360) % 45 < 8) {
                set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, dirTo(dx, dz)));
                return;
            }
            if (lx == 6 && lz == 45) { set(x, BASE, z, CityBlocks.facing(CityBlocks.VENDING_MACHINE, Direction.EAST)); return; }
        } else if (ax < 1 && az < 1) {                                       // obelisk
            for (int y = BASE; y < BASE + 22; y++) set(x, y, z, Blocks.QUARTZ_PILLAR.getDefaultState());
            set(x, BASE + 22, z, Blocks.GOLD_BLOCK.getDefaultState());
            return;
        }
        if (ax < 3 && az < 3) {
            set(x, BASE, z, Blocks.POLISHED_GRANITE.getDefaultState());
            if (ax < 2 && az < 2) set(x, BASE + 1, z, Blocks.POLISHED_GRANITE.getDefaultState());
            return;
        }
        if (lx % 10 == 5 && lz % 10 == 5) {                                   // planters
            set(x, BASE, z, Blocks.MOSS_BLOCK.getDefaultState());
            set(x, BASE + 1, z, Blocks.FLOWERING_AZALEA.getDefaultState());
            return;
        }
        if ((lx == 18 || lx == 33) && lz % 4 == 1 && lz > 10 && lz < 42) {
            set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, lx == 18 ? Direction.WEST : Direction.EAST));
            return;
        }
        if ((lx == 10 || lx == 41) && (lz == 10 || lz == 41)) lamp(x, z, lx == 10 ? Direction.EAST : Direction.WEST);
    }

    private void hospital(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 4 && lx <= 47 && lz >= 10 && lz <= 41) {
            loot = "hospital";
            building(x, z, ox + 4, oz + 10, ox + 47, oz + 41, 9, STYLES[15], Direction.NORTH, Roof.HELIPAD, 0, 0);
            if (z == oz + 10) {                                              // big red cross on the front
                double cxl = x - (ox + 25.5);
                for (int y = BASE + 22; y <= BASE + 32; y++) {
                    double dy = y - (BASE + 27);
                    if ((Math.abs(cxl) <= 1.5 && Math.abs(dy) <= 5) || (Math.abs(dy) <= 1 && Math.abs(cxl) <= 5.5)) set(x, y, z, Blocks.RED_CONCRETE.getDefaultState());
                }
            }
            return;
        }
        set(x, GROUND, z, ASPHALT);
        if (lz >= 4 && lz <= 9 && lx >= 19 && lx <= 32) {                    // entrance canopy
            set(x, BASE + 4, z, Blocks.WHITE_CONCRETE.getDefaultState());
            if ((lx == 19 || lx == 32) && lz == 4) for (int y = BASE; y < BASE + 4; y++) set(x, y, z, Blocks.WHITE_CONCRETE.getDefaultState());
            if (lz == 4 && lx >= 22 && lx <= 29) set(x, BASE + 3, z, Blocks.RED_CONCRETE.getDefaultState());
        }
        if (lz == 6 && (lx == 6 || lx == 10 || lx == 14 || lx == 37 || lx == 41 || lx == 45)) set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z));
    }

    private void fireStation(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 8 && lx <= 43 && lz >= 14 && lz <= 37) {
            loot = "fire_station";
            building(x, z, ox + 8, oz + 14, ox + 43, oz + 37, 2, STYLES[17], Direction.NORTH, Roof.PLAIN, 0, 0);
            if (lz == 14) {
                int b = lx - 11;
                boolean bay = b >= 0 && b < 30 && b % 10 < 6;
                if (bay) for (int y = BASE + 1; y <= BASE + 4; y++) set(x, y, z, AIR);      // garage doors (open)
                if (bay) set(x, BASE + 5, z, Blocks.RED_CONCRETE.getDefaultState());
                if (lx == 25 || lx == 26) set(x, BASE + 7, z, Blocks.RED_CONCRETE.getDefaultState());
            }
            return;
        }
        set(x, GROUND, z, lz < 14 && lx >= 9 && lx <= 42 ? Blocks.SMOOTH_STONE.getDefaultState() : ASPHALT);
        if (lx == 6 && lz == 6) {                                            // drill tower
            for (int y = BASE; y < BASE + 18; y++) set(x, y, z, Blocks.RED_NETHER_BRICKS.getDefaultState());
            set(x, BASE + 18, z, Blocks.BELL.getDefaultState());
        }
    }

    // ------------------------------------------------------------- roadside places

    /** car-park lines and the odd lamp on an asphalt lot */
    private void carLot(int x, int z, int lx, int lz, int z0, int z1) {
        set(x, GROUND, z, ASPHALT);
        if (lz >= z0 && lz <= z1 && lx % 4 == 0 && lx >= 3 && lx <= 48) set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z));
    }

    /** a tall roadside sign: pole, a coloured box and a lit panel */
    private void roadSign(int x, int z, BlockState colour) {
        for (int y = BASE; y < BASE + 7; y++) set(x, y, z, POLE);
        set(x, BASE + 7, z, colour);
        set(x, BASE + 8, z, Blocks.OCHRE_FROGLIGHT.getDefaultState());
        set(x, BASE + 9, z, colour);
    }

    private void diner(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 12 && lx <= 39 && lz >= 18 && lz <= 33) {
            loot = "diner";
            building(x, z, ox + 12, oz + 18, ox + 39, oz + 33, 1, STYLES[19], Direction.NORTH, false);
            boolean wall = lx == 12 || lx == 39 || lz == 18 || lz == 33;
            if (wall) {
                set(x, BASE + 3, z, Blocks.RED_CONCRETE.getDefaultState());                        // the chrome-and-red band
                set(x, BASE + 4, z, Blocks.WHITE_CONCRETE.getDefaultState());
            }
            if (lz == 18 && lx >= 22 && lx <= 29) set(x, BASE + 5, z, Blocks.REDSTONE_LAMP.getDefaultState().with(net.minecraft.state.property.Properties.LIT, true));
            return;
        }
        carLot(x, z, lx, lz, 3, 11);
        if (lx == 6 && lz == 14) roadSign(x, z, Blocks.RED_CONCRETE.getDefaultState());
        if (lz >= 36 && lz <= 46 && lx >= 12 && lx <= 39) {                                         // back patio
            set(x, GROUND, z, Blocks.SMOOTH_STONE.getDefaultState());
            if (lz == 41 && lx % 6 == 2) set(x, BASE, z, f(CityBlocks.PICNIC_TABLE, Direction.NORTH));
            if (lz == 46 && lx % 5 == 0) set(x, BASE, z, f(CityBlocks.TRASH_BIN, Direction.NORTH));
        }
    }

    private void motel(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 6 && lx <= 45 && lz >= 30 && lz <= 43) {
            loot = "motel";
            building(x, z, ox + 6, oz + 30, ox + 45, oz + 43, 2, STYLES[20], Direction.NORTH, false);
            if (lz == 30 && (lx - 6) % 6 == 3) {                                                  // a door to every room, both floors
                for (int y : new int[]{BASE + 1, BASE + 2, BASE + 5, BASE + 6}) set(x, y, z, AIR);
            }
            return;
        }
        carLot(x, z, lx, lz, 4, 12);
        // the upstairs walkway with a railing, and the stairs up at the west end
        if (lz >= 28 && lz <= 29 && lx >= 6 && lx <= 45) {
            set(x, BASE + 4, z, Blocks.SMOOTH_STONE.getDefaultState());
            if (lz == 28) set(x, BASE + 5, z, Blocks.SPRUCE_FENCE.getDefaultState());
            if (lz == 28 && lx % 8 == 6) for (int y = BASE; y < BASE + 4; y++) set(x, y, z, Blocks.SPRUCE_FENCE.getDefaultState());   // posts
        }
        if (lz >= 28 && lz <= 29 && lx >= 1 && lx <= 5) {
            int y = BASE + (lx - 1);
            set(x, y, z, Blocks.STONE_STAIRS.getDefaultState().with(net.minecraft.block.StairsBlock.FACING, Direction.EAST));
            for (int yy = BASE; yy < y; yy++) set(x, yy, z, Blocks.SMOOTH_STONE.getDefaultState());
        }
        if (lx == 47 && lz == 14) roadSign(x, z, Blocks.LIGHT_BLUE_CONCRETE.getDefaultState());
        if (lz >= 16 && lz <= 24 && lx >= 30 && lx <= 44) {                                          // the pool
            boolean rim = lz == 16 || lz == 24 || lx == 30 || lx == 44;
            set(x, GROUND, z, rim ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.WATER.getDefaultState());
            if (!rim) set(x, GROUND - 1, z, Blocks.LIGHT_BLUE_CONCRETE.getDefaultState());
        }
    }

    private void dealership(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 26 && lx <= 47 && lz >= 10 && lz <= 29) {
            loot = "dealership";
            building(x, z, ox + 26, oz + 10, ox + 47, oz + 29, 1, STYLES[21], Direction.NORTH, false);
            if (lz == 10 && lx >= 33 && lx <= 40) for (int y = BASE + 1; y <= BASE + 3; y++) set(x, y, z, AIR);   // big showroom doors
            return;
        }
        set(x, GROUND, z, ASPHALT);
        if (lx >= 3 && lx <= 22 && lz >= 3 && lz <= 46 && lx % 5 == 2 && Math.floorMod(lz, 6) != 0) set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z));
        if (lz == 1 && lx % 6 == 3 && lx <= 24) {                                                    // bunting poles
            for (int y = BASE; y < BASE + 5; y++) set(x, y, z, POLE);
            set(x, BASE + 5, z, (lx / 6) % 2 == 0 ? Blocks.RED_WOOL.getDefaultState() : Blocks.BLUE_WOOL.getDefaultState());
        }
        if (lx == 49 && lz == 3) roadSign(x, z, Blocks.BLUE_CONCRETE.getDefaultState());
    }

    private void mechanic(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 8 && lx <= 39 && lz >= 16 && lz <= 35) {
            loot = "mechanic";
            building(x, z, ox + 8, oz + 16, ox + 39, oz + 35, 1, STYLES[23], Direction.NORTH, false);
            if (lz == 16) {
                int b = lx - 10;
                boolean bay = b >= 0 && b < 28 && b % 9 < 6;                                         // three open service bays
                if (bay) for (int y = BASE + 1; y <= BASE + 3; y++) set(x, y, z, com.nothatcher.vehicleforge.block.RoadBlocks.GARAGE_DOOR.getDefaultState()
                        .with(net.minecraft.block.HorizontalFacingBlock.FACING, Direction.NORTH).with(com.nothatcher.vehicleforge.block.RoadBlocks.GarageDoor.OPEN, true));
                if (bay) set(x, BASE + 4, z, Blocks.YELLOW_CONCRETE.getDefaultState());
            }
            if (lz == 24 && (lx == 12 || lx == 16 || lx == 21 || lx == 25)) set(x, BASE + 1, z, Blocks.IRON_BLOCK.getDefaultState());  // lift posts
            return;
        }
        set(x, GROUND, z, ASPHALT);
        if (lz >= 40 && lz <= 48 && lx >= 4 && lx <= 46 && (lx + lz) % 7 == 0) {                     // tyre stacks
            set(x, BASE, z, Blocks.BLACK_CONCRETE.getDefaultState());
            if ((lx * 3 + lz) % 2 == 0) set(x, BASE + 1, z, Blocks.BLACK_CONCRETE.getDefaultState());
        }
        if (lx == 4 && lz == 12) roadSign(x, z, Blocks.YELLOW_CONCRETE.getDefaultState());
        if (lx == 44 && lz == 20) {                                                                    // oil drums
            set(x, BASE, z, Blocks.BARREL.getDefaultState());
            set(x, BASE, z + 1, Blocks.BARREL.getDefaultState());
        }
    }

    private void bank(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 10 && lx <= 41 && lz >= 14 && lz <= 35) {
            loot = "bank";
            building(x, z, ox + 10, oz + 14, ox + 41, oz + 35, 2, STYLES[22], Direction.NORTH, false);
            if (lz == 33 && lx >= 12 && lx <= 39 && lx % 3 == 0) container(x, BASE + 1, z, "bank_vault", true);   // safe deposit chests
            return;
        }
        set(x, GROUND, z, lz >= 8 && lz <= 13 && lx >= 10 && lx <= 41 ? Blocks.SMOOTH_QUARTZ.getDefaultState() : ASPHALT);
        if (lz >= 9 && lz <= 13 && lx >= 10 && lx <= 41) {                                            // columned portico and steps
            if (lz == 9) set(x, BASE, z, Blocks.QUARTZ_STAIRS.getDefaultState().with(net.minecraft.block.StairsBlock.FACING, Direction.SOUTH));
            if (lz == 10 && lx % 4 == 2) for (int y = BASE; y < BASE + 8; y++) set(x, y, z, Blocks.QUARTZ_PILLAR.getDefaultState());
            set(x, BASE + 8, z, Blocks.SMOOTH_QUARTZ.getDefaultState());
            if (lz == 9) set(x, BASE + 9, z, Blocks.QUARTZ_SLAB.getDefaultState());
        }
        if (lz >= 3 && lz <= 6 && lx % 4 == 0 && lx > 2 && lx < 50) set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z));
    }

    /** a small store with its own stock, a car park and a tall sign; the gun store has bars on the windows */
    private void store(int x, int z, int bx, int bz, int lx, int lz, String kind, Style style, BlockState signColour, boolean bars) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 14 && lx <= 37 && lz >= 16 && lz <= 33) {
            loot = kind;
            building(x, z, ox + 14, oz + 16, ox + 37, oz + 33, 1, style, Direction.NORTH, false);
            if (lz == 16 && lx > 14 && lx < 37) set(x, BASE + 4, z, signColour);                           // fascia
            return;
        }
        carLot(x, z, lx, lz, 3, 11);
        if (bars && lz == 15 && lx > 14 && lx < 37 && (lx < 25 || lx > 26))
            for (int y = BASE + 1; y <= BASE + 3; y++) set(x, y, z, Blocks.IRON_BARS.getDefaultState());
        if (lx == 6 && lz == 14) roadSign(x, z, signColour);
        if (kind.equals("hardware") && lz >= 36 && lz <= 46 && lx >= 14 && lx <= 37) {                     // lumber yard out back
            set(x, GROUND, z, Blocks.GRAVEL.getDefaultState());
            if (lz % 4 == 0 && lx % 6 < 4) {
                set(x, BASE, z, Blocks.STRIPPED_OAK_LOG.getDefaultState().with(net.minecraft.state.property.Properties.AXIS, Direction.Axis.X));
                if (lx % 6 < 2) set(x, BASE + 1, z, Blocks.OAK_PLANKS.getDefaultState());
            }
        }
    }

    private void prison(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        set(x, GROUND, z, Blocks.GRAVEL.getDefaultState());
        boolean corner = (lx <= 3 || lx >= 48) && (lz <= 3 || lz >= 48);
        if (corner) {                                                                                      // guard towers
            boolean core = (lx == 2 || lx == 49) && (lz == 2 || lz == 49);
            for (int y = BASE; y < BASE + 9; y++) set(x, y, z, core ? Blocks.STONE_BRICKS.getDefaultState() : y == BASE + 8 ? Blocks.STONE_BRICK_SLAB.getDefaultState() : AIR);
            if (!core) set(x, BASE + 7, z, Blocks.STONE_BRICKS.getDefaultState());
            if (core) {
                set(x, BASE + 9, z, Blocks.SEA_LANTERN.getDefaultState());
                set(x, BASE + 10, z, Blocks.STONE_BRICK_SLAB.getDefaultState());
            }
            return;
        }
        if (lx == 1 || lx == 50 || lz == 1 || lz == 50) {                                               // the wall, with a gate
            boolean gate = lz == 1 && lx >= 22 && lx <= 29;
            if (!gate) {
                for (int y = BASE; y < BASE + 6; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
                set(x, BASE + 6, z, Blocks.IRON_BARS.getDefaultState());
            } else set(x, GROUND, z, ASPHALT);
            return;
        }
        if (lx >= 8 && lx <= 43 && lz >= 14 && lz <= 37) {
            loot = "prison";
            building(x, z, ox + 8, oz + 14, ox + 43, oz + 37, 2, STYLES[3], Direction.NORTH, false);
            return;
        }
        if (lz >= 2 && lz <= 12 && lx >= 20 && lx <= 31) set(x, GROUND, z, ASPHALT);                    // drive from the gate
        if (lz >= 2 && lz <= 11 && lx >= 4 && lx <= 47 && (lx < 20 || lx > 31)) set(x, GROUND, z, ASPHALT);
        if (lz >= 40 && lz <= 48 && lx >= 6 && lx <= 45) {                                              // the yard: a court and benches
            boolean line = lz == 40 || lz == 48 || lx == 6 || lx == 45 || lx == 25;
            set(x, GROUND, z, line ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.SMOOTH_STONE.getDefaultState());
            if ((lx == 8 || lx == 43) && lz == 44) {
                for (int y = BASE; y < BASE + 3; y++) set(x, y, z, Blocks.IRON_BARS.getDefaultState());
                set(x, BASE + 3, z, Blocks.WHITE_CONCRETE.getDefaultState());
            }
        }
        if (lz == 39 && lx % 7 == 3 && lx > 6 && lx < 45) set(x, BASE, z, f(CityBlocks.BENCH, Direction.SOUTH));
        if ((lx == 5 || lx == 46) && lz % 10 == 5) lamp(x, z, lx == 5 ? Direction.EAST : Direction.WEST);
    }

    private static final int[] TRAILER_X = {4, 20, 36}, TRAILER_Z = {8, 23, 38};

    private void trailerPark(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        for (int tx : TRAILER_X) {
            for (int tz : TRAILER_Z) {
                if (lx >= tx && lx <= tx + 10 && lz >= tz && lz <= tz + 5) {
                    if (rnd(bx * 5 + tx, bz * 5 + tz, 88) < 0.15f) {                                     // an empty pad
                        set(x, GROUND, z, Blocks.GRAVEL.getDefaultState());
                        return;
                    }
                    loot = "trailer";
                    set(x, GROUND, z, Blocks.GRAVEL.getDefaultState());
                    building(x, z, ox + tx, oz + tz, ox + tx + 10, oz + tz + 5, 1, STYLES[24], Direction.NORTH, false);
                    return;
                }
                if (lx == tx + 12 && lz == tz + 2) set(x, BASE, z, Blocks.CAMPFIRE.getDefaultState().with(net.minecraft.block.CampfireBlock.LIT, false));
                if (lx == tx + 12 && lz == tz + 4) set(x, BASE, z, f(CityBlocks.PICNIC_TABLE, Direction.NORTH));
                if (lx == tx + 13 && lz == tz) set(x, BASE, z, f(CityBlocks.MAILBOX, Direction.NORTH));
            }
        }
        int m = Math.floorMod(lz, 15);
        set(x, GROUND, z, m >= 2 && m <= 5 ? Blocks.GRAVEL.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
        if (m < 2 || m > 5) {
            float f = rnd(x, z, 89);
            if (f < 0.1f) set(x, BASE, z, Blocks.GRASS.getDefaultState());
        }
        if ((lx == 0 || lx == 51) && lz % 3 != 0) set(x, BASE, z, Blocks.OAK_FENCE.getDefaultState());
    }

    private void police(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 6 && lx <= 45 && lz >= 16 && lz <= 41) {
            loot = "police";
            building(x, z, ox + 6, oz + 16, ox + 45, oz + 41, 3, STYLES[16], Direction.NORTH, Roof.PLAIN, 0, 0);
            if (z == oz + 16 && x >= ox + 22 && x <= ox + 29) set(x, BASE + 5, z, Blocks.BLUE_CONCRETE.getDefaultState());
            return;
        }
        set(x, GROUND, z, ASPHALT);
        if (lz >= 3 && lz <= 8 && lx % 4 == 0 && lx > 2 && lx < 50) set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_WHITE, Direction.Axis.Z));
        if (lx == 25 && lz == 12) {                                         // flag pole
            for (int y = BASE; y < BASE + 9; y++) set(x, y, z, POLE);
            set(x, BASE + 8, z, Blocks.BLUE_WOOL.getDefaultState());
        }
    }

    private void school(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 4 && lx <= 31 && lz >= 6 && lz <= 29) {
            loot = "school";
            building(x, z, ox + 4, oz + 6, ox + 31, oz + 29, 3, STYLES[4], Direction.NORTH, Roof.PLAIN, 0, 0);
            return;
        }
        if (lz >= 34 && lz <= 50 && lx >= 3 && lx <= 48) {                 // sports field with lines and goals
            boolean line = lz == 34 || lz == 50 || lx == 3 || lx == 48 || lx == 25;
            double ddx = lx - 25.5, ddz = lz - 42;
            if (Math.abs(Math.sqrt(ddx * ddx + ddz * ddz) - 4) < 0.5) line = true;
            if (line) set(x, GROUND, z, Blocks.WHITE_CONCRETE.getDefaultState());
            if ((lx == 4 || lx == 47) && lz >= 40 && lz <= 44) {
                set(x, BASE, z, Blocks.WHITE_CONCRETE.getDefaultState());
                set(x, BASE + 1, z, Blocks.WHITE_CONCRETE.getDefaultState());
                if (lz == 40 || lz == 44) set(x, BASE + 2, z, Blocks.WHITE_CONCRETE.getDefaultState());
                else set(x, BASE + 2, z, Blocks.WHITE_CONCRETE.getDefaultState());
            }
            return;
        }
        if (lx >= 34) {
            set(x, GROUND, z, ASPHALT);
            if (lx == 38 && lz >= 6 && lz <= 26 && lz % 6 == 0) set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_YELLOW, Direction.Axis.X));
        }
    }

    /** football stadium: field, rising stands in two team colours, an outer wall with gates, floodlights */
    private void stadium(int x, int z, int lx, int lz) {
        double dx = lx - 25.5, dz = lz - 25.5;
        double a = 13, bb = 10, e = Math.sqrt((dx / a) * (dx / a) + (dz / bb) * (dz / bb));
        if (e < 1) {
            boolean line = Math.abs(dx) < 0.5 || Math.abs(Math.sqrt(dx * dx + dz * dz) - 3.5) < 0.5 || e > 0.95;
            set(x, GROUND, z, line ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
            if (Math.abs(Math.abs(dx) - 11.5) < 0.6 && Math.abs(dz) < 2.5) {
                set(x, BASE, z, Blocks.WHITE_CONCRETE.getDefaultState());
                set(x, BASE + 1, z, Blocks.WHITE_CONCRETE.getDefaultState());
                set(x, BASE + 2, z, Blocks.WHITE_CONCRETE.getDefaultState());
            }
            return;
        }
        boolean gate = Math.abs(dx) < 2 || Math.abs(dz) < 2;
        if (e < 1.75) {
            int h = (int) ((e - 1) / 0.75 * 15) + 1;
            double ang = Math.toDegrees(Math.atan2(dz, dx));
            boolean aisle = Math.abs(((ang % 45) + 45) % 45 - 22.5) > 21;
            set(x, GROUND, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
            if (gate && e < 1.2) return;
            for (int y = BASE; y < BASE + h; y++) set(x, y, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
            set(x, BASE + h, z, aisle ? Blocks.SMOOTH_STONE_SLAB.getDefaultState() : (dx < 0 ? Blocks.BLUE_CONCRETE : Blocks.RED_CONCRETE).getDefaultState());
            return;
        }
        if (e < 1.85) {
            set(x, GROUND, z, Blocks.WHITE_CONCRETE.getDefaultState());
            for (int y = BASE; y < BASE + 19; y++) {
                if (gate && y < BASE + 4) continue;
                set(x, y, z, (y - BASE) % 5 == 2 && !gate ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
            }
            return;
        }
        set(x, GROUND, z, (lx + lz) % 2 == 0 ? PLAZA : Blocks.SMOOTH_STONE.getDefaultState());
        if ((lx == 3 || lx == 48) && (lz == 3 || lz == 48)) {                // floodlights
            for (int y = BASE; y < BASE + 26; y++) set(x, y, z, POLE);
            for (int y = BASE + 26; y < BASE + 28; y++) set(x, y, z, LIGHT);
        }
    }

    /** a terrace of narrow three/four-storey houses on the north and south sides, gardens in between */
    private void rowhouses(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        boolean north = lz >= 2 && lz <= 13, south = lz >= 38 && lz <= 49;
        if ((north || south) && lx < 48) {
            int i = lx / 8;
            int hx0 = ox + i * 8, hx1 = hx0 + 7;
            int id = hash(bx * 8 + i, bz * 2 + (north ? 0 : 1), 22);
            Style st = STYLES[ROW_STYLES[id % ROW_STYLES.length]];
            int floors = 3 + (id >>> 4) % 2;
            Roof roof = (id >>> 6) % 5 == 0 ? Roof.WATER_TOWER : (id >>> 6) % 5 == 1 ? Roof.GARDEN : Roof.PLAIN;
            loot = "house";
            building(x, z, hx0, north ? oz + 2 : oz + 38, hx1, north ? oz + 13 : oz + 49, floors, st, north ? Direction.NORTH : Direction.SOUTH, roof, 0, 0);
            return;
        }
        if (lz < 2 || lz > 49) set(x, GROUND, z, WALK);
        else if (lx >= 48) set(x, GROUND, z, Blocks.GRAVEL.getDefaultState());
        else if (lx % 8 == 0 && rnd(x, z, 23) < 0.9f) set(x, BASE, z, Blocks.SPRUCE_FENCE.getDefaultState());
    }

    // ------------------------------------------------------- industrial

    private void warehouse(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        int x0 = ox + 3, x1 = ox + 48, z0 = oz + 10, z1 = oz + 44, top = BASE + 10;
        if (x < x0 || x > x1 || z < z0 || z > z1) {
            set(x, GROUND, z, ASPHALT);
            if (lz >= 3 && lz <= 8 && lx % 9 == 1) set(x, GROUND, z, CityBlocks.axis(CityBlocks.LINE_YELLOW, Direction.Axis.Z));
            return;
        }
        set(x, GROUND, z, Blocks.SMOOTH_STONE.getDefaultState());
        boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
        if (edge) {
            int along = (x == x0 || x == x1) ? z : x;
            boolean dock = z == z0 && Math.floorMod(x - x0, 9) >= 2 && Math.floorMod(x - x0, 9) <= 6 && x > x0 + 1 && x < x1 - 1;
            for (int y = BASE; y < top; y++) {
                int rel = y - BASE;
                if (dock && rel < 5) continue;
                BlockState wall = along % 2 == 0 ? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
                if (rel == 7 && along % 3 != 0) wall = Blocks.GLASS.getDefaultState();
                if (dock && rel == 5) wall = Blocks.YELLOW_CONCRETE.getDefaultState();
                set(x, y, z, wall);
            }
            set(x, top, z, Blocks.GRAY_CONCRETE.getDefaultState());
            return;
        }
        boolean sky = Math.floorMod(x, 6) < 2 && Math.floorMod(z, 8) < 4;
        set(x, top, z, sky ? Blocks.GLASS.getDefaultState() : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
        if (Math.floorMod(x, 13) == 6 && Math.floorMod(z, 9) == 4) set(x, top - 1, z, LIGHT);
        // racks of crates and barrels
        int rx = x - x0, rz = z - z0;
        if (rz >= 8 && rz % 7 < 2 && rx % 12 > 1 && rx % 12 < 10) {
            int h = 1 + hash(x, z, 24) % 4;
            for (int y = BASE; y < BASE + h; y++) {
                int k = hash(x, y * 31 + z, 25) % 9;
                if (k == 0) container(x, y, z, "warehouse", false);
                else set(x, y, z, (k < 3 ? Blocks.BARREL : Blocks.OAK_PLANKS).getDefaultState());
            }
        }
    }

    private void factory(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        // smokestacks
        for (int[] st : new int[][]{{41, 10}, {41, 26}}) {
            double dx = lx - st[0], dz = lz - st[1], d2 = dx * dx + dz * dz;
            if (d2 <= 5) {
                set(x, GROUND, z, Blocks.BRICKS.getDefaultState());
                int h = 44;
                for (int y = BASE; y < BASE + h; y++) {
                    boolean band = y >= BASE + h - 9 && ((y - BASE) / 3) % 2 == 0;
                    set(x, y, z, d2 <= 1.0 && y > BASE + 1 ? AIR : band ? Blocks.RED_CONCRETE.getDefaultState() : Blocks.BRICKS.getDefaultState());
                }
                if (dx == 0 && dz == 0) {
                    set(x, BASE + 1, z, Blocks.HAY_BLOCK.getDefaultState());
                    set(x, BASE + 2, z, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.SIGNAL_FIRE, true));
                }
                return;
            }
        }
        // storage tanks
        for (int[] tk : new int[][]{{42, 42}}) {
            double dx = lx - tk[0], dz = lz - tk[1], d2 = dx * dx + dz * dz;
            if (d2 <= 16) {
                set(x, GROUND, z, Blocks.SMOOTH_STONE.getDefaultState());
                for (int y = BASE; y < BASE + 12; y++) set(x, y, z, d2 > 9 ? Blocks.IRON_BLOCK.getDefaultState() : AIR);
                set(x, BASE + 12, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
                return;
            }
        }
        int x0 = ox + 3, x1 = ox + 34, z0 = oz + 3, z1 = oz + 48, top = BASE + 14;
        if (x < x0 || x > x1 || z < z0 || z > z1) {
            set(x, GROUND, z, rnd(x, z, 26) < 0.3f ? Blocks.GRAVEL.getDefaultState() : ASPHALT);
            return;
        }
        set(x, GROUND, z, Blocks.SMOOTH_STONE.getDefaultState());
        boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
        int saw = Math.floorMod(z - z0, 6);
        if (edge) {
            int along = (x == x0 || x == x1) ? z : x;
            for (int y = BASE; y < top; y++) {
                int rel = y - BASE;
                boolean door = z == z0 && rel < 5 && Math.abs(x - (x0 + x1) / 2) <= 2;
                if (door) continue;
                boolean win = rel >= 3 && rel <= 10 && along % 3 != 0 && rel != 6;
                set(x, y, z, win ? Blocks.GLASS.getDefaultState() : Blocks.BRICKS.getDefaultState());
            }
        } else if (Math.floorMod(x, 11) == 5 && Math.floorMod(z, 7) == 3) {
            for (int y = BASE; y < BASE + 3; y++) set(x, y, z, Blocks.IRON_BLOCK.getDefaultState());     // machines
            set(x, BASE + 3, z, Blocks.PISTON.getDefaultState());
        } else if (!edge && Math.floorMod(x, 11) == 2 && Math.floorMod(z, 13) == 6) {
            container(x, BASE, z, "warehouse", false);
        }
        // sawtooth roof with north-facing glazing
        if (saw == 4) {
            for (int y = top; y < top + 4; y++) set(x, y, z, Blocks.GLASS.getDefaultState());
            set(x, top + 4, z, Blocks.BRICKS.getDefaultState());
        } else {
            int h = saw < 4 ? saw + 1 : 0;
            for (int y = top; y <= top + h; y++) set(x, y, z, y == top + h ? Blocks.DEEPSLATE_TILES.getDefaultState() : Blocks.BRICKS.getDefaultState());
        }
        if (!edge && Math.floorMod(x, 9) == 4 && saw == 2) set(x, top - 1, z, LIGHT);
    }

    /** shipping container yard: stacks of 3x3x7 containers in many colours */
    private void containers(int x, int z, int bx, int bz, int lx, int lz) {
        set(x, GROUND, z, ASPHALT);
        if (lx < 2 || lx > 49 || lz < 3 || lz > 47) return;
        int i = (lx - 2) / 4, j = (lz - 3) / 9, cx = (lx - 2) % 4, cz = (lz - 3) % 9;
        if (cx == 3 || cz >= 7) {
            if (cx == 3 && cz == 4 && i % 3 == 1) lamp(x, z, Direction.NORTH);
            return;
        }
        int key = hash(bx * 16 + i, bz * 8 + j, 27);
        int stack = key % 4;
        for (int level = 0; level < stack; level++) {
            int colour = hash(bx * 16 + i, bz * 8 + j * 4 + level, 28);
            boolean end = cz == 0 || cz == 6;
            BlockState c = end ? Blocks.IRON_BLOCK.getDefaultState() : concrete(colour);
            for (int y = 0; y < 3; y++) {
                BlockState b = (!end && cx == 1 && y == 1) ? concrete(colour + 1) : c;    // stripe for detail
                if (!end && (cz % 2 == 0) && cx != 1) b = concrete(colour).getBlock() == Blocks.WHITE_CONCRETE ? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState() : c;
                set(x, BASE + level * 3 + y, z, b);
            }
        }
    }

    /** construction site: fenced with barriers, half-built concrete frame with scaffolding, a tower crane, dirt piles */
    private void construction(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        set(x, GROUND, z, rnd(x, z, 29) < 0.4f ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT.getDefaultState());
        // perimeter barriers with a gate on the north side
        if (lx == 0 || lx == 51 || lz == 0 || lz == 51) {
            boolean gate = lz == 0 && lx >= 20 && lx <= 30;
            if (!gate) set(x, BASE, z, CityBlocks.facing(CityBlocks.ROAD_BARRIER, (lx == 0 || lx == 51) ? Direction.WEST : Direction.NORTH));
            if (gate && (lx == 20 || lx == 30)) set(x, BASE, z, CityBlocks.facing(CityBlocks.TRAFFIC_CONE, Direction.NORTH));
            return;
        }
        // tower crane: lattice mast, cab, long jib with a counterweight, hook on a chain
        int mastX = 40, mastZ = 40, mastH = 44;
        boolean mast = (lx == mastX || lx == mastX + 1) && (lz == mastZ || lz == mastZ + 1);
        int jibY = BASE + mastH + 2;
        if (mast) {
            set(x, GROUND, z, Blocks.STONE.getDefaultState());
            for (int y = BASE; y < jibY; y++) set(x, y, z, (y - BASE) % 3 == 0 ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.IRON_BARS.getDefaultState());
            if (lx == mastX + 1 && lz == mastZ) {
                set(x, jibY - 1, z, Blocks.WHITE_CONCRETE.getDefaultState());
                set(x, jibY - 2, z, Blocks.GLASS.getDefaultState());
            }
            for (int y = jibY + 1; y <= jibY + 5; y++) if (lx == mastX && lz == mastZ) set(x, y, z, Blocks.IRON_BARS.getDefaultState());
        }
        if ((lz == mastZ || lz == mastZ + 1) && lx >= 6 && lx <= 49) {
            set(x, jibY, z, lx % 2 == 0 ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.IRON_BARS.getDefaultState());
            if (lx >= 46) {
                set(x, jibY - 1, z, Blocks.GRAY_CONCRETE.getDefaultState());
                set(x, jibY - 2, z, Blocks.GRAY_CONCRETE.getDefaultState());
            }
            if (lx == 16 && lz == mastZ) {
                for (int y = jibY - 1; y > jibY - 22; y--) set(x, y, z, Blocks.CHAIN.getDefaultState());
                set(x, jibY - 22, z, Blocks.YELLOW_CONCRETE.getDefaultState());
            }
        }
        if (mast) return;
        // half-built tower: concrete frame, finished floors, rebar on top, scaffolding up the front
        int x0 = ox + 6, x1 = ox + 33, z0 = oz + 6, z1 = oz + 33;
        int floorsBuilt = 3 + hash(bx, bz, 30) % 8;
        int top = BASE + floorsBuilt * 4;
        if (x >= x0 && x <= x1 && z >= z0 && z <= z1) {
            set(x, GROUND, z, Blocks.SMOOTH_STONE.getDefaultState());
            boolean column = (x - x0) % 6 == 0 && (z - z0) % 6 == 0 || (x == x1 && (z - z0) % 6 == 0) || (z == z1 && (x - x0) % 6 == 0);
            for (int y = BASE; y < top; y++) {
                int rel = y - BASE;
                if (column) set(x, y, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
                else if (rel % 4 == 0 && rel > 0) set(x, y, z, Blocks.SMOOTH_STONE.getDefaultState());
            }
            if (column) for (int y = top; y < top + 3; y++) set(x, y, z, Blocks.IRON_BARS.getDefaultState());
            else if (rnd(x, z, 31) < 0.04f) set(x, top + 1, z, Blocks.OAK_PLANKS.getDefaultState());
            return;
        }
        if (z == z0 - 1 && x >= x0 && x <= x1 && (x - x0) % 2 == 0) {
            for (int y = BASE; y < top + 1; y++) set(x, y, z, Blocks.SCAFFOLDING.getDefaultState().with(ScaffoldingBlock.DISTANCE, 0));
            return;
        }
        // dirt and gravel piles, a site office and a portable toilet
        double dd = Math.sqrt((lx - 43.0) * (lx - 43.0) + (lz - 10.0) * (lz - 10.0));
        if (dd < 6) for (int y = BASE; y < BASE + (int) (4 - dd * 0.7); y++) set(x, y, z, Blocks.DIRT.getDefaultState());
        double dg = Math.sqrt((lx - 10.0) * (lx - 10.0) + (lz - 44.0) * (lz - 44.0));
        if (dg < 5) for (int y = BASE; y < BASE + (int) (3 - dg * 0.6); y++) set(x, y, z, Blocks.GRAVEL.getDefaultState());
        if (lx == 42 && lz == 28) {
            container(x, BASE, z, "construction", true);
            for (int y = BASE + 1; y < BASE + 3; y++) set(x, y, z, AIR);
        } else if (lx >= 38 && lx <= 47 && lz >= 26 && lz <= 29) {
            boolean inside = lx > 38 && lx < 47 && lz > 26 && lz < 29;
            for (int y = BASE; y < BASE + 3; y++) set(x, y, z, inside ? AIR : (lz == 26 && y == BASE + 1 && lx % 3 == 0) ? Blocks.GLASS.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
            if (inside) set(x, BASE + 3, z, Blocks.WHITE_CONCRETE.getDefaultState());
        }
        if (lx == 36 && lz == 28) {
            set(x, BASE, z, Blocks.BLUE_CONCRETE.getDefaultState());
            set(x, BASE + 1, z, Blocks.BLUE_CONCRETE.getDefaultState());
            set(x, BASE + 2, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        }
    }

    // --------------------------------------------------------------- trees

    /** chance of a tree centred here (lawns and parks only, never on paths, roads or next to buildings) */
    private float treeDensity(int x, int z) {
        int ux = Math.floorMod(x, CELL), uz = Math.floorMod(z, CELL);
        if (ux < C + 2 || uz < C + 2 || ux > CELL - 3 || uz > CELL - 3) return 0;
        int bx = Math.floorDiv(x, CELL), bz = Math.floorDiv(z, CELL);
        int lx = ux - C, lz = uz - C;
        Kind k = kind(bx, bz);
        if (k == Kind.PARK || k == Kind.CHURCH || k == Kind.AIRPORT || k == Kind.STATION || k == Kind.FARM || k == Kind.FOREST
                || k == Kind.MEADOW || k == Kind.ORCHARD) return 0;              // these plant their own trees (or none)
        if (k == Kind.HOUSES) {
            if (lx >= 51 || lz >= 51) return 0;
            int li = lx / 17, lj = lz / 17, px = lx % 17, pz = lz % 17;
            if (li == 1 && lj == 1) return (px >= 3 && px <= 12 && pz >= 3 && pz <= 12) ? 0 : 0.6f;
            Lot l = lot(bx, bz, li, lj);
            if (x >= l.hx0 - 3 && x <= l.hx1 + 3 && z >= l.hz0 - 3 && z <= l.hz1 + 3) return 0;
            if (px >= 12 || pz >= 12 || px < 2 || pz < 2) return 0;
            return 0.35f;
        }
        if (k == Kind.RIVER) return lz <= 1 || lz >= 50 ? 0.55f : 0;
        if (k == Kind.ROWHOUSES) return lz >= 17 && lz <= 34 && lx < 46 && lx % 8 >= 2 && lx % 8 <= 5 ? 0.4f : 0;
        if (k == Kind.SCHOOL) return lx >= 34 && lz >= 30 ? 0 : (lx <= 2 || lz <= 3) && lz < 31 ? 0.3f : 0;
        return 0;
    }

    private void trees(int x, int z) {
        // street trees: tall trunks so buses fit under the canopy
        for (int tx = x - 2; tx <= x + 2; tx++) {
            for (int tz = z - 2; tz <= z + 2; tz++) {
                if (!streetTreeAt(tx, tz)) continue;
                int dx = x - tx, dz = z - tz, d2 = dx * dx + dz * dz;
                BlockState leaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
                if (d2 == 0) for (int y = BASE; y < BASE + 6; y++) set(x, y, z, Blocks.OAK_LOG.getDefaultState());
                for (int y = BASE + 4; y <= BASE + 7; y++) {
                    int layer = y - (BASE + 6);
                    boolean in = layer < 0 ? d2 > 0 && d2 <= 5 : layer == 0 ? d2 <= 2 : d2 <= 1;
                    if (in && isAir(x, y, z)) set(x, y, z, leaves);
                }
            }
        }
        for (int gx = Math.floorDiv(x - 2, 6); gx <= Math.floorDiv(x + 2, 6); gx++) {
            for (int gz = Math.floorDiv(z - 2, 6); gz <= Math.floorDiv(z + 2, 6); gz++) {
                int h = hash(gx, gz, 30);
                int tx = gx * 6 + 1 + h % 4, tz = gz * 6 + 1 + (h >>> 3) % 4;
                int dx = x - tx, dz = z - tz;
                if (Math.abs(dx) > 2 || Math.abs(dz) > 2) continue;
                if (rnd(gx, gz, 31) >= treeDensity(tx, tz)) continue;
                boolean birch = (h >>> 7) % 3 == 0;
                int height = 4 + (h >>> 9) % 2;
                BlockState log = (birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).getDefaultState();
                BlockState leaves = (birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).getDefaultState().with(LeavesBlock.PERSISTENT, true);
                int d2 = dx * dx + dz * dz;
                if (d2 == 0) for (int y = BASE; y < BASE + height; y++) set(x, y, z, log);
                for (int y = BASE + height - 2; y <= BASE + height + 1; y++) {
                    int layer = y - (BASE + height);
                    boolean in = layer < 0 ? d2 > 0 && d2 <= 5 : layer == 0 ? d2 <= 2 : d2 <= 1;
                    if (in && isAir(x, y, z)) set(x, y, z, leaves);
                }
            }
        }
    }

    // --------------------------------------------------------- parked cars

    /** Parked vehicles for every city cell this chunk touches (cells are 81 wide, so a chunk can span two). */
    void parkedCars(int x0, int z0) {
        airportEntities(x0, z0);
        for (int bx = Math.floorDiv(x0, CELL); bx <= Math.floorDiv(x0 + 15, CELL); bx++) {
            for (int bz = Math.floorDiv(z0, CELL); bz <= Math.floorDiv(z0 + 15, CELL); bz++) {
                parkedInCell(bx, bz, x0, z0);
            }
        }
    }

    private void parkAt(String id, double lx, double lz, double y, float yaw, double chance, int bx, int bz, int salt, int x0, int z0) {
        double x = bx * CELL + C + lx, z = bz * CELL + C + lz;
        if (!inChunk(x, z, x0, z0)) return;
        if (rnd(bx * 31 + salt, bz, 43) >= chance) return;
        spawnType(x, y, z, yaw, id, hash(bx, bz * 17 + salt, 44));
    }

    private void parkedInCell(int bx, int bz, int x0, int z0) {
        if (airportCell(bx, bz)) return;                                     // see airportEntities
        int bxs = bx * CELL, bzs = bz * CELL;
        if (countryCell(bx, bz)) {
            countryEntities(bx, bz, x0, z0);
            return;
        }
        switch (kind(bx, bz)) {
            case PARKING -> {
                for (int ri = 0; ri < ROWS.length; ri++) {
                    for (int s = 1; s <= 15; s++) {
                        double cx = bxs + C + 3 * s + 1.5, cz = bzs + C + ROWS[ri] + 0.5;
                        if (inChunk(cx, cz, x0, z0) && rnd(bxs + s, bzs + ri, 41) < 0.45f) spawn(cx, cz, ri == 0 ? 0f : 180f, hash(bxs + s, bzs + ri, 42));
                    }
                }
            }
            case HOUSES -> {
                for (int li = 0; li < 3; li++) {
                    for (int lj = 0; lj < 3; lj++) {
                        if (li == 1 && lj == 1) continue;
                        Lot l = lot(bx, bz, li, lj);
                        if (hasGarage(l)) {
                            boolean frontLow = l.face == Direction.NORTH || l.face == Direction.WEST;
                            double a = (l.alongX ? l.hx1 : l.hz1) - 2 + 0.5;
                            double cmid = (l.alongX ? (l.hz0 + l.hz1) : (l.hx0 + l.hx1)) / 2.0 + 0.5 + (frontLow ? -0.5 : 0.5);
                            double gx = l.alongX ? a : cmid, gz = l.alongX ? cmid : a;
                            float nose = switch (l.face) {      // parked nose-in
                                case NORTH -> 0f;
                                case SOUTH -> 180f;
                                case WEST -> -90f;
                                default -> 90f;
                            };
                            if (inChunk(gx, gz, x0, z0) && rnd(l.ox, l.oz, 62) < 0.7f) {
                                int h = hash(l.ox, l.oz, 63);
                                spawnType(gx, GROUND + 1, gz, nose, GARAGE_CARS[h % GARAGE_CARS.length], h, false);
                            }
                            continue;
                        }
                        double cx, cz;
                        float yaw;
                        if (l.alongX) {
                            cx = l.ox + 15.0;
                            cz = l.face == Direction.NORTH ? l.oz + 3.5 : l.oz + 12.5;
                            yaw = l.face == Direction.NORTH ? 180f : 0f;
                        } else {
                            cz = l.oz + 15.0;
                            cx = l.face == Direction.WEST ? l.ox + 3.5 : l.ox + 12.5;
                            yaw = l.face == Direction.WEST ? 90f : -90f;
                        }
                        if (inChunk(cx, cz, x0, z0) && rnd(l.ox, l.oz, 44) < 0.4f) spawn(cx, cz, yaw, hash(l.ox, l.oz, 45));
                    }
                }
            }
            case HOSPITAL -> {
                int i = 0;
                for (double lx : new double[]{8, 12, 39, 43}) parkAt("ambulance", lx, 6.5, BASE, 0f, 0.6, bx, bz, i++, x0, z0);
            }
            case FIRE -> {
                parkAt("fire_truck", 13.5, 19, BASE + 1, 180f, 0.95, bx, bz, 1, x0, z0);
                parkAt("fire_truck", 23.5, 19, BASE + 1, 180f, 0.6, bx, bz, 2, x0, z0);
                parkAt("ambulance", 33.5, 18, BASE + 1, 180f, 0.6, bx, bz, 3, x0, z0);
            }
            case POLICE -> {
                for (int k = 1; k <= 11; k++) parkAt("police_car", 4 * k + 2, 5.5, BASE, 0f, 0.45, bx, bz, k, x0, z0);
            }
            case SCHOOL -> {
                parkAt("school_bus", 41, 12, BASE, 180f, 0.9, bx, bz, 1, x0, z0);
                parkAt("school_bus", 45.5, 18, BASE, 180f, 0.5, bx, bz, 2, x0, z0);
            }
            case WAREHOUSE -> {
                for (int n = 0; n < 5; n++) parkAt(n % 2 == 0 ? "semi_truck" : "van", 7.5 + 9 * n, 5.5, BASE, 180f, 0.5, bx, bz, n, x0, z0);
                parkAt("forklift", 12, 22, BASE, 90f, 0.7, bx, bz, 7, x0, z0);
                parkAt("forklift", 34, 29, BASE, -90f, 0.5, bx, bz, 8, x0, z0);
            }
            case CONTAINERS -> {
                for (int j = 0; j < 5; j++) for (int i = 0; i < 4; i++)
                    parkAt(i % 2 == 0 ? "forklift" : "semi_truck", 8 + 12 * i, 3 + 9 * j + 7.5, BASE, -90f, 0.2, bx, bz, j * 4 + i, x0, z0);
            }
            case DINER -> {
                parkAt("food_truck", 45, 40, BASE, 0f, 0.7, bx, bz, 1, x0, z0);
                for (int k = 0; k < 10; k++) parkAt(PARKED[hash(bx, bz * 7 + k, 81) % PARKED.length], 5.5 + 4 * k, 7.5, BASE, 180f, 0.45, bx, bz, 10 + k, x0, z0);
            }
            case MOTEL -> {
                for (int k = 0; k < 10; k++) parkAt(k == 3 ? "rv" : PARKED[hash(bx, bz * 7 + k, 82) % PARKED.length], 5.5 + 4 * k, 8.5, BASE, 0f, 0.5, bx, bz, 10 + k, x0, z0);
            }
            case DEALERSHIP -> {
                String[] show = {"sports_car", "rally_car", "muscle_car", "vintage_roadster", "suv", "limousine"};
                int h = hash(bx, bz, 83);
                parkAt(show[h % show.length], 31, 18, BASE + 1, 135f, 0.95, bx, bz, 1, x0, z0);            // showroom
                parkAt(show[(h + 2) % show.length], 38, 22, BASE + 1, 200f, 0.95, bx, bz, 2, x0, z0);
                parkAt(show[(h + 4) % show.length], 43, 16, BASE + 1, 90f, 0.9, bx, bz, 3, x0, z0);
                String[] lotCars = {"sedan", "hatchback", "suv", "pickup_truck", "van", "taxi", "muscle_car", "classic_cruiser", "rally_car"};
                for (int col = 0; col < 4; col++) for (int row = 0; row < 7; row++)
                    parkAt(lotCars[hash(bx * 5 + col, bz * 9 + row, 84) % lotCars.length], 4.5 + 5 * col, 5 + 6 * row, BASE, 0f, 0.7, bx, bz, 20 + col * 7 + row, x0, z0);
            }
            case MECHANIC -> {
                parkAt("tow_truck", 46, 30, BASE, 0f, 0.9, bx, bz, 1, x0, z0);
                parkAt(PARKED[hash(bx, bz, 85) % PARKED.length], 13, 24, BASE + 1, 0f, 0.8, bx, bz, 2, x0, z0);   // on the lifts
                parkAt(PARKED[hash(bx, bz, 86) % PARKED.length], 22, 25, BASE + 1, 0f, 0.6, bx, bz, 3, x0, z0);
                for (int k = 0; k < 4; k++) parkAt(PARKED[hash(bx, bz * 3 + k, 87) % PARKED.length], 8 + 9 * k, 44, BASE, 90f, 0.5, bx, bz, 10 + k, x0, z0);
            }
            case PHARMACY, HARDWARE, GUN_STORE -> {
                for (int k = 0; k < 10; k++) parkAt(PARKED[hash(bx, bz * 7 + k, 90) % PARKED.length], 5.5 + 4 * k, 7.5, BASE, 180f, 0.4, bx, bz, 10 + k, x0, z0);
            }
            case PRISON -> {
                parkAt("prison_bus", 33, 7, BASE, 90f, 0.9, bx, bz, 1, x0, z0);
                for (int k = 0; k < 4; k++) parkAt("police_car", 6 + 4 * k, 6.5, BASE, 0f, 0.6, bx, bz, 2 + k, x0, z0);
                parkAt("military_truck", 40, 7, BASE, -90f, 0.25, bx, bz, 7, x0, z0);
            }
            case TRAILER_PARK -> {
                String[] rides = {"camper_van", "rv", "pickup_truck", "sedan", "classic_cruiser", "atv"};
                int k = 0;
                for (int tx : TRAILER_X) for (int tz : TRAILER_Z)
                    parkAt(rides[hash(bx * 5 + tx, bz * 5 + tz, 91) % rides.length], tx + 5, tz - 2.5, BASE, 90f, 0.6, bx, bz, 30 + k++, x0, z0);
            }
            case BANK -> {
                parkAt("armored_truck", 8, 3.5, BASE, 90f, 0.8, bx, bz, 1, x0, z0);
                parkAt("police_car", 40, 4.5, BASE, -90f, 0.3, bx, bz, 2, x0, z0);
            }
            case CONSTRUCTION -> {
                parkAt("excavator", 40, 20, BASE, 90f, 0.95, bx, bz, 1, x0, z0);
                parkAt("bulldozer", 22, 40, BASE, 180f, 0.85, bx, bz, 2, x0, z0);
                parkAt("dump_truck", 30, 44, BASE, 90f, 0.8, bx, bz, 3, x0, z0);
                parkAt("cement_mixer", 45, 34, BASE, 0f, 0.7, bx, bz, 4, x0, z0);
                parkAt("wheel_loader", 20, 47, BASE, 90f, 0.7, bx, bz, 5, x0, z0);
                parkAt("mobile_crane", 48.5, 21, BASE, 180f, 0.6, bx, bz, 6, x0, z0);
                parkAt("road_roller", 47, 46, BASE, 0f, 0.6, bx, bz, 7, x0, z0);
            }
            case RIVER -> {
                for (int i = 0; i < 4; i++) {
                    for (int side = 0; side < 2; side++) {
                        int salt = i * 2 + side;
                        String id = switch (hash(bx, bz * 9 + salt, 47) % 8) {
                            case 0 -> "yacht";
                            case 1, 2 -> "speedboat";
                            case 3 -> "houseboat";
                            case 4 -> "sailboat";
                            default -> "jet_ski";
                        };
                        parkAt(id, 8 + 12 * i, side == 0 ? 12 : 39, WATER_TOP + 0.5, side == 0 ? 90f : -90f, 0.35, bx, bz, salt, x0, z0);
                    }
                }
            }
            default -> { }
        }
        if (OVERRUN && kind(bx, bz) != Kind.RIVER) roadWrecks(bx, bz, x0, z0);
        // kerbside parking lanes, driving on the right (none on the bridges)
        for (int i = 0; i < 16; i++) {
            int z = z0 + i, x = x0 + i;
            int uz = Math.floorMod(z, CELL), ux = Math.floorMod(x, CELL);
            boolean bridge = riverRow(Math.floorDiv(z, CELL)) && uz >= C;
            if (!bridge && Math.floorDiv(z, CELL) == bz && uz >= C + 5 && uz <= CELL - 8 && Math.floorMod(z, 9) == 4) {
                double west = bxs + 4.0, east = bxs + 25.0;
                if (inChunk(west, z + 0.5, x0, z0) && rnd(bxs, z, 46) < 0.3f) spawn(west, z + 0.5, 0f, hash(bxs, z, 47));
                if (inChunk(east, z + 0.5, x0, z0) && rnd(bxs + 1, z, 46) < 0.3f) spawn(east, z + 0.5, 180f, hash(bxs + 1, z, 47));
            }
            if (Math.floorDiv(x, CELL) == bx && ux >= C + 5 && ux <= CELL - 8 && Math.floorMod(x, 9) == 4) {
                double north = bzs + 4.0, south = bzs + 25.0;
                if (inChunk(x + 0.5, north, x0, z0) && rnd(x, bzs, 48) < 0.3f) spawn(x + 0.5, north, 90f, hash(x, bzs, 49));
                if (inChunk(x + 0.5, south, x0, z0) && rnd(x, bzs + 1, 48) < 0.3f) spawn(x + 0.5, south, -90f, hash(x, bzs + 1, 49));
            }
        }
    }

    private static boolean inChunk(double x, double z, int x0, int z0) {
        return x >= x0 && x < x0 + 16 && z >= z0 && z < z0 + 16;
    }

    private void spawn(double x, double z, float yaw, int h) {
        spawnType(x, BASE, z, yaw, PARKED[h % PARKED.length], h, OVERRUN && (h >>> 13) % 100 < 35);
    }

    private void spawnType(double x, double y, double z, float yaw, String id, int h) {
        spawnType(x, y, z, yaw, id, h, OVERRUN && (h >>> 13) % 100 < 25);
    }

    /** abandoned and crashed cars across the roads, and the odd pile-up at a junction */
    private void roadWrecks(int bx, int bz, int x0, int z0) {
        int bxs = bx * CELL, bzs = bz * CELL;
        for (int i = 0; i < 4; i++) {
            if (rnd(bx * 7 + i, bz, 70) < 0.6f) {          // north-south road
                double along = bzs + C + 3 + rnd(bx * 7 + i, bz, 71) * (CELL - C - 8);
                double lane = bxs + 5 + rnd(bx, bz * 7 + i, 72) * 18;
                float yaw = (rnd(bx, bz * 7 + i, 73) < 0.5f ? 0f : 180f) + (rnd(bx, bz * 7 + i, 74) - 0.5f) * 130f;
                int h = hash(bx * 7 + i, bz, 75);
                if (inChunk(lane, along, x0, z0)) spawnType(lane, BASE, along, yaw, WRECKS[h % WRECKS.length], h, true);
            }
            if (rnd(bx, bz * 7 + i, 76) < 0.6f) {          // east-west road
                double along = bxs + C + 3 + rnd(bx, bz * 7 + i, 77) * (CELL - C - 8);
                double lane = bzs + 5 + rnd(bx * 7 + i, bz, 78) * 18;
                float yaw = (rnd(bx * 7 + i, bz, 79) < 0.5f ? 90f : -90f) + (rnd(bx * 7 + i, bz, 80) - 0.5f) * 130f;
                int h = hash(bx, bz * 7 + i, 81);
                if (inChunk(along, lane, x0, z0)) spawnType(along, BASE, lane, yaw, WRECKS[h % WRECKS.length], h, true);
            }
        }
        if (rnd(bx, bz, 82) < 0.3f) {                         // pile-up in the junction
            int n = 2 + hash(bx, bz, 83) % 3;
            for (int k = 0; k < n; k++) {
                double px = bxs + 8 + rnd(bx, bz * 5 + k, 84) * 13, pz = bzs + 8 + rnd(bx * 5 + k, bz, 85) * 13;
                int h = hash(bx * 5 + k, bz * 3, 86);
                if (inChunk(px, pz, x0, z0)) spawnType(px, BASE, pz, rnd(bx, bz, 87 + k) * 360f, WRECKS[h % WRECKS.length], h, true);
            }
        }
    }

    private void spawnType(double x, double y, double z, float yaw, String id, int h, boolean wreck) {
        EntityType<VehicleEntity> type = ModRegistry.ENTITY_TYPES.get(id);
        if (type == null) return;
        VehicleEntity v = type.create(w.toServerWorld());
        if (v == null) return;
        v.refreshPositionAndAngles(x, y, z, yaw, 0);
        boolean livery = id.equals("taxi") || id.equals("police_car") || id.equals("ambulance") || id.equals("fire_truck") || id.equals("school_bus")
                || v.type.category == com.nothatcher.vehicleforge.VehicleType.Category.TRUCK && !id.equals("van") && !id.equals("pickup_truck");
        if (!livery) v.setPaint(CAR_COLORS[(h >>> 5) % CAR_COLORS.length]);
        v.setFuel(0.2f + ((h >>> 9) % 70) / 100f);
        if (wreck && (h >>> 21) % 3 == 0 && Math.abs(y - BASE) < 0.01) {
            pos.set(Math.floor(x), BASE, Math.floor(z));
            if (w.isAir(pos)) w.setBlockState(pos.toImmutable(), CityBlocks.SMOKE_SOURCE.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        v.addCommandTag("vehicleforge_parked");                          // the city parked it (Outbreak gives these keys)
        // something left in the back
        v.setCargoLoot(new net.minecraft.util.Identifier("vehicleforge", wreck ? "vehicles/wreck" : "vehicles/trunk"), ((long) h << 16) ^ (long) (x * 31 + z));
        if (wreck) {
            v.setWreck(true);
            if ((h >>> 3) % 3 != 0) v.setPaint(BURNT[(h >>> 7) % BURNT.length]);
            v.setHealth(v.type.health * 0.15f);
            v.setFuel(0f);
        }
        w.spawnEntity(v);
    }
}
