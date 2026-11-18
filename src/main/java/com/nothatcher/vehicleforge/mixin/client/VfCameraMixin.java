package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.VehicleEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person pulls back to fit the whole vehicle (a tank or rocket needs a lot more room than Steve). */
@Mixin(Camera.class)
public abstract class VfCameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void vehicleforge$vehicleDistance(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView,
                                             float tickDelta, CallbackInfo ci) {
        if (!thirdPerson || focused == null || !(focused.getVehicle() instanceof VehicleEntity v)) return;
        VfCameraAccessor acc = (VfCameraAccessor) this;
        double x = MathHelper.lerp(tickDelta, focused.prevX, focused.getX());
        double y = MathHelper.lerp(tickDelta, focused.prevY, focused.getY())
                + MathHelper.lerp(tickDelta, acc.vehicleforge$getLastCameraY(), acc.vehicleforge$getCameraY());
        double z = MathHelper.lerp(tickDelta, focused.prevZ, focused.getZ());
        acc.vehicleforge$setPos(x, y + v.type.height * 0.35 * v.scaleNow, z);
        acc.vehicleforge$moveBy(-acc.vehicleforge$clipToSpace(v.type.cameraDistance * Math.max(0.35f, v.scaleNow)), 0, 0);
    }
}
