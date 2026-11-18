package com.nothatcher.dragonfruit.mixin.client;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPos")
    void dragonfruit$setPos(double x, double y, double z);

    @Accessor("cameraY")
    float dragonfruit$getCameraY();

    @Accessor("lastCameraY")
    float dragonfruit$getLastCameraY();
}
