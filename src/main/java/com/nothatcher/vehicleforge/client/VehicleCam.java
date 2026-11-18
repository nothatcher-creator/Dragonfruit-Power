package com.nothatcher.vehicleforge.client;

import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.VehicleType.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * First-person driving camera:
 * - your view leans and tilts with the vehicle (bikes lean into corners, planes bank, cars follow the road surface
 *   and squat/dive/roll on their suspension), worked out for whichever way you're looking;
 * - field of view widens a little with speed so it feels fast;
 * - engine vibration and road rumble, and a jolt when you crash;
 * - empty hands are hidden while you drive.
 */
public final class VehicleCam {
    private static float fovBoost = 0f;
    private static long lastFrame;

    private VehicleCam() {}

    private static VehicleEntity riding(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        return p != null && p.getVehicle() instanceof VehicleEntity v ? v : null;
    }

    /** extra view rotation, applied in view space before the camera's own rotation */
    public static void tilt(MatrixStack ms, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        VehicleEntity v = riding(mc);
        if (v == null || !mc.options.getPerspective().isFirstPerson()) return;
        float roll = MathHelper.lerp(td, v.prevRoll, v.roll);
        float pitch = 0;
        if (v.type.wheeled()) {
            roll += MathHelper.lerp(td, v.prevBodyRoll, v.bodyRoll);
            pitch = MathHelper.lerp(td, v.prevBodyPitch, v.bodyPitch);
        } else if (v.type.category == Category.BOAT || v.type.category == Category.SUBMARINE) {
            pitch = MathHelper.lerp(td, v.prevPitch, v.getPitch()) * 0.6f;
        }
        // express the vehicle's lean in the direction you're looking
        float rel = (float) Math.toRadians(MathHelper.wrapDegrees(mc.player.getYaw(td) - v.getYaw(td)));
        float c = MathHelper.cos(rel), s = MathHelper.sin(rel);
        float viewRoll = (roll * c + pitch * s) * 0.8f;
        float viewPitch = (pitch * c - roll * s) * 0.8f;
        // rumble: idle shake, more with speed and on rough ground; a big jolt in a crash
        float t = (v.age + td);
        float speedFrac = Math.min(1f, v.displaySpeed() / Math.max(0.1f, v.type.maxSpeed));
        float amp = 0;
        if (v.engineRunning() && v.type.category != Category.ANIMAL) amp += 0.04f;
        if (v.type.wheeled() || v.type.category == Category.ANIMAL) amp += speedFrac * (v.type.category == Category.ANIMAL ? 0.9f : 0.18f);
        float wobble = v.getWobble() - td;
        if (wobble > 0) amp += wobble * 0.35f;
        viewRoll += (MathHelper.sin(t * 2.9f) * MathHelper.sin(t * 1.3f)) * amp;
        viewPitch += (MathHelper.sin(t * 3.7f) * MathHelper.cos(t * 2.1f)) * amp;
        if (viewRoll != 0) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(viewRoll));
        if (viewPitch != 0) ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(viewPitch));
    }

    /** multiplier for the field of view: up to +14% at full speed, eased in and out */
    public static double fovScale(float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        VehicleEntity v = riding(mc);
        float target = 0;
        if (v != null) {
            target = Math.min(1f, v.displaySpeed() / Math.max(0.1f, v.type.maxSpeed)) * 0.14f;
        }
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1.0e9f);
        lastFrame = now;
        fovBoost += (target - fovBoost) * Math.min(1f, dt * 3f);
        return 1.0 + fovBoost;
    }

    /** hide the empty floating hand while you're in a vehicle */
    public static boolean hideHands() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return riding(mc) != null && mc.player.getMainHandStack().isEmpty() && mc.player.getOffHandStack().isEmpty();
    }
}
