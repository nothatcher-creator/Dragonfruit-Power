package com.nothatcher.outbreak;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThrowableItem extends Item {
    private final String tip;

    public ThrowableItem(Settings s, String tip) {
        super(s.maxCount(8));
        this.tip = tip;
    }

    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity p, Hand hand) {
        ItemStack st = p.getStackInHand(hand);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.6f, 0.6f);
        if (!w.isClient) {
            ThrownGear g = new ThrownGear(w, p, this);
            g.setVelocity(p, p.getPitch(), p.getYaw(), 0f, 1.1f, 1f);
            w.spawnEntity(g);
        }
        p.getItemCooldownManager().set(this, 20);
        if (!p.getAbilities().creativeMode) st.decrement(1);
        return TypedActionResult.success(st, w.isClient);
    }

    @Override
    public void appendTooltip(ItemStack st, @Nullable World w, List<Text> list, TooltipContext ctx) {
        list.add(Text.translatable(tip).formatted(Formatting.GRAY));
    }
}
