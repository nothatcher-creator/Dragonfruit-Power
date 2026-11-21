package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.iso.IsoActions;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Left and right clicks in the isometric view (see IsoActions). */
@Mixin(MinecraftClient.class)
public abstract class VfIsoClickMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$isoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (IsoActions.onAttack((MinecraftClient) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$isoUse(CallbackInfo ci) {
        if (IsoActions.onUse((MinecraftClient) (Object) this)) ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true, require = 0)
    private void vf$isoDig(boolean breaking, CallbackInfo ci) {
        if (IsoActions.onBreakingTick((MinecraftClient) (Object) this, breaking)) ci.cancel();
    }
}
