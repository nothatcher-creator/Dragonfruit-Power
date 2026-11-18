package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.ModEffects;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draw shrunk creatures (and you) tiny. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;scale(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V", shift = At.Shift.AFTER), require = 0)
    private void vf$shrink(LivingEntity entity, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        float s = ModEffects.scaleOf(entity);
        if (s < 1f) ms.scale(s, s, s);
    }
}
