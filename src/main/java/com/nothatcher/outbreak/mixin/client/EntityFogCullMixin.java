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
        d = Math.max(d, com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd());   // other mods (an isometric camera) may push the fog back
        var b = entity.getBoundingBox();                                    // big vehicles poke out of the fog first
        double r = d + 8.0 + Math.max(entity.getWidth(), Math.max(b.getXLength(), b.getZLength()));
        // measure from the camera to the nearest point of the box, not its centre, so long buses and trains don't pop
        double cx = Math.max(b.minX, Math.min(x, b.maxX)), cz = Math.max(b.minZ, Math.min(z, b.maxZ)), cy = Math.max(b.minY, Math.min(y, b.maxY));
        double dx = cx - x, dy = cy - y, dz = cz - z;
        if (dx * dx + dy * dy + dz * dz > r * r) cir.setReturnValue(false);
    }
}
