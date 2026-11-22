package com.nothatcher.vehicleforge.mixin;

import com.nothatcher.vehicleforge.VehicleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The driver's client runs a vehicle's physics. Vanilla re-runs each move on the server with its own collision
 * check and, when the two disagree by a few centimetres (a rotated footprint scraping a kerb, a wheel on a step),
 * throws the vehicle back to where it was and tells the client. That's what made cars jump back, vanish and turn
 * up where they'd been a moment ago. For our vehicles, trust the driver's position.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class VehicleMoveMixin {
    @Redirect(method = "onVehicleMove", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/server/world/ServerWorld;isSpaceEmpty(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Z"))
    private boolean vf$trustDriver(ServerWorld world, Entity entity, Box box) {
        if (entity instanceof VehicleEntity) return false;      // "it was already stuck": vanilla then never snaps it back
        return world.isSpaceEmpty(entity, box);
    }
}
