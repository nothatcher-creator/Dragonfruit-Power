package com.nothatcher.dragonfruit.mixin.client;

import com.nothatcher.dragonfruit.client.vfx.VfxManager;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third person: hides the model's arm while the rubber arm is stretched out of that shoulder,
 * and raises the right arm forward while charging a hand-held orb (Light Cannon, Quake Bubble...).
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void dragonfruit$armPose(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress,
                                     float headYaw, float headPitch, CallbackInfo ci) {
        PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;
        int id = entity.getId();
        if (VfxManager.armActive(id, 1)) {
            model.rightArm.visible = false;
            model.rightSleeve.visible = false;
        } else if (VfxManager.charging(id)) {
            model.rightArm.pitch = -(float) Math.PI / 2f + model.head.pitch;
            model.rightArm.yaw = -0.1f + model.head.yaw;
            model.rightSleeve.copyTransform(model.rightArm);
        }
        if (VfxManager.armActive(id, -1)) {
            model.leftArm.visible = false;
            model.leftSleeve.visible = false;
        }
    }
}
