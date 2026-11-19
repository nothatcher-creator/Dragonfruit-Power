package com.nothatcher.outbreak.mixin;

import com.nothatcher.outbreak.InfectionEffect;
import com.nothatcher.outbreak.NoiseGoal;
import com.nothatcher.outbreak.Outbreak;
import com.nothatcher.outbreak.OutbreakEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity {
    protected ZombieEntityMixin(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    /** the dead don't burn in the sun */
    @Inject(method = "burnsInDaylight", at = @At("HEAD"), cancellable = true)
    private void outbreak$noSunburn(CallbackInfoReturnable<Boolean> cir) {
        if (this.getWorld().getGameRules().getBoolean(Outbreak.DAYLIGHT)) cir.setReturnValue(false);
    }

    /** they come to noise */
    @Inject(method = "initCustomGoals", at = @At("TAIL"))
    private void outbreak$noiseGoal(CallbackInfo ci) {
        this.goalSelector.add(4, new NoiseGoal((ZombieEntity) (Object) this));
    }

    /** bites can infect: less likely through armour */
    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void outbreak$bite(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || this.getWorld().isClient) return;
        if (!(target instanceof PlayerEntity p) || p.getAbilities().creativeMode || p.isDead()) return;
        if (!this.getWorld().getGameRules().getBoolean(Outbreak.INFECTION) || p.hasStatusEffect(OutbreakEffects.INFECTION)) return;
        float chance = 0.10f * Math.max(0.25f, 1f - p.getArmor() / 26f);
        if (this.random.nextFloat() >= chance) return;
        p.addStatusEffect(new StatusEffectInstance(OutbreakEffects.INFECTION, InfectionEffect.DURATION, 0, false, false, true));
        p.sendMessage(Text.translatable("msg.outbreak.bitten").formatted(Formatting.DARK_RED), true);
    }
}
