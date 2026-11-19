package com.nothatcher.outbreak;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Bandages, painkillers, antibiotics and first aid kits. Hold use to apply. */
public class MedItem extends Item {
    public enum Kind { BANDAGE, PAINKILLERS, ANTIBIOTICS, FIRST_AID }

    private final Kind kind;

    public MedItem(Settings s, Kind kind) {
        super(s);
        this.kind = kind;
    }

    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity p, Hand hand) {
        p.setCurrentHand(hand);
        return TypedActionResult.consume(p.getStackInHand(hand));
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return switch (kind) {
            case BANDAGE -> 30;
            case PAINKILLERS, ANTIBIOTICS -> 20;
            case FIRST_AID -> 50;
        };
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return kind == Kind.PAINKILLERS || kind == Kind.ANTIBIOTICS ? UseAction.EAT : UseAction.BOW;
    }

    @Override
    public ItemStack finishUsing(ItemStack st, World w, LivingEntity e) {
        if (!w.isClient) {
            switch (kind) {
                case BANDAGE -> {
                    e.heal(4);
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));
                }
                case PAINKILLERS -> {
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 300, 0));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 0));
                    e.removeStatusEffect(StatusEffects.NAUSEA);
                }
                case ANTIBIOTICS -> {
                    if (e.hasStatusEffect(OutbreakEffects.INFECTION)) {
                        e.removeStatusEffect(OutbreakEffects.INFECTION);
                        if (e instanceof PlayerEntity p) p.sendMessage(Text.translatable("msg.outbreak.cured").formatted(Formatting.GREEN), true);
                    }
                    e.removeStatusEffect(StatusEffects.POISON);
                    e.removeStatusEffect(StatusEffects.HUNGER);
                }
                case FIRST_AID -> {
                    e.heal(12);
                    e.removeStatusEffect(StatusEffects.POISON);
                    e.removeStatusEffect(StatusEffects.WITHER);
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1));
                }
            }
            w.playSound(null, e.getX(), e.getY(), e.getZ(), kind == Kind.BANDAGE || kind == Kind.FIRST_AID
                    ? SoundEvents.ITEM_ARMOR_EQUIP_LEATHER : SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.8f, 1.1f);
        }
        if (!(e instanceof PlayerEntity p) || !p.getAbilities().creativeMode) st.decrement(1);
        return st;
    }

    @Override
    public void appendTooltip(ItemStack st, @Nullable World w, List<Text> tip, TooltipContext ctx) {
        tip.add(Text.translatable("tooltip.outbreak." + kind.name().toLowerCase()).formatted(Formatting.GRAY));
    }
}
