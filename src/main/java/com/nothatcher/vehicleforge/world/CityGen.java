package com.nothatcher.vehicleforge.world;

import com.nothatcher.vehicleforge.ModRegistry;
import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.block.CityBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

    enum Kind {
        TOWERS, APARTMENTS, PARKING, PARK, GAS, HOUSES,
        LANDMARK, PLAZA, HOSPITAL, FIRE, POLICE, SCHOOL, STADIUM, ROWHOUSES,
        WAREHOUSE, FACTORY, CONTAINERS, CONSTRUCTION, RIVER
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
    private static final BlockState LADDER = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST);
    private static final BlockState PATH = Blocks.DIRT_PATH.getDefaultState();
    private static final BlockState WATER = Blocks.WATER.getDefaultState();
    private static final BlockState[] FLOWERS = {
            Blocks.POPPY.getDefaultState(), Blocks.DANDELION.getDefaultState(), Blocks.CORNFLOWER.getDefaultState(),
            Blocks.OXEYE_DAISY.getDefaultState(), Blocks.AZURE_BLUET.getDefaultState()
    };

    /** facade materials */
    private record Style(BlockState wall, BlockState window, BlockState frame, int bay, boolean tallWindows, boolean shopFront) {}

    private static final Style[] STYLES = {
            // downtown towers
            new Style(Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.GRAY_CONCRETE.getDefaultState(), 4, true, false),
            new Style(Blocks.BLACK_CONCRETE.getDefaultState(), Blocks.GRAY_STAINED_GLASS.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState(), 3, true, false),
            new Style(Blocks.QUARTZ_BLOCK.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.SMOOTH_QUARTZ.getDefaultState(), 2, false, false),
            new Style(Blocks.STONE_BRICKS.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.POLISHED_ANDESITE.getDefaultState(), 2, false, false),
            // midtown
            new Style(Blocks.BRICKS.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.SMOOTH_STONE.getDefaultState(), 3, false, false),
            new Style(Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.CUT_SANDSTONE.getDefaultState(), 2, false, true),
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), 3, false, true),
            new Style(Blocks.TERRACOTTA.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.POLISHED_DEEPSLATE.getDefaultState(), 2, false, true),
            // more towers (8-11)
            new Style(Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(), Blocks.BLUE_STAINED_GLASS.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(), 5, true, false),
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.GREEN_STAINED_GLASS.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(), 4, true, false),
            new Style(Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.BROWN_STAINED_GLASS.getDefaultState(), Blocks.CUT_SANDSTONE.getDefaultState(), 3, true, false),
            new Style(Blocks.POLISHED_GRANITE.getDefaultState(), Blocks.BLACK_STAINED_GLASS.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState(), 2, false, false),
            // more midtown (12-14)
            new Style(Blocks.MUD_BRICKS.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.PACKED_MUD.getDefaultState(), 2, false, false),
            new Style(Blocks.CYAN_TERRACOTTA.getDefaultState(), Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState(), Blocks.STONE.getDefaultState(), 3, false, true),
            new Style(Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.POLISHED_BLACKSTONE.getDefaultState(), 3, false, true),
            // civic (15 hospital, 16 police, 17 red brick, 18 white terracotta rowhouse)
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.QUARTZ_BLOCK.getDefaultState(), 3, false, false),
            new Style(Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.BLUE_STAINED_GLASS.getDefaultState(), Blocks.BLUE_CONCRETE.getDefaultState(), 3, false, true),
            new Style(Blocks.RED_NETHER_BRICKS.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState(), 2, false, false),
            new Style(Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.GLASS.getDefaultState(), Blocks.BROWN_TERRACOTTA.getDefaultState(), 2, false, false),
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
    private static final int[] CAR_COLORS = {
            0xF2F2F2, 0x1A1A1E, 0x8E949A, 0xB7BCC2, 0xA01C1C, 0x1E3F8A, 0x2C5E3A, 0x6B4A2E, 0xC8A040, 0x5C6E80, 0x8A1E4A, 0xE07020
    };

    private final StructureWorldAccess w;
    private final long seed;
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

    private boolean isAir(int x, int y, int z) {
        pos.set(x, y, z);
        return w.isAir(pos);
    }

    static boolean riverRow(int bz) {
        return Math.floorMod(bz, RIVER_EVERY) == RIVER_ROW;
    }

    Kind kind(int bx, int bz) {
        if (riverRow(bz)) return Kind.RIVER;
        if (bx == 0 && bz == 0) return Kind.PARK;                          // central park at spawn
        if (bx == 1 && bz == 1) return Kind.LANDMARK;                      // the tallest tower in the city
        int d = Math.max(Math.abs(bx), Math.abs(bz));
        float r = rnd(bx, bz, 1);
        boolean industrial = d >= 5 && rnd(Math.floorDiv(bx, 3), Math.floorDiv(bz, 3), 70) < 0.25f;
        if (industrial) {
            return r < 0.32f ? Kind.WAREHOUSE : r < 0.55f ? Kind.FACTORY : r < 0.75f ? Kind.CONTAINERS
                    : r < 0.88f ? Kind.CONSTRUCTION : Kind.PARKING;
        }
        if (d <= 4) {
            return r < 0.08f ? Kind.PARK : r < 0.14f ? Kind.PLAZA : r < 0.19f ? Kind.PARKING : r < 0.25f ? Kind.CONSTRUCTION
                    : r < 0.28f ? Kind.HOSPITAL : Kind.TOWERS;
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
            if (r < 0.62f) return Kind.ROWHOUSES;
            return Kind.APARTMENTS;
        }
        return r < 0.12f ? Kind.PARK : r < 0.17f ? Kind.GAS : r < 0.22f ? Kind.SCHOOL : r < 0.25f ? Kind.FIRE : Kind.HOUSES;
    }

    // ------------------------------------------------------------- column

    void column(int x, int z) {
        int ux = Math.floorMod(x, CELL), uz = Math.floorMod(z, CELL);
        boolean rx = ux >= R0 && ux < R1, rz = uz >= R0 && uz < R1;
        int bx = Math.floorDiv(x, CELL), bz = Math.floorDiv(z, CELL);
        boolean bridge = riverRow(bz) && uz >= C && ux < C;                // a north-south road crossing the river
        if (rx || rz) {
            road(x, z, ux, uz, rx, rz);
        } else if (ux < C || uz < C) {
            sidewalk(x, z, ux, uz);
        } else {
            int lx = ux - C, lz = uz - C;
            switch (kind(bx, bz)) {
                case TOWERS -> towersBlock(x, z, bx, bz, lx, lz);
                case APARTMENTS -> lotBuilding(x, z, bx, bz, lx, lz, false);
                case PARKING -> parking(x, z, lx, lz);
                case PARK -> park(x, z, lx, lz);
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
            }
        }
        if (bridge) bridge(x, z, ux, uz - C);
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
            set(x, GROUND + 1, z, ux == R0 - 1 || ux == R1 ? CURB : WALK_SLAB);
            return;
        }
        if (alongZ && ux == 1 && Math.floorMod(z, 45) == 40) { set(x, BASE, z, CityBlocks.facing(CityBlocks.FIRE_HYDRANT, Direction.EAST)); return; }
        if (alongZ && ux == C - 2 && Math.floorMod(z, 45) == 18) { set(x, BASE, z, CityBlocks.facing(CityBlocks.FIRE_HYDRANT, Direction.WEST)); return; }
        if (alongZ && ux == 0 && Math.floorMod(z, 30) == 15) { set(x, BASE, z, CityBlocks.facing(CityBlocks.TRASH_BIN, Direction.EAST)); return; }
        if (alongX && uz == C - 1 && Math.floorMod(x, 30) == 15) { set(x, BASE, z, CityBlocks.facing(CityBlocks.TRASH_BIN, Direction.NORTH)); return; }
        boolean kerb = ux == R0 - 1 || ux == R1 || uz == R0 - 1 || uz == R1;
        set(x, GROUND + 1, z, kerb ? CURB : WALK_SLAB);
    }

    private void lamp(int x, int z, Direction arm) {
        for (int y = BASE; y <= BASE + 5; y++) set(x, y, z, POLE);
        set(x, BASE + 6, z, CityBlocks.facing(CityBlocks.STREET_LAMP, arm));
    }

    private void trafficLight(int x, int z, Direction a, Direction b) {
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
        int d = Math.max(Math.abs(bx), Math.abs(bz));
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
                s = Math.floorMod(x + z * 3, 7) == 0 ? LIGHT : INNER_FLOOR;
            } else {
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
                if (x >= bx0 + 3 && x <= bx0 + 5 && z >= bz1 - 5 && z <= bz1 - 3) {
                    set(x, top + 1, z, Blocks.IRON_BLOCK.getDefaultState());       // air conditioning unit
                    if (x == bx0 + 4) set(x, top + 2, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
                }
            }
            default -> { }
        }
    }

    // --------------------------------------------------- parks and parking

    private void park(int x, int z, int lx, int lz) {
        double dx = lx - 25.5, dz = lz - 25.5, rr = dx * dx + dz * dz;
        if (rr < 30) {                                                       // fountain
            if (rr < 1) {
                set(x, GROUND, z, Blocks.STONE_BRICKS.getDefaultState());
                set(x, BASE, z, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
                set(x, BASE + 1, z, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
                set(x, BASE + 2, z, LIGHT);
            } else if (rr < 20) {
                set(x, GROUND - 1, z, Blocks.STONE_BRICKS.getDefaultState());
                set(x, GROUND, z, WATER);
            } else {
                set(x, GROUND, z, Blocks.STONE_BRICKS.getDefaultState());
                set(x, BASE, z, Blocks.STONE_BRICK_SLAB.getDefaultState());
            }
            return;
        }
        boolean path = (lx >= 24 && lx < 28) || (lz >= 24 && lz < 28) || lx < 1 || lx > 50 || lz < 1 || lz > 50;
        if (path) {
            set(x, GROUND, z, PATH);
            return;
        }
        if ((lx == 23 || lx == 28) && Math.floorMod(lz, 10) == 5 || (lz == 23 || lz == 28) && Math.floorMod(lx, 10) == 5) {
            lamp(x, z, lx == 23 ? Direction.EAST : lx == 28 ? Direction.WEST : lz == 23 ? Direction.SOUTH : Direction.NORTH);
            return;
        }
        if (rnd(x, z, 11) < 0.05f) set(x, BASE, z, FLOWERS[hash(x, z, 12) % FLOWERS.length]);
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
                    set(x, BASE, z, Blocks.IRON_BLOCK.getDefaultState());
                    set(x, BASE + 1, z, Blocks.RED_CONCRETE.getDefaultState());
                    set(x, BASE + 2, z, Blocks.WHITE_CONCRETE.getDefaultState());
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
        boolean drive = l.alongX ? (px >= 14 && (l.face == Direction.NORTH ? z <= l.hz1 : z >= l.hz0))
                : (pz >= 14 && (l.face == Direction.WEST ? x <= l.hx1 : x >= l.hx0));
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
        if (!(inA && inC)) return;
        set(x, GROUND, z, hs.floor);
        set(x, BASE, z, hs.floor);
        boolean edgeA = a == a0 || a == a1, edgeC = c == c0 || c == c1;
        if (edgeA || edgeC) {
            boolean frontWall = switch (l.face) {
                case NORTH, WEST -> c == c0;
                default -> c == c1;
            };
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
        } else {
            set(x, BASE + 4, z, a == (a0 + a1) / 2 && c == (c0 + c1) / 2 ? Blocks.GLOWSTONE.getDefaultState() : hs.floor);
            if (a == a0 + 1 && c == c1 - 1) set(x, BASE + 1, z, Blocks.CRAFTING_TABLE.getDefaultState());
            if (a == a1 - 1 && c == c1 - 1) set(x, BASE + 1, z, Blocks.BOOKSHELF.getDefaultState());
        }
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
            for (int y = BASE; y <= BASE + 2; y++) set(x, y, z, Blocks.GLASS.getDefaultState());
        } else if (along == 4) {
            set(x, GROUND + 1, z, WALK_SLAB);
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

    /** a river: quay walls, promenades with trees, benches and lamps, and 40 blocks of water */
    private void river(int x, int z, int lx, int lz) {
        if (lz <= 4 || lz >= 47) {                                          // promenade
            boolean grass = lz <= 1 || lz >= 50;
            set(x, GROUND, z, grass ? Blocks.GRASS_BLOCK.getDefaultState() : WALK);
            if (lz == 4 || lz == 47) {
                if (Math.floorMod(x, 16) == 8) lamp(x, z, lz == 4 ? Direction.SOUTH : Direction.NORTH);
                else if (Math.floorMod(x, 16) == 2 || Math.floorMod(x, 16) == 3) set(x, BASE, z, CityBlocks.facing(CityBlocks.BENCH, lz == 4 ? Direction.SOUTH : Direction.NORTH));
            }
            return;
        }
        if (lz == 5 || lz == 46) {                                           // quay wall with a railing
            for (int y = BED; y <= GROUND; y++) set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            set(x, BASE, z, Blocks.STONE_BRICK_WALL.getDefaultState());
            return;
        }
        set(x, BED, z, rnd(x, z, 19) < 0.2f ? Blocks.CLAY.getDefaultState() : Blocks.GRAVEL.getDefaultState());
        for (int y = BED + 1; y <= GROUND; y++) set(x, y, z, y <= WATER_TOP ? WATER : AIR);
        if (lz == 6 && rnd(x, 0, 20) < 0.06f) set(x, WATER_TOP + 1, z, Blocks.LILY_PAD.getDefaultState());
    }

    private void plaza(int x, int z, int lx, int lz) {
        set(x, GROUND, z, (lx + lz) % 2 == 0 ? PLAZA : Blocks.POLISHED_DIORITE.getDefaultState());
        double dx = lx - 25.5, dz = lz - 25.5, ax = Math.abs(dx), az = Math.abs(dz);
        if (ax < 1 && az < 1) {                                              // obelisk
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

    private void police(int x, int z, int bx, int bz, int lx, int lz) {
        int ox = bx * CELL + C, oz = bz * CELL + C;
        if (lx >= 6 && lx <= 45 && lz >= 16 && lz <= 41) {
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
            for (int y = BASE; y < BASE + h; y++) set(x, y, z, (hash(x, y * 31 + z, 25) % 3 == 0 ? Blocks.BARREL : Blocks.OAK_PLANKS).getDefaultState());
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
        if (lx >= 38 && lx <= 47 && lz >= 26 && lz <= 29) {
            for (int y = BASE; y < BASE + 3; y++) set(x, y, z, (lz == 26 && y == BASE + 1 && lx % 3 == 0) ? Blocks.GLASS.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
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
        if (k == Kind.PARK) {
            double dx = lx - 25.5, dz = lz - 25.5;
            if (dx * dx + dz * dz < 64 || (lx >= 21 && lx < 31) || (lz >= 21 && lz < 31)) return 0;
            return 0.8f;
        }
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
        int bxs = bx * CELL, bzs = bz * CELL;
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
                        String id = switch (hash(bx, bz * 9 + salt, 47) % 5) {
                            case 0 -> "yacht";
                            case 1, 2 -> "speedboat";
                            default -> "jet_ski";
                        };
                        parkAt(id, 8 + 12 * i, side == 0 ? 12 : 39, WATER_TOP + 0.5, side == 0 ? 90f : -90f, 0.35, bx, bz, salt, x0, z0);
                    }
                }
            }
            default -> { }
        }
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
        spawnType(x, BASE, z, yaw, PARKED[h % PARKED.length], h);
    }

    private void spawnType(double x, double y, double z, float yaw, String id, int h) {
        EntityType<VehicleEntity> type = ModRegistry.ENTITY_TYPES.get(id);
        if (type == null) return;
        VehicleEntity v = type.create(w.toServerWorld());
        if (v == null) return;
        v.refreshPositionAndAngles(x, y, z, yaw, 0);
        boolean livery = id.equals("taxi") || id.equals("police_car") || id.equals("ambulance") || id.equals("fire_truck") || id.equals("school_bus")
                || v.type.category == com.nothatcher.vehicleforge.VehicleType.Category.TRUCK && !id.equals("van") && !id.equals("pickup_truck");
        if (!livery) v.setPaint(CAR_COLORS[(h >>> 5) % CAR_COLORS.length]);
        v.setFuel(0.2f + ((h >>> 9) % 70) / 100f);
        w.spawnEntity(v);
    }
}
