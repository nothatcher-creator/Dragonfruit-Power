package com.nothatcher.outbreak.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;

/**
 * The "Zomboid Look" colour grade: a single full-screen pass over the world (not the HUD or menus) that washes the
 * colour out, warms it toward olive, softens the contrast and adds a vignette and faint grain.
 *
 * The shader lives in the built-in Zomboid Look resource pack, so turning that pack off in Resource Packs turns
 * this off too. If the device's graphics driver can't compile it, it just stays off.
 */
public final class ZomboidGrade {
    private static final Identifier POST = new Identifier("minecraft", "shaders/post/outbreak_zomboid.json");
    private static PostEffectProcessor fx;
    private static boolean tried;
    private static int w = -1, h = -1;

    private ZomboidGrade() {}

    /** resources reloaded (pack switched on or off): build it again next frame */
    public static void reload() {
        if (fx != null) fx.close();
        fx = null;
        tried = false;
    }

    public static void render(float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        if (!tried) {
            tried = true;
            load(mc);
        }
        if (fx == null) return;
        Framebuffer fb = mc.getFramebuffer();
        if (fb.textureWidth != w || fb.textureHeight != h) {
            w = fb.textureWidth;
            h = fb.textureHeight;
            fx.setupDimensions(w, h);
        }
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        fx.render(tickDelta);
        fb.beginWrite(true);
    }

    private static void load(MinecraftClient mc) {
        if (mc.getResourceManager().getResource(POST).isEmpty()) return;     // the pack is off
        try {
            fx = new PostEffectProcessor(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), POST);
            w = h = -1;
        } catch (Exception e) {
            com.nothatcher.outbreak.Outbreak.LOG.warn("Zomboid Look colour grade unavailable on this device: {}", e.toString());
            fx = null;
        }
    }
}
