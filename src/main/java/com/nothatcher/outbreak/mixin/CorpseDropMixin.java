package com.nothatcher.outbreak.mixin;

import com.nothatcher.outbreak.CorpseEntity;
import com.nothatcher.outbreak.Corpses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A zombie's drops are collected while it dies, and when the dying animation ends it becomes a corpse holding them
 * (instead of vanishing in a puff).
 */
@Mixin(LivingEntity.class)
public abstract class CorpseDropMixin implements Corpses.Holder {
    @Unique
    private List<ItemStack> outbreak$loot;

    @Override
    public List<ItemStack> outbreak$loot() {
        return outbreak$loot;
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private void outbreak$collect(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!Corpses.wants(self)) return;
        outbreak$loot = new ArrayList<>();
        Corpses.capturing = self;
    }

    @Inject(method = "drop", at = @At("RETURN"))
    private void outbreak$collected(DamageSource source, CallbackInfo ci) {
        if (Corpses.capturing == (Object) this) Corpses.capturing = null;
    }

    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    private void outbreak$lieDown(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient || self.isRemoved() || self.deathTime < 19 || !(self instanceof ZombieEntity z)) return;
        if (outbreak$loot == null && !Corpses.wants(self)) return;
        CorpseEntity.of(z, outbreak$loot == null ? List.of() : outbreak$loot);
        self.remove(Entity.RemovalReason.KILLED);
        ci.cancel();
    }
}
