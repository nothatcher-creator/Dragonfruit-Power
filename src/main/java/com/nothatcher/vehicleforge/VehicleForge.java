package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.interior.InteriorManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleForge implements ModInitializer {
    public static final String MOD_ID = "vehicleforge";
    public static final Logger LOG = LoggerFactory.getLogger("Vehicle Forge");
    /** /gamerule vehicleFuel false turns fuel off */
    public static GameRules.Key<GameRules.BooleanRule> FUEL_RULE;

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        FUEL_RULE = GameRuleRegistry.register("vehicleFuel", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
        ModRegistry.register();
        com.nothatcher.vehicleforge.world.CityWorld.register();
        Net.registerServer();
        InteriorManager.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> Scheduler.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Scheduler.clear());
        LOG.info("Vehicle Forge loaded: {} vehicles", VehicleTypes.ALL.size());
    }
}
