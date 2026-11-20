package com.nothatcher.outbreak;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Worn on the head: night vision while the battery lasts (durability drains while worn; repair with a car battery). */
public class NightVisionGoggles extends ArmorItem {
    public NightVisionGoggles(ArmorMaterial m, Settings s) {
        super(m, Type.HELMET, s);
    }

    @Override
    public void inventoryTick(ItemStack st, World w, Entity e, int slot, boolean selected) {
        if (w.isClient || !(e instanceof LivingEntity le) || le.getEquippedStack(EquipmentSlot.HEAD) != st) return;
        if (st.getDamage() >= st.getMaxDamage() - 1) return;
        le.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, true, false, true));
        if (w.getTime() % 40 == 0) st.setDamage(st.getDamage() + 1);
    }
}
