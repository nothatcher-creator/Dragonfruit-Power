package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import com.nothatcher.vehicleforge.client.iso.IsoCursor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After the game picks what you're looking at, the isometric cursor overrides it. */
@Mixin(GameRenderer.class)
public abstract class VfIsoTargetMixin {
    @Inject(method = "updateTargetedEntity", at = @At("TAIL"), require = 0)
    private void vf$isoTarget(float tickDelta, CallbackInfo ci) {
        if (IsoCam.active()) IsoCursor.update(MinecraftClient.getInstance());
    }
}
