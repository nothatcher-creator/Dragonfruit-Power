package com.nothatcher.vehicleforge.client.iso;

import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Movement in the isometric view is relative to the screen, as in an isometric game: forward on the keys (or the
 * joystick) is up the screen, whichever way you're facing. While you walk you turn to face where you're going, so
 * sprinting works; hold attack or use and you keep facing the cursor and strafe instead.
 */
public final class IsoInput {
    /** were you moving last tick? (the cursor only turns you while you stand still or aim) */
    public static boolean moving;

    private IsoInput() {}

    public static boolean aiming(MinecraftClient mc) {
        return mc.options.attackKey.isPressed() || mc.options.useKey.isPressed();
    }

    /** after the keys are read: re-aim the movement (or, while auto-walking, take it over) */
    public static void apply(Input in) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (!IsoCam.active() || p == null) {
            moving = false;
            return;
        }
        boolean manual = in.pressingForward || in.pressingBack || in.pressingLeft || in.pressingRight;
        if (manual) {
            IsoWalker.cancel();
            IsoActions.stopBreaking();
        }
        if (IsoWalker.active()) {
            IsoWalker.steer(mc, p, in);
            moving = true;
            return;
        }
        if (p.hasVehicle()) {                              // vehicles read the raw keys themselves
            moving = false;
            return;
        }
        float f = in.movementForward, s = in.movementSideways;
        if (f == 0 && s == 0) {
            moving = false;
            return;
        }
        moving = true;
        float camYaw = IsoCam.cameraYaw();
        if (aiming(mc)) {
            // keep facing the cursor: turn the screen-relative input into your own frame
            double d = Math.toRadians(camYaw - p.getYaw());
            in.movementSideways = (float) (s * Math.cos(d) - f * Math.sin(d));
            in.movementForward = (float) (f * Math.cos(d) + s * Math.sin(d));
            return;
        }
        float moveYaw = camYaw + (float) Math.toDegrees(Math.atan2(-s, f));
        p.setYaw(moveYaw);
        p.setBodyYaw(moveYaw);
        p.setHeadYaw(moveYaw);
        in.movementForward = Math.min(1f, (float) Math.sqrt(f * f + s * s));
        in.movementSideways = 0f;
    }
}
