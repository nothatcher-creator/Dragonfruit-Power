package com.nothatcher.vehicleforge.mixin;

import com.nothatcher.vehicleforge.ModEffects;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players have their own fixed sizes and eye heights, so they need shrinking separately. */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true, require = 0)
    private void vf$shrinkDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        float s = ModEffects.scaleOf((PlayerEntity) (Object) this);
        if (s < 1f) cir.setReturnValue(cir.getReturnValue().scaled(s));
    }

    @Inject(method = "getActiveEyeHeight", at = @At("RETURN"), cancellable = true, require = 0)
    private void vf$shrinkEyes(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        float s = ModEffects.scaleOf((PlayerEntity) (Object) this);
        if (s < 1f) cir.setReturnValue(cir.getReturnValue() * s);
    }
}
