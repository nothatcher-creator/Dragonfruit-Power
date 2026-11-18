package com.nothatcher.dragonfruit.mixin.client;

import com.nothatcher.dragonfruit.client.CustomSkins;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Elemental form skins and your own per-fruit skins (config/dragonfruit/skins). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void dragonfruit$fruitSkin(CallbackInfoReturnable<Identifier> cir) {
        Identifier id = CustomSkins.skinFor((AbstractClientPlayerEntity) (Object) this);
        if (id != null) cir.setReturnValue(id);
    }
}
