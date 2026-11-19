package com.nothatcher.vehicleforge.block;

import com.nothatcher.vehicleforge.VehicleForge;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
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
    public static final Block MANHOLE = reg("manhole_cover", new Block(road().sounds(BlockSoundGroup.METAL)));
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
    public static final Block BENCH = reg("bench", new FacingShapeBlock(prop().mapColor(MapColor.OAK_TAN),
            VoxelShapes.union(box(0, 5, 2, 16, 7, 12), box(0, 7, 11, 16, 15, 13), box(1, 0, 3, 3, 5, 11), box(13, 0, 3, 15, 5, 11)), false));
    public static final Block BUS_STOP_SIGN = reg("bus_stop_sign", new FacingShapeBlock(metal(),
            VoxelShapes.union(box(7, 0, 7, 9, 16, 9), box(2, 9, 7.5, 14, 16, 8.5)), false));
    public static final Block TRASH_BIN = reg("trash_bin", new FacingShapeBlock(metal().mapColor(MapColor.DARK_GREEN),
            box(3, 0, 3, 13, 14, 13), false));

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
