package com.nothatcher.dragonfruit.client;

import com.nothatcher.dragonfruit.FruitType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;

/**
 * Over-the-shoulder third-person camera (used in F5 back view).
 * - smooth follow with a little lag, so launches and dashes feel fast
 * - pulls back automatically during big abilities and at high speed
 * - never clips through walls
 * Modes (cycle with V): right shoulder, left shoulder, centred, vanilla.
 */
public final class PowerCam {
    public static final int RIGHT = 0, LEFT = 1, CENTER = 2, OFF = 3;
    private static int mode = RIGHT;

    private static Vec3d smoothEye;
    private static double side = 0.75, distance = 3.6;
    private static long lastNanos;

    private PowerCam() {}

    public static boolean active() {
        return mode != OFF;
    }

    public static void cycle() {
        mode = (mode + 1) % 4;
        String key = switch (mode) {
            case RIGHT -> "msg.dragonfruit.cam_right";
            case LEFT -> "msg.dragonfruit.cam_left";
            case CENTER -> "msg.dragonfruit.cam_center";
            default -> "msg.dragonfruit.cam_off";
        };
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.translatable(key).formatted(Formatting.YELLOW), true);
    }

    private static double targetSide() {
        return switch (mode) {
            case RIGHT -> 0.75;
            case LEFT -> -0.75;
            default -> 0.0;
        };
    }

    private static double targetDistance(Entity e) {
        double d = 3.6;
        d += Math.max(0, Math.min(3.5, CameraFx.fovKickAmount() * 9.0)); // abilities pull the camera back
        d += Math.min(2.5, e.getVelocity().length() * 1.3);            // so does speed
        if (e.isInSneakingPose()) d -= 0.6;
        if (e instanceof net.minecraft.entity.player.PlayerEntity pl) {
            FruitType beast = ClientPowerData.beastOf(pl);
            if (beast == FruitType.DRAGON) d += 3.5;
            else if (beast == FruitType.REINDEER) d += 1.0;
        }
        return d;
    }

    /**
     * Computes the camera position. eye = exact (interpolated) eye position, yaw/pitch = camera rotation.
     */
    public static Vec3d position(BlockView world, Entity e, Vec3d eye, float pitch, float yaw) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.016 : Math.min(0.1, (now - lastNanos) / 1.0E9);
        lastNanos = now;

        if (smoothEye == null || smoothEye.squaredDistanceTo(eye) > 64) smoothEye = eye; // teleports snap
        double kPos = 1 - Math.exp(-dt * 16);
        smoothEye = smoothEye.add(eye.subtract(smoothEye).multiply(kPos));
        Vec3d lag = smoothEye.subtract(eye);
        if (lag.length() > 1.4) smoothEye = eye.add(lag.normalize().multiply(1.4));

        double kSide = 1 - Math.exp(-dt * 8);
        side += (targetSide() - side) * kSide;
        double target = targetDistance(e);
        double kDist = 1 - Math.exp(-dt * (target > distance ? 7 : 4));
        distance += (target - distance) * kDist;

        Vec3d fwd = Vec3d.fromPolar(pitch, yaw);
        Vec3d right = fwd.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 1.0E-4) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(fwd).normalize();

        Vec3d pivot = clip(world, e, eye, smoothEye.add(up.multiply(0.2)));
        Vec3d shoulder = clip(world, e, pivot, pivot.add(right.multiply(side)));
        return clip(world, e, shoulder, shoulder.subtract(fwd.multiply(distance)));
    }

    /** Moves from a toward b, stopping 0.2 blocks before anything solid (checks the four corners of a small box). */
    private static Vec3d clip(BlockView world, Entity e, Vec3d a, Vec3d b) {
        Vec3d delta = b.subtract(a);
        double len = delta.length();
        if (len < 1.0E-4) return a;
        double best = len;
        double o = 0.1;
        double[][] offs = {{0, 0, 0}, {o, o, o}, {-o, o, -o}, {o, -o, -o}, {-o, -o, o}};
        for (double[] off : offs) {
            Vec3d s = a.add(off[0], off[1], off[2]);
            BlockHitResult hit = world.raycast(new RaycastContext(s, s.add(delta), RaycastContext.ShapeType.VISUAL,
                    RaycastContext.FluidHandling.NONE, e));
            if (hit.getType() != HitResult.Type.MISS) {
                best = Math.min(best, hit.getPos().distanceTo(s) - 0.2);
            }
        }
        best = Math.max(0, best);
        return a.add(delta.multiply(best / len));
    }

    public static void reset() {
        smoothEye = null;
        lastNanos = 0;
    }
}
