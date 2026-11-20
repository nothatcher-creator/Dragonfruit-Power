package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla backs the culling frustum up until the camera's whole 8-block cell is in view. That's for perspective
 * cameras; with an orthographic one the view doesn't widen as it backs up, so it could back up forever. Skip it.
 */
@Mixin(Frustum.class)
public abstract class VfIsoFrustumMixin {
    @Inject(method = "coverBoxAroundSetPosition", at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$isoNoRecede(int boxSize, CallbackInfoReturnable<Frustum> cir) {
        if (IsoCam.active()) cir.setReturnValue((Frustum) (Object) this);
    }
}
