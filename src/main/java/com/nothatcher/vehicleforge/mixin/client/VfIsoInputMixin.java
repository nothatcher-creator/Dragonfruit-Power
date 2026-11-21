package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.iso.IsoInput;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Screen-relative movement (and auto-walking) in the isometric view. */
@Mixin(KeyboardInput.class)
public abstract class VfIsoInputMixin {
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void vf$isoMove(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        IsoInput.apply((KeyboardInput) (Object) this);
    }
}
