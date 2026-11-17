package com.nothatcher.dragonfruit.mixin.client;

import com.nothatcher.dragonfruit.client.CameraFx;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** FOV punches: rocket launch, pistol, Second Gear, etc. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void dragonfruit$fov(CallbackInfoReturnable<Double> cir) {
        double mul = CameraFx.fovMultiplier();
        if (mul != 1.0) cir.setReturnValue(cir.getReturnValue() * mul);
    }

    /** Screen shake rides on the vanilla hurt-tilt hook so it affects both world and hand. */
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"))
    private void dragonfruit$shake(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        CameraFx.applyShake(matrices, tickDelta);
    }
}
