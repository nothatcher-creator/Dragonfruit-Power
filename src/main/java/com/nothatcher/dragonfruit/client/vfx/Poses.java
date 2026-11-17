package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Where arms and hand-held charges start, for first person (camera space) and third person (body space). */
public final class Poses {
    private Poses() {}

    public static boolean isFirstPersonOwner(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return e == mc.getCameraEntity() && mc.options.getPerspective().isFirstPerson();
    }

    /** Direction the entity is looking (camera direction when it is us in first person). */
    public static Vec3d look(Entity e, float td) {
        if (isFirstPersonOwner(e)) {
            Camera cam = MinecraftClient.getInstance().gameRenderer.getCamera();
            return Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
        }
        return e.getRotationVec(td);
    }

    public static Vec3d eye(Entity e, float td) {
        if (isFirstPersonOwner(e)) return MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        return e.getCameraPosVec(td);
    }

    public static Vec3d rightOf(Vec3d look) {
        Vec3d r = look.crossProduct(Gfx.UP);
        if (r.lengthSquared() < 1.0E-4) r = new Vec3d(1, 0, 0);
        return r.normalize();
    }

    /**
     * Shoulder / arm root. In first person the arm comes out from the lower corner of the screen,
     * exactly where the vanilla hand sits, so it feels like your own arm stretching.
     */
    public static Vec3d shoulder(Entity e, float td, int side, double jx, double jy) {
        if (isFirstPersonOwner(e)) {
            Camera cam = MinecraftClient.getInstance().gameRenderer.getCamera();
            Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
            Vec3d right = rightOf(look);
            Vec3d up = right.crossProduct(look);
            return cam.getPos()
                    .add(right.multiply(0.34 * side + jx))
                    .add(up.multiply(-0.30 + jy))
                    .add(look.multiply(0.22));
        }
        float bodyYaw = e.getYaw();
        if (e instanceof LivingEntity le) bodyYaw = MathHelper.lerp(td, le.prevBodyYaw, le.bodyYaw);
        double rad = Math.toRadians(bodyYaw);
        Vec3d right = new Vec3d(-Math.cos(rad), 0, -Math.sin(rad));
        double h = e.isInSneakingPose() ? 1.12 : 1.38;
        Vec3d forward = new Vec3d(-Math.sin(rad), 0, Math.cos(rad));
        return e.getLerpedPos(td).add(0, h + jy, 0).add(right.multiply(0.36 * side + jx)).add(forward.multiply(0.05));
    }

    /** Point just in front of the right hand, for charging orbs. */
    public static Vec3d handFront(Entity e, float td) {
        Vec3d look = look(e, td);
        Vec3d right = rightOf(look);
        Vec3d up = right.crossProduct(look);
        if (isFirstPersonOwner(e)) {
            return eye(e, td).add(look.multiply(1.1)).add(right.multiply(0.32)).add(up.multiply(-0.22));
        }
        return eye(e, td).add(look.multiply(1.0)).add(right.multiply(0.35)).add(up.multiply(-0.35));
    }
}
