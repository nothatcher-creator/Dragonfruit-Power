package com.nothatcher.vehicleforge.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fog is measured from the camera, which in the isometric view hangs well back from you. Short atmospheric fog
 * (rain, Outbreak's haze) is pushed back by that distance so it sits around you rather than swallowing you.
 * Runs after other mods' fog tweaks (higher priority = applied later).
 */
@Mixin(value = BackgroundRenderer.class, priority = 2000)
public abstract class VfIsoFogMixin {
    @Inject(method = "applyFog", at = @At("TAIL"), require = 0)
    private static void vf$isoFog(Camera camera, BackgroundRenderer.FogType type, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (thickFog || !IsoCam.active()) return;
        float end = RenderSystem.getShaderFogEnd();
        if (end >= viewDistance * 0.9f) return;                             // ordinary distance fog: leave it at the edge of the world
        float off = IsoCam.camDist;
        RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() + off);
        RenderSystem.setShaderFogEnd(end + off);
    }
}
