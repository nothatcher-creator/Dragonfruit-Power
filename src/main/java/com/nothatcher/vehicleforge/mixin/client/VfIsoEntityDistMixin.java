package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entities stop drawing past a distance from the camera that depends on their size. The isometric camera sits far
 * back from you, so zombies and cars near you were dropping out of view. In that view, measure from the player.
 */
@Mixin(Entity.class)
public abstract class VfIsoEntityDistMixin {
    @Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void vf$isoDistance(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!IsoCam.active()) return;
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        Entity self = (Entity) (Object) this;
        cir.setReturnValue(self.shouldRender(self.squaredDistanceTo(p.getX(), p.getEyeY(), p.getZ())));
    }
}
