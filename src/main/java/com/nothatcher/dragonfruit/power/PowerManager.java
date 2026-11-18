package com.nothatcher.dragonfruit.power;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.net.Net;
import com.nothatcher.dragonfruit.net.Vfx;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.enchantment.FrostWalkerEnchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PowerManager {
    /** absolute server tick when each ability comes off cooldown */
    private static final Map<UUID, int[]> COOLDOWN_END = new HashMap<>();
    /** absolute server tick when the fruit's buff (Second Gear / Flame Body) ends */
    private static final Map<UUID, Integer> BUFF_END = new HashMap<>();

    private PowerManager() {}

    // ------------------------------------------------------------------ state

    public static FruitType get(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        return server == null ? FruitType.NONE : PowerState.get(server).getFruit(p.getUuid());
    }

    private static int now(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        return server == null ? 0 : server.getTicks();
    }

    public static void grant(ServerPlayerEntity p, FruitType type) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        FruitType old = get(p);
        PowerState.get(server).setFruit(p.getUuid(), type);
        COOLDOWN_END.remove(p.getUuid());
        BUFF_END.remove(p.getUuid());

        ServerWorld w = (ServerWorld) p.getWorld();
        Vec3d center = p.getPos().add(0, 1, 0);
        Vfx.burst(w, center, Vfx.rgb(type.color), Vfx.B_AWAKEN, 1.5f);
        Vfx.ring(w, p.getPos().add(0, 0.1, 0), Vfx.UP, 6f, type.color, 24, Vfx.S_NONE);
        Vfx.shake(w, center, 0.6f, 25, 12);
        Vfx.flash(w, center, 0.35f, 10);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.8f, 1.3f);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.6f, 1.6f);

        if (old != FruitType.NONE && old != type) {
            p.sendMessage(Text.translatable("msg.dragonfruit.replaced", old.displayName()).formatted(Formatting.GRAY), false);
        }
        p.sendMessage(Text.translatable("msg.dragonfruit.awaken", type.displayName()).styled(s -> s.withColor(type.color).withBold(true)), false);
        p.sendMessage(Text.translatable("msg.dragonfruit.controls").formatted(Formatting.YELLOW), false);
        sync(p);
    }

    public static void clear(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        PowerState.get(server).setFruit(p.getUuid(), FruitType.NONE);
        COOLDOWN_END.remove(p.getUuid());
        BUFF_END.remove(p.getUuid());
        sync(p);
    }

    public static boolean buffActive(ServerPlayerEntity p) {
        Integer end = BUFF_END.get(p.getUuid());
        return end != null && end > now(p);
    }

    public static void setBuff(ServerPlayerEntity p, int ticks) {
        BUFF_END.put(p.getUuid(), now(p) + ticks);
    }

    public static void sync(ServerPlayerEntity p) {
        FruitType f = get(p);
        int t = now(p);
        int[] ends = COOLDOWN_END.get(p.getUuid());
        int[] remaining = new int[f.abilities.length];
        for (int i = 0; i < remaining.length; i++) {
            remaining[i] = ends != null && i < ends.length ? ends[i] - t : 0;
        }
        Integer buffEnd = BUFF_END.get(p.getUuid());
        Net.sendState(p, f.ordinal(), remaining, buffEnd == null ? 0 : buffEnd - t);
    }

    // ---------------------------------------------------------------- abilities

    public static void useAbility(ServerPlayerEntity p, int index) {
        if (!p.isAlive() || p.isSpectator()) return;
        FruitType f = get(p);
        if (f == FruitType.NONE || index < 0 || index >= f.abilities.length) return;

        if (p.isTouchingWater() && !p.isCreative()) {
            p.sendMessage(Text.translatable("msg.dragonfruit.water").formatted(Formatting.AQUA), true);
            return;
        }

        int t = now(p);
        int[] ends = COOLDOWN_END.computeIfAbsent(p.getUuid(), k -> new int[f.abilities.length]);
        if (ends.length != f.abilities.length) {
            ends = new int[f.abilities.length];
            COOLDOWN_END.put(p.getUuid(), ends);
        }
        if (ends[index] > t && !p.isCreative()) {
            float secs = (ends[index] - t) / 20f;
            p.sendMessage(Text.translatable("msg.dragonfruit.cooldown", f.abilityName(index), String.format("%.1f", secs))
                    .formatted(Formatting.RED), true);
            sync(p);
            return;
        }

        boolean used = Abilities.run(p, f, index);
        if (used) {
            // creative players still get a short cooldown so spamming the key doesn't stack effects
            int cd = p.isCreative() ? Math.min(10, f.abilities[index].cooldown()) : f.abilities[index].cooldown();
            ends[index] = t + cd;
        }
        sync(p);
    }

    // ------------------------------------------------------------------ events

    public static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Scheduler.tick();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                tickPassives(p, server.getTicks());
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            COOLDOWN_END.clear();
            BUFF_END.clear();
            Scheduler.clear();
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity p) return allowDamage(p, source);
            return true;
        });
    }

    private static void tickPassives(ServerPlayerEntity p, int tick) {
        if (!p.isAlive()) return;
        FruitType f = get(p);
        if (f == FruitType.NONE) return;

        // Seawater weakness: drained while in water.
        if (p.isTouchingWater() && !p.isCreative() && !p.isSpectator() && tick % 20 == 0) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 1, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, true, false, true));
        }

        switch (f) {
            case ICE -> {
                if (p.isOnGround() && !p.isSpectator()) {
                    FrostWalkerEnchantment.freezeWater(p, p.getWorld(), p.getBlockPos(), 1);
                }
            }
            case FLAME -> {
                if (buffActive(p) && tick % 10 == 0) {
                    ServerWorld w = (ServerWorld) p.getWorld();
                    for (LivingEntity e : Abilities.inRadius(w, p.getPos().add(0, 1, 0), 2.6, p)) {
                        e.setOnFireFor(4);
                        Abilities.hurt(p, e, 3f, false);
                    }
                }
            }
            default -> { }
        }
    }

    private static boolean allowDamage(ServerPlayerEntity p, DamageSource source) {
        FruitType f = get(p);
        if (f == FruitType.NONE) return true;
        ServerWorld w = (ServerWorld) p.getWorld();
        switch (f) {
            case RUBBER -> {
                if (source.isIn(DamageTypeTags.IS_FALL) || source.isIn(DamageTypeTags.IS_LIGHTNING)) return false;
                if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                    // bullets and arrows bounce off rubber
                    w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 1f, 1.4f);
                    Vfx.burst(w, p.getPos().add(0, 1.2, 0), Vec3d.ZERO, Vfx.B_RUBBER_HIT, 0.5f);
                    return false;
                }
            }
            case FLAME -> {
                if (source.isIn(DamageTypeTags.IS_FIRE)) return false;
                if (buffActive(p) && !source.isIn(DamageTypeTags.IS_PROJECTILE)
                        && source.getAttacker() instanceof LivingEntity attacker && attacker != p) {
                    // logia: melee passes through flames and burns the attacker
                    attacker.setOnFireFor(5);
                    Vfx.burst(w, p.getPos().add(0, 1, 0), Vec3d.ZERO, Vfx.B_FIRE_SMALL, 1f);
                    return false;
                }
            }
            case ICE -> {
                if (source.isIn(DamageTypeTags.IS_FREEZING)) return false;
            }
            case LIGHT -> {
                if (source.isIn(DamageTypeTags.IS_FALL)) return false;
            }
            case QUAKE -> {
                if (source.isIn(DamageTypeTags.IS_EXPLOSION)) return false;
            }
            case THUNDER -> {
                if (source.isIn(DamageTypeTags.IS_LIGHTNING)) return false;
                if (!source.isIn(DamageTypeTags.IS_PROJECTILE) && source.getAttacker() instanceof LivingEntity attacker
                        && attacker != p && attacker.isAlive()) {
                    // touching a thunder user shocks you
                    attacker.timeUntilRegen = 0;
                    attacker.damage(p.getDamageSources().lightningBolt(), 3f);
                    Vfx.burst(w, attacker.getBoundingBox().getCenter(), Vec3d.ZERO, Vfx.B_THUNDER, 0.5f);
                }
            }
            case MAGMA -> {
                if (source.isIn(DamageTypeTags.IS_FIRE)) return false;
            }
            case GRAVITY -> {
                if (source.isIn(DamageTypeTags.IS_FALL)) return false;
            }
            default -> { }
        }
        return true;
    }
}
