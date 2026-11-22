package com.nothatcher.outbreak.mixin.client;

import com.nothatcher.outbreak.client.CharacterClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** players who picked a survivor look in character creation are drawn with it (body, first-person arm and all) */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void outbreak$survivorLook(CallbackInfoReturnable<Identifier> cir) {
        Identifier look = CharacterClient.skin(((AbstractClientPlayerEntity) (Object) this).getUuid());
        if (look != null) cir.setReturnValue(look);
    }
}
