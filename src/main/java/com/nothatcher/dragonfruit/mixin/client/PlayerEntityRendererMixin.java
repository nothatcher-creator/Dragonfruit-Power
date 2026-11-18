package com.nothatcher.dragonfruit.mixin.client;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.client.ClientPowerData;
import com.nothatcher.dragonfruit.client.render.BeastRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Players in a beast form are drawn as the beast instead of the player model. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void dragonfruit$beast(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
                                   VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        FruitType beast = ClientPowerData.beastOf(player);
        if (beast == FruitType.NONE) return;
        BeastRenderer.render(player, beast, tickDelta, matrices, vcp, light);
        ci.cancel();
    }
}
