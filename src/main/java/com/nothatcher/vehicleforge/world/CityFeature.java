package com.nothatcher.vehicleforge.world;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Builds the city one chunk at a time; every column is a pure function of its coordinates and the seed. */
public class CityFeature extends Feature<DefaultFeatureConfig> {
    public CityFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
        StructureWorldAccess world = ctx.getWorld();
        BlockPos origin = ctx.getOrigin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        CityGen gen = new CityGen(world, world.getSeed());
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                gen.column(x, z);
            }
        }
        gen.parkedCars(x0, z0);
        return true;
    }
}
