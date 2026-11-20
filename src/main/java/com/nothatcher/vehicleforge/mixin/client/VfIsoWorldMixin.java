package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The sky dome and clouds are built for a perspective camera and smear across an orthographic view, so the
 * isometric view leaves them out: what you see above the horizon is the plain sky colour.
 */
@Mixin(WorldRenderer.class)
public abstract class VfIsoWorldMixin {
    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$noSky(MatrixStack matrices, Matrix4f projection, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        if (IsoCam.active()) ci.cancel();
    }

    @Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$noClouds(MatrixStack matrices, Matrix4f projection, float tickDelta, double x, double y, double z, CallbackInfo ci) {
        if (IsoCam.active()) ci.cancel();
    }
}
