package com.nothatcher.vehicleforge.mixin.client;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface VfCameraAccessor {
    @Invoker("setPos")
    void vehicleforge$setPos(double x, double y, double z);

    @Invoker("setRotation")
    void vehicleforge$setRotation(float yaw, float pitch);

    @Invoker("moveBy")
    void vehicleforge$moveBy(double x, double y, double z);

    @Invoker("clipToSpace")
    double vehicleforge$clipToSpace(double desiredDistance);

    @Accessor("cameraY")
    float vehicleforge$getCameraY();

    @Accessor("lastCameraY")
    float vehicleforge$getLastCameraY();
}
