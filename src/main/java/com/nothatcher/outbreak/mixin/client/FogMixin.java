package com.nothatcher.outbreak.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nothatcher.outbreak.client.OutbreakClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Eerie ground fog in the overworld: a grey-green haze by day that closes in to a few dozen blocks, thick and
 * near-black at night, heavier in the rain. Underwater, lava, blindness and darkness fog are left alone.
 */
@Mixin(BackgroundRenderer.class)
public abstract class FogMixin {
    @Shadow
    private static float red;
    @Shadow
    private static float green;
    @Shadow
    private static float blue;

    private static boolean outbreak$active(Camera camera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!OutbreakClient.fogOn || mc.world == null || mc.world.getRegistryKey() != World.OVERWORLD) return false;
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return false;
        return !(camera.getFocusedEntity() instanceof LivingEntity le
                && (le.hasStatusEffect(StatusEffects.BLINDNESS) || le.hasStatusEffect(StatusEffects.DARKNESS) || le.hasStatusEffect(StatusEffects.NIGHT_VISION)));
    }

    /** 0 at noon, 1 at midnight */
    private static float outbreak$night(ClientWorld w, float td) {
        return MathHelper.clamp((0.85f - w.getSkyBrightness(td)) / 0.6f, 0f, 1f);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private static void outbreak$fogColor(Camera camera, float td, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (!outbreak$active(camera)) return;
        float night = outbreak$night(world, td);
        float rain = world.getRainGradient(td);
        float tr = MathHelper.lerp(night, 0.56f, 0.04f), tg = MathHelper.lerp(night, 0.60f, 0.05f), tb = MathHelper.lerp(night, 0.56f, 0.05f);
        float mix = 0.55f + rain * 0.2f;
        red = MathHelper.lerp(mix, red, tr);
        green = MathHelper.lerp(mix, green, tg);
        blue = MathHelper.lerp(mix, blue, tb);
        RenderSystem.clearColor(red, green, blue, 0f);
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void outbreak$fogDistance(Camera camera, BackgroundRenderer.FogType type, float viewDistance, boolean thick, float td, CallbackInfo ci) {
        if (thick || !outbreak$active(camera)) return;
        ClientWorld w = MinecraftClient.getInstance().world;
        float night = outbreak$night(w, td);
        float thickness = 1f + w.getRainGradient(td) * 0.35f;
        float end = MathHelper.lerp(night, 72f, 34f) / thickness;
        end = Math.min(end, viewDistance);
        if (type == BackgroundRenderer.FogType.FOG_TERRAIN) {
            RenderSystem.setShaderFogStart(MathHelper.lerp(night, 4f, 0f));
            RenderSystem.setShaderFogEnd(end);
        } else {
            // haze the sky toward the fog colour too, so the horizon disappears
            RenderSystem.setShaderFogStart(0f);
            RenderSystem.setShaderFogEnd(end * 0.6f);
        }
    }
}
