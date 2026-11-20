package com.nothatcher.outbreak;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Police vest, military vest + helmet, riot helmet. Textures: assets/minecraft/textures/models/armor/outbreak_*. */
public enum OutbreakArmor implements ArmorMaterial {
    POLICE("outbreak_police", 22, 2, 6, 0, 0f, 0f),
    MILITARY("outbreak_military", 30, 3, 7, 0, 2f, 0.05f),
    RIOT("outbreak_riot", 25, 3, 5, 0, 1f, 0.1f),
    KEVLAR("outbreak_kevlar", 38, 4, 9, 0, 3f, 0.1f);

    private final String name;
    private final int durability, helmet, chest, legs;
    private final float toughness, knockback;

    OutbreakArmor(String name, int durability, int helmet, int chest, int legs, float toughness, float knockback) {
        this.name = name;
        this.durability = durability;
        this.helmet = helmet;
        this.chest = chest;
        this.legs = legs;
        this.toughness = toughness;
        this.knockback = knockback;
    }

    @Override
    public int getDurability(ArmorItem.Type type) {
        return durability * switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> helmet;
            case CHESTPLATE -> chest;
            case LEGGINGS -> legs;
            case BOOTS -> 0;
        };
    }

    @Override
    public int getEnchantability() {
        return 8;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.LEATHER);
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
        return knockback;
    }
}
