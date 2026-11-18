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

    enum Kind { TOWERS, APARTMENTS, PARKING, PARK, GAS, HOUSES }

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
    };
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

    Kind kind(int bx, int bz) {
        if (bx == 0 && bz == 0) return Kind.PARK;                          // central park at spawn
        int d = Math.max(Math.abs(bx), Math.abs(bz));
        float r = rnd(bx, bz, 1);
        if (d <= 3) return r < 0.12f ? Kind.PARK : r < 0.2f ? Kind.PARKING : Kind.TOWERS;
        if (d <= 8) return r < 0.12f ? Kind.PARK : r < 0.24f ? Kind.PARKING : r < 0.32f ? Kind.GAS : Kind.APARTMENTS;
        return r < 0.15f ? Kind.PARK : r < 0.21f ? Kind.GAS : Kind.HOUSES;
    }

    // ------------------------------------------------------------- column

    void column(int x, int z) {
        int ux = Math.floorMod(x, CELL), uz = Math.floorMod(z, CELL);
        boolean rx = ux >= R0 && ux < R1, rz = uz >= R0 && uz < R1;
        if (rx || rz) {
            road(x, z, ux, uz, rx, rz);
        } else if (ux < C || uz < C) {
            sidewalk(x, z, ux, uz);
        } else {
            int bx = Math.floorDiv(x, CELL), bz = Math.floorDiv(z, CELL);
            int lx = ux - C, lz = uz - C;
            switch (kind(bx, bz)) {
                case TOWERS -> lotBuilding(x, z, bx, bz, lx, lz, true);
                case APARTMENTS -> lotBuilding(x, z, bx, bz, lx, lz, false);
                case PARKING -> parking(x, z, lx, lz);
                case PARK -> park(x, z, lx, lz);
                case GAS -> gas(x, z, bx, bz, lx, lz);
                case HOUSES -> houses(x, z, bx, bz, lx, lz);
            }
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
    }

    private void sidewalk(int x, int z, int ux, int uz) {
        set(x, GROUND, z, WALK);
        boolean cornerX = ux == R0 - 1 || ux == R1, cornerZ = uz == R0 - 1 || uz == R1;
        if (cornerX && cornerZ) {
            trafficLight(x, z, uz == R0 - 1 ? Direction.NORTH : Direction.SOUTH, ux == R0 - 1 ? Direction.WEST : Direction.EAST);
            return;
        }
        boolean alongZ = uz >= C + 4 && uz < CELL - 4, alongX = ux >= C + 4 && ux < CELL - 4;
        // street lamps on the kerb, arms reaching over the road
        if (alongZ && ux == R0 - 1 && Math.floorMod(z, 18) == 0) { lamp(x, z, Direction.EAST); return; }
        if (alongZ && ux == R1 && Math.floorMod(z, 18) == 9) { lamp(x, z, Direction.WEST); return; }
        if (alongX && uz == R0 - 1 && Math.floorMod(x, 18) == 0) { lamp(x, z, Direction.SOUTH); return; }
        if (alongX && uz == R1 && Math.floorMod(x, 18) == 9) { lamp(x, z, Direction.NORTH); return; }
        // hydrants and bins
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
        int floors = towers ? 8 + (int) (r * 22) + (d <= 1 ? 8 : 0) : 2 + (int) (r * 8);
        int style = hash(lotX, lotZ, 4) % 4 + (towers ? 0 : 4);
        Direction front = lj == 0 ? Direction.NORTH : Direction.SOUTH;
        if (px < 2 || px > 23 || pz < 2 || pz > 23) {
            boolean walkway = (px == 12 || px == 13) && (front == Direction.NORTH ? pz < 2 : pz > 23);
            set(x, GROUND, z, towers || walkway ? PLAZA : Blocks.GRASS_BLOCK.getDefaultState());
            return;
        }
        building(x, z, ox + 2, oz + 2, ox + 23, oz + 23, floors, STYLES[style], front, towers && floors >= 14 && r > 0.35f);
    }

    /**
     * One column of a building with floors every 4 blocks, lights in the floors, windows on every facade,
     * a 2-high entrance on the front, and a ladder shaft in the corner up to a walkable roof (helipad on tall towers).
     */
    private void building(int x, int z, int x0, int z0, int x1, int z1, int floors, Style st, Direction front, boolean helipad) {
        int top = BASE + floors * 4;
        boolean ex = x == x0 || x == x1, ez = z == z0 || z == z1;
        boolean edge = ex || ez, corner = ex && ez;
        set(x, GROUND, z, st.frame);
        int along = ex ? z : x;
        boolean frontFace = switch (front) {
            case NORTH -> z == z0;
            case SOUTH -> z == z1;
            case WEST -> x == x0;
            default -> x == x1;
        };
        int mid = front.getAxis() == Direction.Axis.Z ? (x0 + x1) / 2 : (z0 + z1) / 2;
        boolean doorCol = frontFace && !corner && (along == mid || along == mid + 1);
        boolean ladderCol = x == x0 + 1 && z == z0 + 1;
        for (int y = BASE; y < top; y++) {
            int rel = y - BASE;
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
            } else if (floorRow) {
                s = Math.floorMod(x + z * 3, 7) == 0 ? LIGHT : INNER_FLOOR;
            } else {
                continue;
            }
            set(x, y, z, s);
        }
        // roof, parapet, rooftop units or a helipad
        if (ladderCol) {
            set(x, top, z, LADDER);
            return;
        }
        BlockState roof = st.frame;
        if (helipad && !edge) {
            double cx = x - (x0 + x1) / 2.0, cz = z - (z0 + z1) / 2.0;
            double ax = Math.abs(cx), az = Math.abs(cz);
            if (ax <= 5 && az <= 5) {
                if (Math.max(ax, az) > 4) roof = LINE_Y;
                else if ((ax >= 1.5 && ax <= 2.5 && az <= 2.5) || (az < 1 && ax <= 2)) roof = LINE_W;
                else roof = ASPHALT;
            }
        }
        set(x, top, z, roof);
        if (edge) {
            set(x, top + 1, z, st.frame);
        } else if (!helipad && x >= x0 + 3 && x <= x0 + 5 && z >= z1 - 5 && z <= z1 - 3) {
            set(x, top + 1, z, Blocks.IRON_BLOCK.getDefaultState());       // air conditioning unit
            if (x == x0 + 4) set(x, top + 2, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
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
        return 0;
    }

    private void trees(int x, int z) {
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

    void parkedCars(int x0, int z0) {
        int bx = Math.floorDiv(x0, CELL), bz = Math.floorDiv(z0, CELL);
        int bxs = bx * CELL, bzs = bz * CELL;
        Kind k = kind(bx, bz);
        if (k == Kind.PARKING) {
            for (int ri = 0; ri < ROWS.length; ri++) {
                for (int s = 1; s <= 15; s++) {
                    double cx = bxs + C + 3 * s + 1.5, cz = bzs + C + ROWS[ri] + 0.5;
                    if (inChunk(cx, cz, x0, z0) && rnd(bxs + s, bzs + ri, 41) < 0.45f) {
                        spawn(cx, cz, ri == 0 ? 0f : 180f, hash(bxs + s, bzs + ri, 42));
                    }
                }
            }
        } else if (k == Kind.HOUSES) {
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
        // kerbside parking lanes, driving on the right
        for (int i = 0; i < 16; i++) {
            int z = z0 + i, x = x0 + i;
            int uz = Math.floorMod(z, CELL), ux = Math.floorMod(x, CELL);
            if (uz >= C + 5 && uz <= CELL - 8 && Math.floorMod(z, 9) == 4) {
                double west = bxs + 4.0, east = bxs + 25.0;
                if (inChunk(west, z + 0.5, x0, z0) && rnd(bxs, z, 46) < 0.3f) spawn(west, z + 0.5, 0f, hash(bxs, z, 47));
                if (inChunk(east, z + 0.5, x0, z0) && rnd(bxs + 1, z, 46) < 0.3f) spawn(east, z + 0.5, 180f, hash(bxs + 1, z, 47));
            }
            if (ux >= C + 5 && ux <= CELL - 8 && Math.floorMod(x, 9) == 4) {
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
        String id = PARKED[h % PARKED.length];
        EntityType<VehicleEntity> type = ModRegistry.ENTITY_TYPES.get(id);
        if (type == null) return;
        VehicleEntity v = type.create(w.toServerWorld());
        if (v == null) return;
        v.refreshPositionAndAngles(x, BASE, z, yaw, 0);
        if (!id.equals("taxi") && !id.equals("police_car")) v.setPaint(CAR_COLORS[(h >>> 5) % CAR_COLORS.length]);
        v.setFuel(0.2f + ((h >>> 9) % 70) / 100f);
        w.spawnEntity(v);
    }
}
