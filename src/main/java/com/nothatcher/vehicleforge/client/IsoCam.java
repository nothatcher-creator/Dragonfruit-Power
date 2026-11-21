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
 * With no perspective the camera can sit anywhere along its line without changing the picture, so it stays back
 * out of the way and a clipping cut (see {@link #place}) slices off whatever is between it and you: the roof over
 * your head, the near side of a tower. The cut leans back toward level as it moves in, so the street at your feet
 * is never cut away.
 *
 * It holds its diagonal; the rotate keys ([ and ]) turn it a quarter at a time, and zoom is = and -. In this view
 * you steer a cursor rather than your head, movement is relative to the screen, and clicks act where the cursor
 * points (see the iso package).
 */
public final class IsoCam {
    /** true isometric: atan(1 / sqrt 2) */
    public static final float PITCH = 35.264f;
    private static final float COS_P = (float) Math.cos(Math.toRadians(PITCH)), SIN_P = (float) Math.sin(Math.toRadians(PITCH));
    private static final float MIN_ZOOM = 16f, MAX_ZOOM = 64f;

    private static boolean active;
    private static float targetYaw = 45f, yaw = 45f;
    private static float zoomTarget = 20f, zoom = 20f;
    private static long lastNanos;
    /** how far the camera sits back from your eyes this frame (fog is pushed back by this much) */
    public static float camDist = 30f;
    /**
     * The cut that slices away whatever is between the camera and you, as used by the projection: depth from the
     * camera at the middle of the screen, and how much deeper it gets per block up the screen. With nothing in the
     * way it's square to the view and well short of the street. When something is in the way it moves in to just
     * short of it and leans back toward level, so it takes off what's above and in front of you (roofs, upper floors,
     * the near side of a tower) without ever dipping into the ground at your feet.
     */
    public static float cut0 = 2f, cutSlope = 0f;
    private static float cutE = 0f, lastDt = 0f;

    private static KeyBinding zoomIn, zoomOut, rotLeft, rotRight;

    private IsoCam() {}

    public static void registerKeys() {
        zoomIn = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.iso_zoom_in", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, "category.vehicleforge"));
        zoomOut = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.iso_zoom_out", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, "category.vehicleforge"));
        rotLeft = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.iso_rotate_left", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, "category.vehicleforge"));
        rotRight = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.iso_rotate_right", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, "category.vehicleforge"));
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
        cutE = 0f;
        com.nothatcher.vehicleforge.client.iso.IsoCursor.reset();
        if (mc.inGameHud != null) {
            mc.inGameHud.setOverlayMessage(Text.translatable("msg.vehicleforge.iso_on",
                    zoomIn.getBoundKeyLocalizedText(), zoomOut.getBoundKeyLocalizedText(),
                    rotLeft.getBoundKeyLocalizedText(), rotRight.getBoundKeyLocalizedText()).formatted(Formatting.GRAY), false);
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
        while (rotLeft.wasPressed()) if (active) targetYaw -= 90f;
        while (rotRight.wasPressed()) if (active) targetYaw += 90f;
        if (!active) {
            com.nothatcher.vehicleforge.client.iso.IsoWalker.cancel();
            com.nothatcher.vehicleforge.client.iso.IsoActions.stopBreaking();
        }
    }

    /**
     * Per frame: where the camera goes and where the cut sits. {@code eye} is where you look from;
     * {@code eyeAboveGround} is how high that is above what you're standing on (or the vehicle's bottom).
     * Returns how far back from the eye to put the camera.
     */
    public static double place(net.minecraft.world.World w, net.minecraft.util.math.Vec3d eye, float camYaw, double eyeAboveGround) {
        float hh = zoom;
        // how much nearer the camera the street at the bottom of the screen is than your eyes, plus a margin
        float ground = (float) Math.max(1.5, (hh * COS_P - eyeAboveGround) / SIN_P + 1.5);
        // the camera sits well back, so buildings that aren't in the way are left whole (as in Zomboid); not so
        // far that you end up near the fog at the edge of the render distance
        int view = net.minecraft.client.MinecraftClient.getInstance().options.getClampedViewDistance() * 16;
        float far = Math.max(ground + 4f, Math.min(96f, view * 0.45f));
        float back = far + 2f;
        // look back toward the camera for anything solid in the way
        net.minecraft.util.math.Vec3d toCam = net.minecraft.util.math.Vec3d.fromPolar(PITCH, camYaw).multiply(-1);
        net.minecraft.util.math.BlockPos.Mutable at = new net.minecraft.util.math.BlockPos.Mutable();
        float hit = Float.MAX_VALUE;
        long last = Long.MIN_VALUE;
        for (float t = 0.3f; t < far; t += 0.25f) {
            at.set(eye.x + toCam.x * t, eye.y + toCam.y * t, eye.z + toCam.z * t);
            long key = at.asLong();
            if (key == last) continue;
            last = key;
            if (blocks(w, at)) {
                hit = t;
                break;
            }
        }
        // nothing in the way: the cut stays back by the camera and nothing is sliced
        float target = hit == Float.MAX_VALUE ? far : Math.min(Math.max(0.3f, hit - 0.75f), far);
        // move in at once (so you're never hidden), ease back out (so walking past things doesn't flicker)
        cutE = target < cutE || cutE <= 0 ? target : Math.min(target, cutE + (target - cutE) * Math.min(1f, lastDt * 4f) + lastDt * 6f);
        float e = cutE;
        // close in, the cut leans back so the street at your feet is never cut away; further out it can stand square
        cutSlope = e < ground ? (ground - e) / hh : 0f;
        cut0 = back - e;
        camDist = back;
        return back;
    }

    /** solid things you can't see through; leaves, glass, posts and the like don't count */
    private static boolean blocks(net.minecraft.world.World w, net.minecraft.util.math.BlockPos pos) {
        net.minecraft.block.BlockState st = w.getBlockState(pos);
        if (st.isAir() || !st.isOpaque() || st.isIn(net.minecraft.registry.tag.BlockTags.LEAVES) || st.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) return false;
        return !st.getCollisionShape(w, pos).isEmpty();
    }

    /** the camera's current yaw (movement is relative to it) */
    public static float cameraYaw() {
        return yaw;
    }

    /** per frame: ease the turn and the zoom */
    public static float frameYaw(Entity focused, float tickDelta) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        lastDt = dt;
        // the camera holds its diagonal (you face the cursor, so following your facing would spin it); the rotate
        // keys turn it a quarter at a time
        float k = dt <= 0 ? 1f : Math.min(1f, dt * 7f);
        yaw += MathHelper.wrapDegrees(targetYaw - yaw) * k;
        zoom += (zoomTarget - zoom) * k;
        return yaw;
    }
}
