package com.nothatcher.dragonfruit.client;

import com.nothatcher.dragonfruit.FruitType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Screen shake, FOV punches and white flashes. Applied through GameRendererMixin and the HUD. */
public final class CameraFx {
    private static float shakeIntensity;
    private static int shakeLeft, shakeDuration;
    private static float fovKick, fovHold;
    private static int fovHoldTicks;
    private static float flash, prevFlash;
    private static int clock;
    private static float speedLines;
    private static int speedLinesLeft, speedLinesDuration;

    private CameraFx() {}

    public static void shake(float intensity, int duration) {
        float current = currentShake(0);
        if (intensity >= current) {
            shakeIntensity = Math.min(intensity, 3f);
            shakeDuration = Math.max(1, duration);
            shakeLeft = shakeDuration;
        }
    }

    public static void kickFov(float amount) {
        kickFov(amount, 0);
    }

    public static void kickFov(float amount, int holdTicks) {
        if (Math.abs(amount) > Math.abs(fovKick)) fovKick = amount;
        fovHold = amount;
        fovHoldTicks = holdTicks;
    }

    public static void flash(float amount) {
        flash = Math.min(1f, Math.max(flash, amount));
    }

    public static void speedLines(float intensity, int duration) {
        speedLines = Math.max(speedLines * speedLinesFraction(0), intensity);
        speedLinesDuration = Math.max(1, duration);
        speedLinesLeft = speedLinesDuration;
    }

    private static float speedLinesFraction(float td) {
        if (speedLinesLeft <= 0) return 0;
        return Math.max(0, (speedLinesLeft - td) / speedLinesDuration);
    }

    /** 0..1 strength of the anime speed lines overlay. */
    public static float speedLinesAlpha(float td) {
        return speedLines * speedLinesFraction(td);
    }

    public static float fovKickAmount() {
        return fovKick;
    }

    public static void tick() {
        clock++;
        if (speedLinesLeft > 0) speedLinesLeft--;
        if (shakeLeft > 0) shakeLeft--;
        if (fovHoldTicks > 0) {
            fovHoldTicks--;
            fovKick = fovKick + (fovHold - fovKick) * 0.5f;
        } else {
            fovKick *= 0.78f;
            if (Math.abs(fovKick) < 0.002f) fovKick = 0;
        }
        prevFlash = flash;
        flash *= 0.8f;
        if (flash < 0.01f) flash = 0;
    }

    public static void reset() {
        shakeLeft = 0;
        fovKick = 0;
        fovHoldTicks = 0;
        flash = prevFlash = 0;
        speedLinesLeft = 0;
    }

    private static float currentShake(float td) {
        if (shakeLeft <= 0) return 0;
        return shakeIntensity * Math.max(0, (shakeLeft - td) / shakeDuration);
    }

    /** Called from the camera tilt hook; rotates the view a little. */
    public static void applyShake(MatrixStack matrices, float td) {
        float s = currentShake(td);
        if (s <= 0.001f) return;
        float t = clock + td;
        float roll = (float) (Math.sin(t * 2.9) * 2.6 + Math.sin(t * 7.3) * 1.1) * s;
        float pitch = (float) (Math.cos(t * 3.7) * 1.7 + Math.sin(t * 9.1) * 0.6) * s;
        float yaw = (float) (Math.sin(t * 4.3 + 1.3) * 1.2) * s;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
    }

    public static double fovMultiplier() {
        double m = 1.0 + fovKick;
        if (ClientPowerData.fruit == FruitType.RUBBER && ClientPowerData.buffActive()) m += 0.06;
        return Math.max(0.5, Math.min(1.8, m));
    }

    public static float flashAlpha(float td) {
        return prevFlash + (flash - prevFlash) * td;
    }

    public static int clock() {
        return clock;
    }
}
