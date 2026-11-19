package com.nothatcher.outbreak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbreak: a survival-horror companion to Vehicle Forge. Zombies everywhere that don't burn in the sun, drawn by
 * the noise of gunshots and engines; bites can infect you; guns, ammo, melee weapons, armour, medicine and food
 * are hidden in houses and buildings (Vehicle Forge city buildings and vanilla village/dungeon chests).
 */
public class Outbreak implements ModInitializer {
    public static final String MOD_ID = "outbreak";
    public static final Logger LOG = LoggerFactory.getLogger("Outbreak");

    public static GameRules.Key<GameRules.BooleanRule> ZOMBIES, DAYLIGHT, INFECTION;
    public static GameRules.Key<GameRules.IntRule> CAP, SPRINTERS;

    public static final Identifier RELOAD = id("reload");
    public static final RegistryKey<net.minecraft.entity.damage.DamageType> INFECTION_DAMAGE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id("infection"));

    public static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static SoundEvent sound(String name) {
        return SOUNDS.get(name);
    }

    @Override
    public void onInitialize() {
        ZOMBIES = GameRuleRegistry.register("outbreakZombies", GameRules.Category.SPAWNING, GameRuleFactory.createBooleanRule(true));
        DAYLIGHT = GameRuleRegistry.register("outbreakDaylightZombies", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
        INFECTION = GameRuleRegistry.register("outbreakInfection", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
        CAP = GameRuleRegistry.register("outbreakZombieCap", GameRules.Category.SPAWNING, GameRuleFactory.createIntRule(70, 0, 400));
        SPRINTERS = GameRuleRegistry.register("outbreakSprinterPercent", GameRules.Category.MOBS, GameRuleFactory.createIntRule(8, 0, 100));

        for (String s : new String[]{"pistol_fire", "shotgun_fire", "rifle_fire", "smg_fire", "reload", "empty_click"}) {
            Identifier sid = id(s);
            SOUNDS.put(s, Registry.register(Registries.SOUND_EVENT, sid, SoundEvent.of(sid)));
        }
        OutbreakEffects.register();
        OutbreakItems.register();
        LootInjector.register();

        ServerTickEvents.END_WORLD_TICK.register(w -> {
            ZombieSpawner.tick(w);
            Noise.vehicleNoise(w);
        });
        ServerPlayNetworking.registerGlobalReceiver(RELOAD, (server, player, handler, buf, sender) -> server.execute(() -> {
            ItemStack st = player.getMainHandStack();
            if (st.getItem() instanceof GunItem gun) gun.reload(player, st);
        }));
        LOG.info("Outbreak loaded: stay quiet, stay alive.");
    }

    public static boolean creative(PlayerEntity p) {
        return p.getAbilities().creativeMode;
    }
}
