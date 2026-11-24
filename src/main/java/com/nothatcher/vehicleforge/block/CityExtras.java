package com.nothatcher.vehicleforge.block;

import com.nothatcher.vehicleforge.VehicleForge;
import com.nothatcher.vehicleforge.power.PowerBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Block entities, the seat entity and smoke particles for the city blocks. */
public final class CityExtras {
    public static BlockEntityType<PropStorageBlockEntity> STORAGE_BE;
    public static BlockEntityType<PowerBlockEntity> POWER_BE;
    public static BlockEntityType<SmokeBlockEntity> SMOKE_BE;
    public static EntityType<SeatEntity> SEAT;
    public static BlockEntityType<NeonSignBlockEntity> NEON_BE;
    public static final DefaultParticleType FLY = FabricParticleTypes.simple(false);
    public static final DefaultParticleType DARK_SMOKE = FabricParticleTypes.simple(true);
    public static final DefaultParticleType STEAM = FabricParticleTypes.simple(true);

    private CityExtras() {}

    /** what headlights put on the road ahead at night (no item) */
    public static final HeadlightBlock HEADLIGHT = Registry.register(Registries.BLOCK, VehicleForge.id("headlight_light"),
            new HeadlightBlock(net.minecraft.block.AbstractBlock.Settings.create().replaceable().noCollision().dropsNothing().air()
                    .noBlockBreakParticles().luminance(st -> st.get(HeadlightBlock.LEVEL))));

    public static void register() {
        STORAGE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, VehicleForge.id("prop_storage"),
                FabricBlockEntityTypeBuilder.create(PropStorageBlockEntity::new, CityBlocks.STORAGE).build());
        POWER_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, VehicleForge.id("power"),
                FabricBlockEntityTypeBuilder.create(PowerBlockEntity::new, CityBlocks.POWER).build());
        SMOKE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, VehicleForge.id("smoke"),
                FabricBlockEntityTypeBuilder.create(SmokeBlockEntity::new, new Block[]{CityBlocks.SMOKE_SOURCE, CityBlocks.STEAM_VENT}).build());
        SEAT = Registry.register(Registries.ENTITY_TYPE, VehicleForge.id("seat"),
                FabricEntityTypeBuilder.<SeatEntity>create(SpawnGroup.MISC, SeatEntity::new).dimensions(EntityDimensions.fixed(0.2f, 0.2f))
                        .trackRangeBlocks(48).trackedUpdateRate(20).build());
        Registry.register(Registries.PARTICLE_TYPE, VehicleForge.id("dark_smoke"), DARK_SMOKE);
        Registry.register(Registries.PARTICLE_TYPE, VehicleForge.id("steam"), STEAM);
        Registry.register(Registries.PARTICLE_TYPE, VehicleForge.id("fly"), FLY);
        NEON_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, VehicleForge.id("neon_sign"),
                FabricBlockEntityTypeBuilder.create(NeonSignBlockEntity::new, CityBlocks.NEON_SIGN).build());
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> com.nothatcher.vehicleforge.power.PowerGrid.clear());
    }
}
