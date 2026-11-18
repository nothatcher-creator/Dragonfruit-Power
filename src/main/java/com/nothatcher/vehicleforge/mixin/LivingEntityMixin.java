package com.nothatcher.vehicleforge.mixin;

import com.nothatcher.vehicleforge.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shrunk creatures get a smaller hitbox (and, through it, lower eyes). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Unique
    private float vf$lastScale = 1f;

    protected LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void vf$shrinkDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        float s = ModEffects.scaleOf((LivingEntity) (Object) this);
        if (s < 1f) cir.setReturnValue(cir.getReturnValue().scaled(s));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vf$updateSize(CallbackInfo ci) {
        float s = ModEffects.scaleOf((LivingEntity) (Object) this);
        if (Math.abs(s - vf$lastScale) > 0.01f) {
            vf$lastScale = s;
            calculateDimensions();
        }
    }
}
