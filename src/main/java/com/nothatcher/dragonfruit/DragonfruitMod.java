package com.nothatcher.dragonfruit;

import com.nothatcher.dragonfruit.net.Net;
import com.nothatcher.dragonfruit.power.PowerManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DragonfruitMod implements ModInitializer {
    public static final String MOD_ID = "dragonfruit";
    public static final Logger LOG = LoggerFactory.getLogger("Dragonfruit Powers");

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        com.nothatcher.dragonfruit.content.ModContent.register();
        ModItems.register();
        Net.registerServer();
        PowerManager.registerEvents();
        ModLoot.register();
        ModCommands.register();
        LOG.info("Dragonfruit Powers loaded: {} fruits", FruitType.values().length - 1);
    }
}
