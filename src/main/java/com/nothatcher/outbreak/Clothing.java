package com.nothatcher.outbreak;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * One material per piece of clothing so each gets its own look (texture: assets/minecraft/textures/models/armor/
 * outbreak_<name>_layer_1/2.png). Thick clothes protect a little, and every armour point makes bites less likely to
 * break the skin.
 */
public class Clothing implements ArmorMaterial {
    private final String name;
    private final int durability, protection;
    private final float toughness;
    private final boolean leather;

    public Clothing(String name, int durability, int protection, float toughness, boolean leather) {
        this.name = "outbreak_" + name;
        this.durability = durability;
        this.protection = protection;
        this.toughness = toughness;
        this.leather = leather;
    }

    @Override
    public int getDurability(ArmorItem.Type type) {
        return durability;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return protection;
    }

    @Override
    public int getEnchantability() {
        return 10;
    }

    @Override
    public SoundEvent getEquipSound() {
        return leather ? SoundEvents.ITEM_ARMOR_EQUIP_LEATHER : SoundEvents.ITEM_ARMOR_EQUIP_GENERIC;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(leather ? Items.LEATHER : Items.STRING);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return 0;
    }
}
