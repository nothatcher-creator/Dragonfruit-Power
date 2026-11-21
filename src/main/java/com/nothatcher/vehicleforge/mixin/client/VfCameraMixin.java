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
        if (!thirdPerson || focused == null) return;
        VfCameraAccessor acc = (VfCameraAccessor) this;
        if (com.nothatcher.vehicleforge.client.IsoCam.active()) {
            isometric(acc, focused, tickDelta);
            return;
        }
        if (!(focused.getVehicle() instanceof VehicleEntity v)) return;
        double x = MathHelper.lerp(tickDelta, focused.prevX, focused.getX());
        double y = MathHelper.lerp(tickDelta, focused.prevY, focused.getY())
                + MathHelper.lerp(tickDelta, acc.vehicleforge$getLastCameraY(), acc.vehicleforge$getCameraY());
        double z = MathHelper.lerp(tickDelta, focused.prevZ, focused.getZ());
        acc.vehicleforge$setPos(x, y + v.type.height * 0.35 * v.scaleNow, z);
        acc.vehicleforge$moveBy(-acc.vehicleforge$clipToSpace(v.type.cameraDistance * Math.max(0.35f, v.scaleNow)), 0, 0);
    }

    /** the isometric view: fixed diagonal and pitch, camera well back; the cut (IsoCam.place) clears the way */
    private static void isometric(VfCameraAccessor acc, Entity focused, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, focused.prevX, focused.getX());
        double feet = MathHelper.lerp(tickDelta, focused.prevY, focused.getY());
        double y = feet + MathHelper.lerp(tickDelta, acc.vehicleforge$getLastCameraY(), acc.vehicleforge$getCameraY());
        double z = MathHelper.lerp(tickDelta, focused.prevZ, focused.getZ());
        Entity root = focused.getRootVehicle();
        double ground = root == focused ? feet : MathHelper.lerp(tickDelta, root.prevY, root.getY());
        if (focused.getVehicle() instanceof VehicleEntity v) y += v.type.height * 0.35 * v.scaleNow;
        float yaw = com.nothatcher.vehicleforge.client.IsoCam.frameYaw(focused, tickDelta);
        acc.vehicleforge$setRotation(yaw, com.nothatcher.vehicleforge.client.IsoCam.PITCH);
        acc.vehicleforge$setPos(x, y, z);
        double back = com.nothatcher.vehicleforge.client.IsoCam.place(focused.getWorld(), new net.minecraft.util.math.Vec3d(x, y, z), yaw, Math.max(0.5, y - ground));
        acc.vehicleforge$moveBy(-back, 0, 0);
    }
}
