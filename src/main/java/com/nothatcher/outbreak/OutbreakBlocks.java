package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import java.util.ArrayList;
import java.util.List;

/** Decay blocks for an overrun city: blood, litter, broken glass, scorch marks, weeds and trash bags. */
public final class OutbreakBlocks {
    public static final List<Block> ALL = new ArrayList<>();

    /** a flat stain or scatter lying on the block below; no collision, breaks instantly, pops off if unsupported */
    public static class Decal extends Block {
        private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 0.5, 16);

        public Decal(Settings s) {
            super(s);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }

        @Override
        public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
            return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP);
        }

        @Override
        public BlockState getStateForNeighborUpdate(BlockState state, Direction dir, BlockState other, WorldAccess world, BlockPos pos, BlockPos otherPos) {
            return dir == Direction.DOWN && !canPlaceAt(state, world, pos) ? net.minecraft.block.Blocks.AIR.getDefaultState()
                    : super.getStateForNeighborUpdate(state, dir, other, world, pos, otherPos);
        }
    }

    /** weeds pushing up through the pavement */
    public static class Weeds extends Decal {
        private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 12, 14);

        public Weeds(Settings s) {
            super(s);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }
    }

    public static class TrashBag extends Decal {
        private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 11, 14);

        public TrashBag(Settings s) {
            super(s);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }
    }

    private static AbstractBlock.Settings decal(MapColor c, BlockSoundGroup sound) {
        return AbstractBlock.Settings.create().mapColor(c).noCollision().breakInstantly().nonOpaque().replaceable()
                .pistonBehavior(PistonBehavior.DESTROY).sounds(sound);
    }

    public static final Block BLOOD = reg("blood_splatter", new Decal(decal(MapColor.DARK_RED, BlockSoundGroup.HONEY)));
    /** big stains: drawn across up to 3x3 blocks, but each is still a single, replaceable, walk-through block */
    public static final List<Block> BIG_DECALS = new ArrayList<>();
    public static final Block BLOOD_POOL = big("blood_pool", MapColor.DARK_RED, BlockSoundGroup.HONEY);
    public static final Block BLOOD_SPRAY = big("blood_spray", MapColor.DARK_RED, BlockSoundGroup.HONEY);
    public static final Block BLOOD_DRAG = big("blood_drag", MapColor.DARK_RED, BlockSoundGroup.HONEY);
    public static final Block DRIED_BLOOD = big("dried_blood", MapColor.TERRACOTTA_RED, BlockSoundGroup.HONEY);
    public static final Block DIRT_SCUFF = big("dirt_scuff", MapColor.DIRT_BROWN, BlockSoundGroup.SAND);
    public static final Block OIL_STAIN = big("oil_stain", MapColor.BLACK, BlockSoundGroup.HONEY);
    public static final Block SKID_MARKS = big("skid_marks", MapColor.BLACK, BlockSoundGroup.SAND);

    private static Block big(String name, MapColor c, BlockSoundGroup sound) {
        Block b = reg(name, new Decal(decal(c, sound)));
        BIG_DECALS.add(b);
        return b;
    }

    public static final Block LITTER = reg("litter", new Decal(decal(MapColor.WHITE, BlockSoundGroup.WOOL)));
    public static final Block BROKEN_GLASS = reg("broken_glass", new Decal(decal(MapColor.CLEAR, BlockSoundGroup.GLASS)));
    public static final Block SCORCH = reg("scorch_mark", new Decal(decal(MapColor.BLACK, BlockSoundGroup.SAND)));
    public static final Block WEEDS = reg("weeds", new Weeds(decal(MapColor.DARK_GREEN, BlockSoundGroup.GRASS)));
    public static final Block TRASH_BAG = reg("trash_bag", new TrashBag(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).noCollision()
            .strength(0.2f).nonOpaque().pistonBehavior(PistonBehavior.DESTROY).sounds(BlockSoundGroup.WOOL)));

    public static final Feature<DefaultFeatureConfig> OVERRUN = new OverrunFeature(DefaultFeatureConfig.CODEC);

    /** a flashlight's invisible light (no item; it cleans itself up) */
    public static final Block FLASHLIGHT_LIGHT = net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.BLOCK,
            Outbreak.id("flashlight_light"), new FlashlightLightBlock(net.minecraft.block.AbstractBlock.Settings.create()
                    .replaceable().noCollision().dropsNothing().air().luminance(s -> 14).noBlockBreakParticles()));

    /** a barricaded door, window or gap (no item: you build it with a hammer, planks and nails) */
    public static final Block BARRICADE = Registry.register(Registries.BLOCK, Outbreak.id("barricade"),
            new BarricadeBlock(AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(-1f, 1200f).nonOpaque().dropsNothing()
                    .sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.BLOCK).blockVision((st, w, p) -> false)
                    .solidBlock((st, w, p) -> false).suffocates((st, w, p) -> false)));
    public static final net.minecraft.block.entity.BlockEntityType<BarricadeBlockEntity> BARRICADE_BE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Outbreak.id("barricade"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(BarricadeBlockEntity::new, BARRICADE).build());

    private OutbreakBlocks() {}

    public static final Block RAIN_BARREL = reg("rain_barrel", new RainBarrelBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLUE).strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque()));

    // ---- decorations
    private static AbstractBlock.Settings dec(MapColor c, BlockSoundGroup g, float hardness) {
        return AbstractBlock.Settings.create().mapColor(c).strength(hardness).sounds(g).nonOpaque();
    }

    private static net.minecraft.util.shape.VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Block.createCuboidShape(x0, y0, z0, x1, y1, z1);
    }

    public static final Block SANDBAGS = reg("sandbags", new Traps.Decor(dec(MapColor.PALE_YELLOW, BlockSoundGroup.SAND, 1.2f), box(0, 0, 0, 16, 12, 16), box(0, 0, 0, 16, 12, 16)));
    public static final Block WOODEN_CRATE = reg("wooden_crate", new Traps.Decor(dec(MapColor.OAK_TAN, BlockSoundGroup.WOOD, 1.5f), box(1, 0, 1, 15, 14, 15), box(1, 0, 1, 15, 14, 15)));
    public static final Block OIL_DRUM = reg("oil_drum", new Traps.Decor(dec(MapColor.RED, BlockSoundGroup.METAL, 2f), box(2, 0, 2, 14, 15, 14), box(2, 0, 2, 14, 15, 14)));
    public static final Block RUBBLE_PILE = reg("rubble_pile", new Traps.Decor(dec(MapColor.STONE_GRAY, BlockSoundGroup.GRAVEL, 1f), box(0, 0, 0, 16, 6, 16), box(0, 0, 0, 16, 6, 16)));
    public static final Block MATTRESS = reg("mattress", new Traps.Decor(dec(MapColor.WHITE_GRAY, BlockSoundGroup.WOOL, 0.4f), box(0, 0, 0, 16, 4, 16), box(0, 0, 0, 16, 4, 16)));
    public static final Block HELP_SIGN = reg("help_sign", new Traps.Decor(dec(MapColor.OAK_TAN, BlockSoundGroup.WOOD, 0.8f).noCollision(), box(0, 0, 14, 16, 16, 16), box(14, 0, 0, 16, 16, 16)));
    public static final Block CAUTION_TAPE = reg("caution_tape", new Traps.Decor(dec(MapColor.YELLOW, BlockSoundGroup.WOOL, 0.1f).noCollision(), box(0, 9, 7, 16, 11, 9), box(7, 9, 0, 9, 11, 16)));
    public static final Block BODY_BAG = reg("body_bag", new Traps.Decor(dec(MapColor.BLACK, BlockSoundGroup.WOOL, 0.5f), box(2, 0, 0, 14, 4, 16), box(0, 0, 2, 16, 4, 14)));

    // ---- booby traps
    public static final Block BEAR_TRAP = reg("bear_trap", new Traps.BearTrap(dec(MapColor.IRON_GRAY, BlockSoundGroup.METAL, 1.5f).noCollision()));
    public static final Block SPIKE_STRIP = reg("spike_strip", new Traps.SpikeStrip(dec(MapColor.OAK_TAN, BlockSoundGroup.WOOD, 1f).noCollision()));
    public static final Block BARBED_WIRE = reg("barbed_wire", new Traps.BarbedWire(dec(MapColor.IRON_GRAY, BlockSoundGroup.CHAIN, 1f).noCollision()));
    public static final Block TIN_CAN_ALARM = reg("tin_can_alarm", new Traps.TinCanAlarm(dec(MapColor.IRON_GRAY, BlockSoundGroup.CHAIN, 0.3f).noCollision()));

    private static Block reg(String name, Block b) {
        Registry.register(Registries.BLOCK, Outbreak.id(name), b);
        Registry.register(Registries.ITEM, Outbreak.id(name), new BlockItem(b, new Item.Settings()));
        ALL.add(b);
        return b;
    }

    public static void register() {
        Registry.register(Registries.FEATURE, Outbreak.id("overrun"), OVERRUN);
        // the Vehicle Forge city gets the full treatment; nothing changes if that mod isn't installed
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(RegistryKey.of(RegistryKeys.BIOME, new Identifier("vehicleforge", "city"))),
                GenerationStep.Feature.TOP_LAYER_MODIFICATION, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Outbreak.id("overrun")));
    }
}
