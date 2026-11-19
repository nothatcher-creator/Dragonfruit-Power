package com.nothatcher.outbreak.mixin.client;

import com.nothatcher.outbreak.client.OutbreakClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** hunting rifle scope: sneak to look down it */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class ClientPlayerFovMixin {
    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void outbreak$scope(CallbackInfoReturnable<Float> cir) {
        if (OutbreakClient.scoped((AbstractClientPlayerEntity) (Object) this)) cir.setReturnValue(0.22f);
    }
}
