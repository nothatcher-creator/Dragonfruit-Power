package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.interior.InteriorManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleForge implements ModInitializer {
    public static final String MOD_ID = "vehicleforge";
    public static final Logger LOG = LoggerFactory.getLogger("Vehicle Forge");

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModRegistry.register();
        Net.registerServer();
        InteriorManager.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> Scheduler.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Scheduler.clear());
        LOG.info("Vehicle Forge loaded: {} vehicles", VehicleTypes.ALL.size());
    }
}
