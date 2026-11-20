package com.nothatcher.vehicleforge.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * A fourth F5 view: true isometric. The camera looks down at 35.26 degrees along a diagonal, with an orthographic
 * projection (no perspective: things don't shrink with distance, like a classic isometric game).
 *
 * The camera sits a short way back and is pulled in by walls and ceilings like the normal third-person camera.
 * Because the projection is orthographic, pulling it in doesn't zoom anything; it just moves the near clipping
 * plane, which slices away whatever is between the camera and you (the roof over your head, the tower in front).
 *
 * It turns in 90-degree steps to keep the diagonal closest to the way you're facing, so forward on the keys stays
 * roughly up the screen. Zoom with the zoom keys (= and - by default).
 */
public final class IsoCam {
    /** true isometric: atan(1 / sqrt 2) */
    public static final float PITCH = 35.264f;
    /** how far back the camera sits before walls pull it in */
    public static final double DIST = 28.0;
    private static final float MIN_ZOOM = 16f, MAX_ZOOM = 64f;

    private static boolean active;
    private static float targetYaw = 45f, yaw = 45f;
    private static float zoomTarget = 20f, zoom = 20f;
    private static long lastNanos;
    /** how far the camera actually ended up from you this frame (fog is pushed back by this much) */
    public static float camDist = (float) DIST;

    private static KeyBinding zoomIn, zoomOut;

    private IsoCam() {}

    public static void registerKeys() {
        zoomIn = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.iso_zoom_in", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, "category.vehicleforge"));
        zoomOut = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.iso_zoom_out", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, "category.vehicleforge"));
    }

    /** is the isometric view in use right now? */
    public static boolean active() {
        if (!active) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world != null && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK;
    }

    /** half the height of the view, in blocks */
    public static float halfHeight() {
        return zoom;
    }

    /**
     * F5: first person, behind, in front, isometric, then back to first person. (The isometric view runs on the
     * "behind" perspective, so the game draws your character.)
     */
    public static void cycle(GameOptions options, Perspective next) {
        if (active) {
            active = false;
            options.setPerspective(Perspective.FIRST_PERSON);
            return;
        }
        if (options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            enter();
            options.setPerspective(Perspective.THIRD_PERSON_BACK);
            return;
        }
        options.setPerspective(next);
    }

    private static void enter() {
        active = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity e = mc.getCameraEntity();
        targetYaw = e == null ? 45f : nearestDiagonal(e.getYaw());
        yaw = targetYaw;
        zoom = zoomTarget;
        lastNanos = 0;
        if (mc.inGameHud != null) {
            mc.inGameHud.setOverlayMessage(Text.translatable("msg.vehicleforge.iso_on",
                    zoomIn.getBoundKeyLocalizedText(), zoomOut.getBoundKeyLocalizedText()).formatted(Formatting.GRAY), false);
        }
    }

    private static float nearestDiagonal(float y) {
        return Math.round((y - 45f) / 90f) * 90f + 45f;
    }

    /** client tick: zoom keys, and drop out if something else changed the perspective */
    public static void tick(MinecraftClient mc) {
        if (active && (mc.world == null || mc.options.getPerspective() != Perspective.THIRD_PERSON_BACK)) active = false;
        while (zoomIn.wasPressed()) if (active) zoomTarget = Math.max(MIN_ZOOM, zoomTarget / 1.25f);
        while (zoomOut.wasPressed()) if (active) zoomTarget = Math.min(MAX_ZOOM, zoomTarget * 1.25f);
    }

    /** per frame: ease the turn and the zoom */
    public static float frameYaw(Entity focused, float tickDelta) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        float facing = focused.getYaw(tickDelta);
        // a little past the halfway point before it turns, so it doesn't flick back and forth
        if (Math.abs(MathHelper.wrapDegrees(facing - targetYaw)) > 60f) targetYaw = nearestDiagonal(facing);
        float k = dt <= 0 ? 1f : Math.min(1f, dt * 7f);
        yaw += MathHelper.wrapDegrees(targetYaw - yaw) * k;
        zoom += (zoomTarget - zoom) * k;
        return yaw;
    }
}
