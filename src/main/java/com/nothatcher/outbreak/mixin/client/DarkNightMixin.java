package com.nothatcher.outbreak.mixin.client;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Zomboid nights: once the sun's down, the sky gives almost no light, so the dark is properly dark and lamps,
 * fires and flashlights matter. Daylight is unchanged; dusk and dawn fade smoothly. Lightning still lights it up.
 */
@Mixin(LightmapTextureManager.class)
public abstract class DarkNightMixin {
    @Redirect(method = "update", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getSkyBrightness(F)F"))
    private float outbreak$darkNights(ClientWorld world, float tickDelta) {
        float f = world.getSkyBrightness(tickDelta);                  // about 0.2 at night up to 1.0 by day
        if (!world.getDimension().hasSkyLight()) return f;
        float t = MathHelper.clamp((f - 0.2f) / 0.6f, 0f, 1f);
        t = t * t * (3f - 2f * t);
        return f * MathHelper.lerp(t, 0.12f, 1f);
    }
}
