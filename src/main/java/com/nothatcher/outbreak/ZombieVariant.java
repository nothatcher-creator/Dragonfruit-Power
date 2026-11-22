package com.nothatcher.outbreak;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

/**
 * Not every corpse gets up the same way. Crawlers have lost the use of their legs and drag themselves along,
 * low to the ground and easy to miss in the grass. Bloaters are swollen and slow, and burst into a sickening
 * cloud when they go down. Brutes are big, tough and hard to knock back, often still in riot gear.
 */
public enum ZombieVariant {
    NORMAL, CRAWLER, BLOATER, BRUTE;

    public static ZombieVariant of(Entity e) {
        if (!(e instanceof ZombieJob.Holder h)) return NORMAL;
        int v = h.outbreak$variant();
        return v >= 0 && v < values().length ? values()[v] : NORMAL;
    }

    static ZombieVariant roll(Random r) {
        int k = r.nextInt(100);
        return k < 8 ? CRAWLER : k < 12 ? BLOATER : k < 15 ? BRUTE : NORMAL;
    }

    /** set a freshly spawned zombie up as this variant */
    void apply(ZombieEntity z) {
        if (z instanceof ZombieJob.Holder h) h.outbreak$setVariant(ordinal());
        switch (this) {
            case CRAWLER -> {
                base(z, EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.07);
                base(z, EntityAttributes.GENERIC_MAX_HEALTH, 14);
            }
            case BLOATER -> {
                base(z, EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1);
                base(z, EntityAttributes.GENERIC_MAX_HEALTH, 34);
            }
            case BRUTE -> {
                base(z, EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15);
                base(z, EntityAttributes.GENERIC_MAX_HEALTH, 50);
                base(z, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
                base(z, EntityAttributes.GENERIC_ATTACK_DAMAGE, 6);
                if (z.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                    z.equipStack(EquipmentSlot.HEAD, new ItemStack(OutbreakItems.RIOT_HELMET));
                    z.setEquipmentDropChance(EquipmentSlot.HEAD, 0.3f);
                }
            }
            default -> { }
        }
        z.setHealth(z.getMaxHealth());
    }

    private static void base(ZombieEntity z, net.minecraft.entity.attribute.EntityAttribute a, double v) {
        EntityAttributeInstance i = z.getAttributeInstance(a);
        if (i != null) i.setBaseValue(v);
    }
}
