package com.nothatcher.vehicleforge.block;

import com.nothatcher.vehicleforge.VehicleForge;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.List;

/** Road and street blocks used by the city world (and placeable by hand). */
public final class CityBlocks {
    public static final List<Block> ALL = new ArrayList<>();

    private static AbstractBlock.Settings road() {
        return AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(1.5f, 6f).requiresTool().sounds(BlockSoundGroup.STONE);
    }

    private static AbstractBlock.Settings walk() {
        return AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_GRAY).strength(1.5f, 6f).requiresTool().sounds(BlockSoundGroup.STONE);
    }

    private static AbstractBlock.Settings metal() {
        return AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(2f, 6f).requiresTool().sounds(BlockSoundGroup.METAL).nonOpaque();
    }

    private static AbstractBlock.Settings prop() {
        return AbstractBlock.Settings.create().mapColor(MapColor.ORANGE).strength(0.6f).sounds(BlockSoundGroup.WOOD).nonOpaque();
    }

    private static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Block.createCuboidShape(x0, y0, z0, x1, y1, z1);
    }

    public static final Block ASPHALT = reg("asphalt", new Block(road()));
    public static final Block LINE_WHITE = reg("asphalt_white_line", new PaintedRoadBlock(road()));
    public static final Block LINE_YELLOW = reg("asphalt_yellow_line", new PaintedRoadBlock(road()));
    public static final Block LINE_DOUBLE_YELLOW = reg("asphalt_double_yellow_line", new PaintedRoadBlock(road()));
    public static final Block CROSSWALK = reg("asphalt_crosswalk", new PaintedRoadBlock(road()));
    public static final Block MANHOLE = reg("manhole_cover", new ManholeBlock(road().sounds(BlockSoundGroup.METAL).nonOpaque()));
    public static final Block STORM_DRAIN = reg("storm_drain", new Block(road().sounds(BlockSoundGroup.METAL)));
    public static final Block SIDEWALK = reg("sidewalk", new Block(walk()));
    public static final Block SIDEWALK_SLAB = reg("sidewalk_slab", new SlabBlock(walk()));
    public static final Block CURB = reg("curb", new SlabBlock(walk()));
    public static final Block LAMP_POST = reg("lamp_post", new PoleBlock(metal()));
    public static final Block STREET_LAMP = reg("street_lamp", new FacingShapeBlock(metal().luminance(s -> 15),
            VoxelShapes.union(box(6, 0, 6, 10, 16, 10), box(7, 11, 0, 9, 14, 8)), true));
    public static final Block TRAFFIC_LIGHT = reg("traffic_light", new TrafficLightBlock(metal().luminance(s -> 8),
            box(4, 0, 5, 12, 16, 12)));
    public static final Block TRAFFIC_CONE = reg("traffic_cone", new FacingShapeBlock(prop(),
            VoxelShapes.union(box(2, 0, 2, 14, 2, 14), box(5, 2, 5, 11, 13, 11)), false));
    public static final Block ROAD_BARRIER = reg("road_barrier", new FacingShapeBlock(walk().nonOpaque(),
            VoxelShapes.union(box(0, 0, 3, 16, 5, 13), box(0, 5, 5, 16, 16, 11)), false));
    public static final Block ROADBLOCK = reg("roadblock", new FacingShapeBlock(prop(),
            box(0, 0, 5, 16, 15, 11), false));
    public static final Block FIRE_HYDRANT = reg("fire_hydrant", new FacingShapeBlock(metal().mapColor(MapColor.RED),
            VoxelShapes.union(box(4, 0, 4, 12, 2, 12), box(5, 2, 5, 11, 12, 11), box(6, 12, 6, 10, 14, 10)), false));
    public static final Block BENCH = reg("bench", new SeatBlock(prop().mapColor(MapColor.OAK_TAN),
            VoxelShapes.union(box(0, 5, 2, 16, 7, 12), box(0, 7, 11, 16, 15, 13), box(1, 0, 3, 3, 5, 11), box(13, 0, 3, 15, 5, 11)), 0.45));
    public static final Block BUS_STOP_SIGN = reg("bus_stop_sign", new FacingShapeBlock(metal(),
            VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(2, 9, 7.5, 14, 16, 8.5)), false));
    public static final Block TRASH_BIN = reg("trash_bin", new FacingShapeBlock(metal().mapColor(MapColor.DARK_GREEN),
            box(3, 0, 3, 13, 14, 13), false));

    public static final Block GARAGE_DOOR = reg("garage_door", new GarageDoorBlock(metal().mapColor(MapColor.WHITE).strength(4f, 8f)));

    // ---- glass: no frame lines, neighbouring panes merge into one sheet
    public static final Block CLEAR_GLASS = reg("clear_glass", new GlassBlock(AbstractBlock.Settings.copy(Blocks.GLASS).mapColor(MapColor.CLEAR)));
    public static final Block REFLECTIVE_GLASS = reg("reflective_glass", new GlassBlock(AbstractBlock.Settings.copy(Blocks.GLASS)
            .mapColor(MapColor.LIGHT_BLUE).strength(0.6f)));

    // ---- furniture
    private static AbstractBlock.Settings furniture(MapColor c, BlockSoundGroup sound) {
        return AbstractBlock.Settings.create().mapColor(c).strength(1f).sounds(sound).nonOpaque();
    }

    public static final Block DESK = reg("desk", new StorageProp(furniture(MapColor.OAK_TAN, BlockSoundGroup.WOOD), box(0, 0, 0, 16, 14, 16), 1, SoundEvents.BLOCK_BARREL_OPEN));
    public static final Block OFFICE_CHAIR = reg("office_chair", new SeatBlock(furniture(MapColor.BLACK, BlockSoundGroup.WOOL),
            VoxelShapes.union(box(3, 0, 3, 13, 8, 13), box(3, 8, 11, 13, 16, 14)), 0.55));
    public static final Block COMPUTER = reg("computer", new ToggleProp(furniture(MapColor.BLACK, BlockSoundGroup.METAL), box(2, 0, 3, 14, 10, 10)));
    public static final Block FILING_CABINET = reg("filing_cabinet", new StorageProp(furniture(MapColor.GRAY, BlockSoundGroup.METAL), box(1, 0, 2, 15, 16, 15), 2, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN));
    public static final Block WATER_COOLER = reg("water_cooler", new InteractiveProp(furniture(MapColor.WHITE, BlockSoundGroup.METAL), box(4, 0, 4, 12, 16, 12), InteractiveProp.Kind.WATER));
    public static final Block POTTED_PLANT = reg("potted_plant", new FacingShapeBlock(furniture(MapColor.DARK_GREEN, BlockSoundGroup.GRASS), box(2, 0, 2, 14, 16, 14), false));
    public static final Block SOFA = reg("sofa", new SeatBlock(furniture(MapColor.BLUE, BlockSoundGroup.WOOL),
            VoxelShapes.union(box(0, 0, 1, 16, 7, 15), box(0, 7, 11, 16, 14, 15)), 0.45));
    public static final Block TV = reg("tv", new ToggleProp(furniture(MapColor.BLACK, BlockSoundGroup.METAL), box(0, 0, 6, 16, 15, 12)));
    public static final Block KITCHEN_COUNTER = reg("kitchen_counter", new StorageProp(furniture(MapColor.WHITE, BlockSoundGroup.STONE), box(0, 0, 0, 16, 15, 16), 1, SoundEvents.BLOCK_BARREL_OPEN));
    public static final Block FRIDGE = reg("fridge", new StorageProp(furniture(MapColor.WHITE, BlockSoundGroup.METAL), box(1, 0, 1, 15, 16, 15), 2, SoundEvents.BLOCK_IRON_DOOR_OPEN));
    public static final Block STOVE = reg("stove", new InteractiveProp(furniture(MapColor.WHITE, BlockSoundGroup.METAL), box(0, 0, 0, 16, 15, 16), InteractiveProp.Kind.STOVE));
    public static final Block STORE_SHELF = reg("store_shelf", new StorageProp(furniture(MapColor.WHITE, BlockSoundGroup.METAL), box(0, 0, 1, 16, 16, 15), 1, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC));
    public static final Block CHECKOUT_COUNTER = reg("checkout_counter", new StorageProp(furniture(MapColor.GRAY, BlockSoundGroup.METAL), box(0, 0, 0, 16, 16, 16), 1, SoundEvents.BLOCK_CHEST_OPEN));

    // ---- street furniture
    public static final Block MAILBOX = reg("mailbox", new StorageProp(metal().mapColor(MapColor.BLUE), box(3, 0, 3, 13, 15, 13), 1, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN));
    public static final Block NEWSPAPER_BOX = reg("newspaper_box", new StorageProp(metal().mapColor(MapColor.RED), box(3, 0, 4, 13, 12, 12), 1, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN));
    public static final Block VENDING_MACHINE = reg("vending_machine", new InteractiveProp(metal().mapColor(MapColor.RED).luminance(s -> 7), box(1, 0, 2, 15, 16, 14), InteractiveProp.Kind.VENDING));
    public static final Block ATM = reg("atm", new InteractiveProp(metal().luminance(s -> 4), box(2, 0, 4, 14, 16, 13), InteractiveProp.Kind.ATM));
    public static final Block PARKING_METER = reg("parking_meter", new InteractiveProp(metal(), VoxelShapes.union(box(7, 0, 7, 9, 10, 9), box(5, 10, 6, 11, 15, 10)), InteractiveProp.Kind.METER));
    public static final Block STOP_SIGN = reg("stop_sign", new FacingShapeBlock(metal().mapColor(MapColor.RED), VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(3, 8, 6.5, 13, 16, 7.5)), false));
    public static final Block PICNIC_TABLE = reg("picnic_table", new SeatBlock(prop().mapColor(MapColor.OAK_TAN), box(0, 0, 0, 16, 11, 16), 0.45));
    public static final Block PLANTER = reg("planter", new FacingShapeBlock(walk().nonOpaque(), box(1, 0, 1, 15, 15, 15), false));
    public static final Block AC_UNIT = reg("ac_unit", new FacingShapeBlock(metal(), box(0, 0, 1, 16, 12, 15), false));
    public static final Block FOUNTAIN_SPOUT = reg("fountain_spout", new FountainSpoutBlock(walk().nonOpaque(), box(4, 0, 4, 12, 12, 12)));

    // ---- church and churchyard
    public static final Block PEW = reg("pew", new SeatBlock(prop().mapColor(MapColor.BROWN),
            VoxelShapes.union(box(0, 0, 2, 16, 7, 13), box(0, 7, 12, 16, 16, 14)), 0.45));
    public static final Block ALTAR = reg("altar", new FacingShapeBlock(walk().nonOpaque().mapColor(MapColor.WHITE), box(0, 0, 2, 16, 14, 14), false));
    public static final Block GRAVESTONE = reg("gravestone", new FacingShapeBlock(walk().nonOpaque(), VoxelShapes.union(box(3, 0, 6, 13, 2, 10), box(4, 2, 7, 12, 14, 9)), false));

    // ---- park trees
    public static final Block MAPLE_LEAVES = reg("maple_leaves", new LeavesBlock(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).mapColor(MapColor.ORANGE)));
    public static final Block LINDEN_LEAVES = reg("linden_leaves", new LeavesBlock(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).mapColor(MapColor.DARK_GREEN)));

    // ---- smoke and steam
    public static final Block SMOKE_SOURCE = reg("smoke_source", new SmokeBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).noCollision()
            .strength(0.3f).nonOpaque().sounds(BlockSoundGroup.WOOD).luminance(s -> 5), false));
    public static final Block STEAM_VENT = reg("steam_vent", new SmokeBlock(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).noCollision()
            .strength(1f).nonOpaque().sounds(BlockSoundGroup.METAL), true));

    // ---- electricity
    public static final Block GENERATOR = reg("generator", new com.nothatcher.vehicleforge.power.PowerBlock(metal().mapColor(MapColor.YELLOW).strength(3f),
            box(1, 0, 2, 15, 12, 14), com.nothatcher.vehicleforge.power.PowerBlock.Kind.GENERATOR));
    public static final Block SOLAR_PANEL = reg("solar_panel", new com.nothatcher.vehicleforge.power.PowerBlock(metal().mapColor(MapColor.BLUE),
            box(0, 0, 0, 16, 8, 16), com.nothatcher.vehicleforge.power.PowerBlock.Kind.SOLAR));
    public static final Block WIND_TURBINE = reg("wind_turbine", new com.nothatcher.vehicleforge.power.PowerBlock(metal().mapColor(MapColor.WHITE).strength(3f),
            box(2, 0, 2, 14, 16, 14), com.nothatcher.vehicleforge.power.PowerBlock.Kind.WIND));
    public static final Block BATTERY = reg("battery_bank", new com.nothatcher.vehicleforge.power.PowerBlock(metal().mapColor(MapColor.GRAY),
            box(1, 0, 3, 15, 14, 13), com.nothatcher.vehicleforge.power.PowerBlock.Kind.BATTERY));
    public static final Block POWER_POLE = reg("power_pole", new com.nothatcher.vehicleforge.power.PowerBlock(prop().mapColor(MapColor.BROWN).strength(2f),
            box(6, 0, 6, 10, 16, 10), com.nothatcher.vehicleforge.power.PowerBlock.Kind.POLE));
    public static final Block ELECTRIC_LIGHT = reg("electric_light", new com.nothatcher.vehicleforge.power.ElectricLightBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(0.8f).sounds(BlockSoundGroup.GLASS)));

    // ---- secrets and the airport
    public static final Block SECRET_BOOKSHELF = reg("secret_bookshelf", new SecretBlock(AbstractBlock.Settings.copy(Blocks.BOOKSHELF).nonOpaque()));
    public static final Block SECRET_QUARTZ = reg("secret_quartz", new SecretBlock(AbstractBlock.Settings.copy(Blocks.SMOOTH_QUARTZ).nonOpaque()));
    public static final Block SECRET_STONE = reg("secret_stone", new SecretBlock(AbstractBlock.Settings.copy(Blocks.SMOOTH_STONE).nonOpaque()));
    public static final Block SUITCASE = reg("suitcase", new StorageProp(furniture(MapColor.BLUE, BlockSoundGroup.WOOL), box(2, 0, 5, 14, 11, 11), 1, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER));
    /** buried under every airport column so Outbreak knows where the airport is; just dirt to look at */
    public static final Block AIRPORT_MARKER = reg("airport_marker", new Block(AbstractBlock.Settings.copy(Blocks.DIRT)));

    // ---- wave 8: more street furniture, signs, billboards, slopes, the elevator
    public static final Block PARKING_STOP = reg("parking_stop", new FacingShapeBlock(walk().nonOpaque(), box(0, 0, 5, 16, 3, 11), false));
    public static final Block SPEED_LIMIT_SIGN = reg("speed_limit_sign", new FacingShapeBlock(metal().mapColor(MapColor.WHITE), VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(4, 6, 6.5, 12, 16, 7.5)), false));
    public static final Block ONE_WAY_SIGN = reg("one_way_sign", new FacingShapeBlock(metal().mapColor(MapColor.BLACK), VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(2, 10, 6.5, 14, 14, 7.5)), false));
    public static final Block NO_PARKING_SIGN = reg("no_parking_sign", new FacingShapeBlock(metal().mapColor(MapColor.WHITE), VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(4, 7, 6.5, 12, 16, 7.5)), false));
    public static final Block YIELD_SIGN = reg("yield_sign", new FacingShapeBlock(metal().mapColor(MapColor.RED), VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(3, 8, 6.5, 13, 16, 7.5)), false));
    public static final Block STREET_NAME_SIGN = reg("street_name_sign", new FacingShapeBlock(metal().mapColor(MapColor.GREEN), VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(0, 12, 7, 16, 15, 9)), false));
    public static final Block BILLBOARD_SODA = reg("billboard_soda", new FacingShapeBlock(metal().nonOpaque(), box(0, 0, 6, 16, 16, 10), false));
    public static final Block BILLBOARD_BURGER = reg("billboard_burger", new FacingShapeBlock(metal().nonOpaque(), box(0, 0, 6, 16, 16, 10), false));
    public static final Block BILLBOARD_INSURANCE = reg("billboard_insurance", new FacingShapeBlock(metal().nonOpaque(), box(0, 0, 6, 16, 16, 10), false));
    public static final Block BILLBOARD_RADIO = reg("billboard_radio", new FacingShapeBlock(metal().nonOpaque(), box(0, 0, 6, 16, 16, 10), false));
    public static final Block[] BILLBOARDS = {BILLBOARD_SODA, BILLBOARD_BURGER, BILLBOARD_INSURANCE, BILLBOARD_RADIO};
    public static final Block BOLLARD = reg("bollard", new FacingShapeBlock(metal().mapColor(MapColor.YELLOW), box(5, 0, 5, 11, 14, 11), false));
    public static final Block PHONE_BOOTH = reg("phone_booth", new FacingShapeBlock(metal().mapColor(MapColor.RED).nonOpaque(), box(1, 0, 1, 15, 16, 15), false));
    public static final Block TRASH_BAGS = reg("trash_bags", new FacingShapeBlock(furniture(MapColor.BLACK, BlockSoundGroup.WOOL), box(1, 0, 1, 15, 9, 15), false));
    public static final Block BIKE_RACK = reg("bike_rack", new FacingShapeBlock(metal().nonOpaque(), box(0, 0, 6, 16, 10, 10), false));
    public static final Block ELECTRICAL_BOX = reg("electrical_box", new FacingShapeBlock(metal().mapColor(MapColor.GREEN), box(2, 0, 4, 14, 15, 12), false));
    public static final Block SATELLITE_DISH = reg("satellite_dish", new FacingShapeBlock(metal().nonOpaque(), box(2, 0, 2, 14, 14, 14), false));
    public static final Block AWNING = reg("awning", new FacingShapeBlock(furniture(MapColor.RED, BlockSoundGroup.WOOL).noCollision(), box(0, 12, 0, 16, 16, 16), false));
    public static final Block FIRE_ESCAPE = reg("fire_escape", new FacingShapeBlock(metal().nonOpaque(), box(0, 0, 0, 16, 2, 16), false));
    public static final Block ROOF_VENT = reg("roof_vent", new FacingShapeBlock(metal(), box(4, 0, 4, 12, 10, 12), false));
    public static final Block ELEVATOR = reg("elevator", new ElevatorBlock(metal().mapColor(MapColor.IRON_GRAY).strength(4f)));
    public static final Block SLOPE_SHINGLE = reg("slope_shingle", new SlopeBlock(walk().nonOpaque().sounds(BlockSoundGroup.DEEPSLATE_TILES)));
    public static final Block SLOPE_RED_TILE = reg("slope_red_tile", new SlopeBlock(walk().nonOpaque().mapColor(MapColor.RED)));
    public static final Block SLOPE_WOOD = reg("slope_wood", new SlopeBlock(prop().nonOpaque().mapColor(MapColor.BROWN)));
    public static final Block SLOPE_SPRUCE = reg("slope_spruce", new SlopeBlock(prop().nonOpaque().mapColor(MapColor.SPRUCE_BROWN)));
    public static final Block SLOPE_STONE = reg("slope_stone", new SlopeBlock(walk().nonOpaque()));
    public static final Block SLOPE_METAL = reg("slope_metal", new SlopeBlock(metal().nonOpaque()));

    // ---- wave 9: potholes at any depth, flies, neon lettering
    public static final Block POTHOLE = reg("pothole", new PotholeBlock(road()));
    public static final Block DIRT_POTHOLE = reg("dirt_pothole", new PotholeBlock(AbstractBlock.Settings.create().mapColor(MapColor.DIRT_BROWN)
            .strength(0.6f).sounds(BlockSoundGroup.GRAVEL)));
    public static final Block FLIES = reg("flies", new FliesBlock(AbstractBlock.Settings.create().replaceable().noCollision().nonOpaque()
            .strength(0.1f).sounds(BlockSoundGroup.WOOL).noBlockBreakParticles()));
    public static final Block NEON_SIGN = reg("neon_sign", new NeonSignBlock(AbstractBlock.Settings.create().mapColor(MapColor.PINK).noCollision()
            .nonOpaque().strength(0.5f).sounds(BlockSoundGroup.GLASS).luminance(s -> 12)));

    public static final Block[] STORAGE = {SUITCASE, DESK, FILING_CABINET, KITCHEN_COUNTER, FRIDGE, STORE_SHELF, CHECKOUT_COUNTER, MAILBOX, NEWSPAPER_BOX};
    public static final Block[] POWER = {GENERATOR, SOLAR_PANEL, WIND_TURBINE, BATTERY, POWER_POLE};

    /** blocks that need the cutout or translucent render layer */
    public static final Block[] CUTOUT = {POTTED_PLANT, PLANTER, MAPLE_LEAVES, LINDEN_LEAVES, STOP_SIGN, SMOKE_SOURCE, STEAM_VENT, POWER_POLE,
            BIKE_RACK, YIELD_SIGN, FIRE_ESCAPE, SATELLITE_DISH, BILLBOARD_SODA, BILLBOARD_BURGER, BILLBOARD_INSURANCE, BILLBOARD_RADIO};
    public static final Block[] TRANSLUCENT = {CLEAR_GLASS, REFLECTIVE_GLASS, PHONE_BOOTH};

    private CityBlocks() {}

    private static Block reg(String id, Block b) {
        Registry.register(Registries.BLOCK, VehicleForge.id(id), b);
        Registry.register(Registries.ITEM, VehicleForge.id(id), new BlockItem(b, new Item.Settings()));
        ALL.add(b);
        return b;
    }

    public static void register() {
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(TRAFFIC_LIGHT))
                .displayName(Text.translatable("itemGroup.vehicleforge.city"))
                .entries((ctx, entries) -> ALL.forEach(entries::add))
                .build();
        Registry.register(Registries.ITEM_GROUP, VehicleForge.id("city"), group);
    }

    /** blockstate helpers for world generation */
    public static net.minecraft.block.BlockState axis(Block b, net.minecraft.util.math.Direction.Axis axis) {
        return b.getDefaultState().with(Properties.HORIZONTAL_AXIS, axis);
    }

    public static net.minecraft.block.BlockState facing(Block b, net.minecraft.util.math.Direction d) {
        return b.getDefaultState().with(Properties.HORIZONTAL_FACING, d);
    }
}
