package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import com.nothatcher.vehicleforge.client.iso.IsoCursor;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** In the isometric view, dragging moves the cursor instead of turning your head. */
@Mixin(Mouse.class)
public abstract class VfIsoMouseMixin {
    @Redirect(method = "updateMouse", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void vf$isoCursor(ClientPlayerEntity player, double dx, double dy) {
        if (IsoCam.active()) IsoCursor.move(dx, dy);
        else player.changeLookDirection(dx, dy);
    }
}
