package com.nothatcher.outbreak.mixin.client;

import com.nothatcher.outbreak.client.OutbreakClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Anything standing deep inside the Outbreak fog would be drawn fully fogged, i.e. invisible, so don't draw it at
 * all. Parked cars and zombies past the fog line cost a lot to render on a phone for nothing.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityFogCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void outbreak$fogCull(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        float d = OutbreakClient.cullDist;
        if (d <= 0f || entity.isGlowing()) return;
        double r = d + 8.0 + entity.getWidth();                             // big vehicles poke out of the fog first
        if (entity.squaredDistanceTo(x, y, z) > r * r) cir.setReturnValue(false);
    }
}
