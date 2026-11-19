package com.nothatcher.outbreak;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Zombie infection. It gets worse over twenty minutes: hunger, then nausea and weakness, then slowness and pain.
 * If it runs its course you die and get back up as one of them. Antibiotics cure it; milk does not.
 */
public class InfectionEffect extends StatusEffect {
    public static final int DURATION = 24000;

    public InfectionEffect() {
        super(StatusEffectCategory.HARMFUL, 0x4E7A2B);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 40 == 0 || duration <= 2;
    }

    @Override
    public void applyUpdateEffect(LivingEntity e, int amplifier) {
        if (e.getWorld().isClient) return;
        StatusEffectInstance inst = e.getStatusEffect(this);
        if (inst == null) return;
        int d = inst.getDuration();
        if (d <= 2) {
            turn(e);
            return;
        }
        float left = d / (float) DURATION;
        if (left < 0.75f) e.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 0, true, false));
        if (left < 0.5f && e.getRandom().nextInt(8) == 0) e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0, true, false));
        if (left < 0.5f) e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0, true, false));
        if (left < 0.3f) e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0, true, false));
        if (left < 0.12f && e.getRandom().nextInt(3) == 0) e.damage(source(e), 1f);
        if (e instanceof PlayerEntity p && d % 2400 == 0) {
            String key = left > 0.66f ? "msg.outbreak.infection_1" : left > 0.33f ? "msg.outbreak.infection_2" : "msg.outbreak.infection_3";
            p.sendMessage(Text.translatable(key).formatted(Formatting.DARK_GREEN), true);
        }
    }

    static DamageSource source(LivingEntity e) {
        return new DamageSource(e.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(Outbreak.INFECTION_DAMAGE));
    }

    /** the end: die, and a zombie wearing your clothes gets up where you fell */
    static void turn(LivingEntity e) {
        if (!(e.getWorld() instanceof ServerWorld w)) return;
        ZombieEntity z = EntityType.ZOMBIE.create(w);
        if (z != null) {
            z.refreshPositionAndAngles(e.getX(), e.getY(), e.getZ(), e.getYaw(), 0);
            z.initialize(w, w.getLocalDifficulty(e.getBlockPos()), SpawnReason.CONVERSION, null, null);
            z.setBaby(false);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                z.equipStack(slot, e.getEquippedStack(slot).copy());
                z.setEquipmentDropChance(slot, 0f);
            }
            z.setCustomName(e.getName());
            z.setPersistent();
            w.spawnEntityAndPassengers(z);
        }
        e.removeStatusEffect(OutbreakEffects.INFECTION);
        e.damage(source(e), Float.MAX_VALUE);
    }
}
