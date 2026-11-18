package com.nothatcher.dragonfruit.mixin.client;

import com.nothatcher.dragonfruit.client.vfx.VfxManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the normal first-person hand while that arm is stretched out, so you only see the rubber arm. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void dragonfruit$hideStretchedArm(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                              float swingProgress, ItemStack item, float equipProgress,
                                              MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                              CallbackInfo ci) {
        if (com.nothatcher.dragonfruit.client.ClientPowerData.beastOf(player) != com.nothatcher.dragonfruit.FruitType.NONE) {
            ci.cancel(); // you're an animal: no human hands
            return;
        }
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == Arm.RIGHT ? 1 : -1;
        if (VfxManager.armActive(player.getId(), side)) ci.cancel();
    }
}
