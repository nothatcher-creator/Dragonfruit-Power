package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Isometric view: swap the perspective projection for an orthographic one. */
@Mixin(GameRenderer.class)
public abstract class VfIsoProjectionMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true, require = 0)
    private void vf$ortho(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (!IsoCam.active()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float aspect = (float) mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());
        float hh = IsoCam.halfHeight(), hw = hh * aspect;
        float range = Math.max(256f, ((GameRenderer) (Object) this).getFarPlaneDistance()) + IsoCam.camDist;
        // orthographic, but with the near plane replaced by IsoCam's cut: depth (-z) is clipped where it's less
        // than cut0 + cutSlope * y, and the far plane sits `range` beyond that
        Matrix4f m = new Matrix4f().setOrtho(-hw, hw, -hh, hh, IsoCam.cut0, IsoCam.cut0 + range);
        m.m12(-2f * IsoCam.cutSlope / range);
        m.m22(-2f / range);
        m.m32(-2f * IsoCam.cut0 / range - 1f);
        cir.setReturnValue(m);
    }
}
