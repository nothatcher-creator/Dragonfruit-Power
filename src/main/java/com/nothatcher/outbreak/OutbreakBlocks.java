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
    public static final Block LITTER = reg("litter", new Decal(decal(MapColor.WHITE, BlockSoundGroup.WOOL)));
    public static final Block BROKEN_GLASS = reg("broken_glass", new Decal(decal(MapColor.CLEAR, BlockSoundGroup.GLASS)));
    public static final Block SCORCH = reg("scorch_mark", new Decal(decal(MapColor.BLACK, BlockSoundGroup.SAND)));
    public static final Block WEEDS = reg("weeds", new Weeds(decal(MapColor.DARK_GREEN, BlockSoundGroup.GRASS)));
    public static final Block TRASH_BAG = reg("trash_bag", new TrashBag(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).noCollision()
            .strength(0.2f).nonOpaque().pistonBehavior(PistonBehavior.DESTROY).sounds(BlockSoundGroup.WOOL)));

    public static final Feature<DefaultFeatureConfig> OVERRUN = new OverrunFeature(DefaultFeatureConfig.CODEC);

    private OutbreakBlocks() {}

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
