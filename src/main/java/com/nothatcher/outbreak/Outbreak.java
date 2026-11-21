package com.nothatcher.outbreak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

    public static GameRules.Key<GameRules.BooleanRule> ZOMBIES, DAYLIGHT, INFECTION, CORPSES;
    public static GameRules.Key<GameRules.IntRule> CAP, SPRINTERS, AREA_POP, CORPSE_MINUTES, MAX_CORPSES;

    public static final Identifier RELOAD = id("reload");
    public static final Identifier SHOVE = id("shove");
    public static final Identifier ACTION = id("action");
    public static final Identifier PROGRESS = id("progress");
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
        CAP = GameRuleRegistry.register("outbreakMaxZombies", GameRules.Category.SPAWNING, GameRuleFactory.createIntRule(30, 0, 400));
        SPRINTERS = GameRuleRegistry.register("outbreakSprinterChance", GameRules.Category.MOBS, GameRuleFactory.createIntRule(3, 0, 100));
        CORPSES = GameRuleRegistry.register("outbreakCorpses", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
        CORPSE_MINUTES = GameRuleRegistry.register("outbreakCorpseMinutes", GameRules.Category.MOBS, GameRuleFactory.createIntRule(20, 0, 10000));
        MAX_CORPSES = GameRuleRegistry.register("outbreakMaxCorpses", GameRules.Category.MOBS, GameRuleFactory.createIntRule(40, 1, 1000));
        AREA_POP = GameRuleRegistry.register("outbreakAreaPopulation", GameRules.Category.SPAWNING, GameRuleFactory.createIntRule(60, 1, 2000));

        for (String s : new String[]{"pistol_fire", "shotgun_fire", "rifle_fire", "smg_fire", "silenced_fire", "revolver_fire", "sniper_fire", "reload", "empty_click"}) {
            Identifier sid = id(s);
            SOUNDS.put(s, Registry.register(Registries.SOUND_EVENT, sid, SoundEvent.of(sid)));
        }
        OutbreakEffects.register();
        OutbreakBlocks.register();
        OutbreakItems.register();
        LootInjector.register();

        ServerTickEvents.END_WORLD_TICK.register(w -> {
            ZombieSpawner.tick(w);
            Noise.vehicleNoise(w);
            Actions.tick(w);
            Airport.tick();
            CarKeys.tick();
            Flashlights.tick(w);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(Airport::onLoad);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(CarKeys::onLoad);
        CarKeys.register();
        Blood.register();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ZombieEntity && entity.getWorld() instanceof ServerWorld sw)
                Population.of(sw).onKill(sw, entity.getBlockPos());
        });
        ServerPlayNetworking.registerGlobalReceiver(SHOVE, (server, player, handler, buf, sender) -> server.execute(() -> Shove.shove(player)));
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, sender) -> Actions.receive(server, player, buf));
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
