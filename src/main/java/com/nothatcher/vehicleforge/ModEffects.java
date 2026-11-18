package com.nothatcher.vehicleforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Status effects. Shrunk: a quarter of your normal size (and slower legs). Milk cures it. */
public final class ModEffects {
    public static final float SHRUNK_SCALE = 0.25f;

    public static final StatusEffect SHRUNK = new StatusEffect(StatusEffectCategory.NEUTRAL, 0x3EC1D3) { }
            .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "5c1b0f5e-3a51-4c7d-9a8e-1f2b3c4d5e6f", -0.45, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

    private ModEffects() {}

    public static void register() {
        Registry.register(Registries.STATUS_EFFECT, VehicleForge.id("shrunk"), SHRUNK);
    }

    /** shrunk by the potion effect, or sitting in a shrunk car (that part is visible on every client) */
    public static boolean isShrunk(LivingEntity e) {
        if (e.hasStatusEffect(SHRUNK)) return true;
        return e.getVehicle() instanceof VehicleEntity v && v.getScaleTarget() < 0.9f;
    }

    public static float scaleOf(LivingEntity e) {
        if (e.hasStatusEffect(SHRUNK)) return SHRUNK_SCALE;
        if (e.getVehicle() instanceof VehicleEntity v && v.getScaleTarget() < 0.9f) return Math.max(SHRUNK_SCALE, v.scaleNow);
        return 1f;
    }
}
