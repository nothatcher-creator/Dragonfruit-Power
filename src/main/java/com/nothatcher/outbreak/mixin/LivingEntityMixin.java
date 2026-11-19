package com.nothatcher.outbreak.mixin;

import com.nothatcher.outbreak.OutbreakEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** milk (and anything else that clears every effect) can't wash out the infection: only antibiotics can */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private StatusEffectInstance outbreak$kept;

    @Inject(method = "clearStatusEffects", at = @At("HEAD"))
    private void outbreak$keep(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        StatusEffectInstance inf = self.getStatusEffect(OutbreakEffects.INFECTION);
        outbreak$kept = inf == null ? null : new StatusEffectInstance(inf);
    }

    @Inject(method = "clearStatusEffects", at = @At("RETURN"))
    private void outbreak$restore(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (outbreak$kept != null && self.isAlive() && !self.getWorld().isClient) self.addStatusEffect(outbreak$kept);
        outbreak$kept = null;
    }
}
