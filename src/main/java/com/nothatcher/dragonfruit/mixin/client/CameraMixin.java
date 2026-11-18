package com.nothatcher.dragonfruit.mixin.client;

import com.nothatcher.dragonfruit.client.PowerCam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the vanilla back-view third-person position with the over-the-shoulder Power Cam. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void dragonfruit$powerCam(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                      float tickDelta, CallbackInfo ci) {
        if (!thirdPerson || inverseView || focusedEntity == null || !PowerCam.active()) return;
        Camera self = (Camera) (Object) this;
        CameraAccessor acc = (CameraAccessor) this;
        double eyeY = MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY())
                + MathHelper.lerp(tickDelta, acc.dragonfruit$getLastCameraY(), acc.dragonfruit$getCameraY());
        Vec3d eye = new Vec3d(MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()), eyeY,
                MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ()));
        Vec3d pos = PowerCam.position(area, focusedEntity, eye, self.getPitch(), self.getYaw());
        acc.dragonfruit$setPos(pos.x, pos.y, pos.z);
    }
}
