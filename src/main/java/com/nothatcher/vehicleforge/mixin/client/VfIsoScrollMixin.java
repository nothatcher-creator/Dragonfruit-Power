package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In the isometric view the mouse wheel zooms instead of changing hotbar slot (number keys still do that). */
@Mixin(Mouse.class)
public abstract class VfIsoScrollMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$isoZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (window != mc.getWindow().getHandle() || mc.currentScreen != null || mc.getOverlay() != null || !IsoCam.active()) return;
        IsoCam.scroll(vertical == 0 ? horizontal : vertical);
        ci.cancel();
    }
}
