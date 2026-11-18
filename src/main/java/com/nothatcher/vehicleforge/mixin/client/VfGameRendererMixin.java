package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.VehicleCam;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** First-person vehicle camera hooks (all optional: if a hook can't attach, the game still runs). */
@Mixin(GameRenderer.class)
public abstract class VfGameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), require = 0)
    private void vf$vehicleTilt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        VehicleCam.tilt(matrices, tickDelta);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void vf$speedFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (changingFov) cir.setReturnValue(cir.getReturnValue() * VehicleCam.fovScale(tickDelta));
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$hideHands(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (VehicleCam.hideHands()) ci.cancel();
    }
}
