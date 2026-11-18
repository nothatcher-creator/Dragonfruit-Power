package com.nothatcher.dragonfruit.content;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.net.Vfx;
import com.nothatcher.dragonfruit.power.PowerManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Right-click a player (or mob) to cuff them: fruit powers sealed for 60 seconds, slowed and weakened. */
public class SeastoneCuffsItem extends Item {
    public SeastoneCuffsItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity target, Hand hand) {
        if (user.getWorld().isClient) return ActionResult.SUCCESS;
        int ticks = 1200;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 2));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ticks, 2));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, ticks, 1));
        if (target instanceof ServerPlayerEntity sp) {
            PowerManager.seal(sp, ticks);
            if (PowerManager.get(sp) != FruitType.NONE) {
                sp.sendMessage(Text.translatable("msg.dragonfruit.cuffed").formatted(Formatting.DARK_AQUA), true);
            }
        }
        ServerWorld w = (ServerWorld) user.getWorld();
        Vfx.burst(w, target.getPos().add(0, 1, 0), Vec3d.ZERO, Vfx.B_ICE_SHATTER, 0.5f);
        w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.5f, 0.6f);
        if (!user.getAbilities().creativeMode) stack.decrement(1);
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.dragonfruit.cuffs").formatted(Formatting.GRAY));
    }
}
