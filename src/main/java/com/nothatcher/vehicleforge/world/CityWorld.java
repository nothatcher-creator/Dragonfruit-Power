package com.nothatcher.vehicleforge.world;

import com.nothatcher.vehicleforge.VehicleForge;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/**
 * "Vehicle Forge City" world type: a superflat world (see data/vehicleforge/worldgen) whose only biome runs this
 * feature in every chunk, laying out roads, sidewalks, towers, apartments, houses, parks, parking lots and gas stations.
 */
public final class CityWorld {
    public static final Feature<DefaultFeatureConfig> CITY = new CityFeature(DefaultFeatureConfig.CODEC);

    private CityWorld() {}

    public static void register() {
        Registry.register(Registries.FEATURE, VehicleForge.id("city"), CITY);
    }
}
