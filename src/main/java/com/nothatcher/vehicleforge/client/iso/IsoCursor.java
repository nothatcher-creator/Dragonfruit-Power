package com.nothatcher.vehicleforge.client.iso;

import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * The isometric view's cursor. Dragging (mouse or touch) moves it around the screen instead of turning your head,
 * and whatever it points at becomes the game's target: the block outline, breaking, using and attacking all follow
 * it. Things out of reach are still tracked so a click on them can open the options menu.
 *
 * It's a drawn cursor rather than the system one so the game keeps the mouse captured: on phone launchers that
 * keeps the in-game touch controls (joystick and buttons) on screen.
 */
public final class IsoCursor {
    /** cursor position, 0..1 across and down the window */
    private static double u = 0.5, v = 0.5;

    /** what the cursor is over this frame, at any distance (null = nothing but sky) */
    public static BlockHitResult block;
    public static Entity entity;
    public static Vec3d point;
    /** is that within reach of where you stand? */
    public static boolean inReach;

    private IsoCursor() {}

    public static void reset() {
        u = 0.5;
        v = 0.5;
    }

    public static double u() {
        return u;
    }

    public static double v() {
        return v;
    }

    /** a drag of (dx, dy), in the units the game would have turned your head by */
    public static void move(double dx, double dy) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double w = Math.max(1, mc.getWindow().getWidth()), h = Math.max(1, mc.getWindow().getHeight());
        u = MathHelper.clamp(u + dx / w, 0.0, 1.0);
        v = MathHelper.clamp(v + dy / h, 0.0, 1.0);
    }

    public static double reach(MinecraftClient mc) {
        return mc.interactionManager == null ? 4.5 : mc.interactionManager.getReachDistance();
    }

    /**
     * Cast the cursor into the world (called each frame and each tick, after the game picks its own target) and
     * point the game's target at what it hits.
     */
    public static void update(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        Camera cam = mc.gameRenderer.getCamera();
        if (p == null || mc.world == null || !cam.isReady()) return;
        float hh = IsoCam.halfHeight();
        float aspect = (float) mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());
        double nx = u * 2 - 1, ny = 1 - v * 2;
        double yawR = Math.toRadians(cam.getYaw());
        Vec3d fwd = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
        Vec3d right = new Vec3d(-Math.cos(yawR), 0, -Math.sin(yawR));
        Vec3d up = right.crossProduct(fwd);
        double vy = ny * hh;
        Vec3d origin = cam.getPos().add(right.multiply(nx * hh * aspect)).add(up.multiply(vy));
        // start where the cutaway leaves off: whatever's nearer than that isn't drawn, so it can't be clicked
        double startDepth = IsoCam.cut0 + IsoCam.cutSlope * vy;
        Vec3d start = origin.add(fwd.multiply(startDepth)), end = origin.add(fwd.multiply(startDepth + 256));

        BlockHitResult bh = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
        double maxSq = bh.getType() == HitResult.Type.MISS ? start.squaredDistanceTo(end) : start.squaredDistanceTo(bh.getPos());
        Box sweep = new Box(start, end).expand(1.0);
        EntityHitResult eh = ProjectileUtil.raycast(p, start, end, sweep,
                e -> !e.isSpectator() && e.canHit() && e != p && e != p.getVehicle(), maxSq);

        Vec3d eye = p.getEyePos();
        double reach = reach(mc);
        if (eh != null) {
            entity = eh.getEntity();
            block = null;
            point = eh.getPos();
            inReach = eye.squaredDistanceTo(eh.getPos()) <= reach * reach;
        } else if (bh.getType() == HitResult.Type.BLOCK) {
            entity = null;
            block = bh;
            point = bh.getPos();
            inReach = eye.squaredDistanceTo(bh.getPos()) <= reach * reach;
        } else {
            entity = null;
            block = null;
            // nothing there: aim at the spot level with your eyes
            double t = Math.abs(fwd.y) < 1e-4 ? 0 : (eye.y - start.y) / fwd.y;
            point = start.add(fwd.multiply(Math.max(0, t)));
            inReach = false;
        }

        // only things in reach become the game's own target (the server rejects anything further)
        if (inReach && entity != null) {
            mc.crosshairTarget = eh;
            mc.targetedEntity = entity;
        } else if (inReach && block != null) {
            mc.crosshairTarget = block;
            mc.targetedEntity = null;
        } else {
            mc.crosshairTarget = BlockHitResult.createMissed(point, Direction.UP, p.getBlockPos());
            mc.targetedEntity = null;
        }

        // face the cursor, unless you're walking somewhere (then you face where you're going)
        boolean flying = p.getVehicle() instanceof VehicleEntity ve && ve.type.flies();
        if (!flying && !IsoWalker.active() && !IsoActions.breaking() && (!IsoInput.moving || IsoInput.aiming(mc))) lookAt(p, point);
    }

    public static void lookAt(ClientPlayerEntity p, Vec3d at) {
        Vec3d d = at.subtract(p.getEyePos());
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        if (horiz < 1e-3 && Math.abs(d.y) < 1e-3) return;
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, horiz));
        p.setYaw(yaw);
        p.setPitch(MathHelper.clamp(pitch, -90f, 90f));
        p.setHeadYaw(yaw);
    }

    /** the cursor itself, and a word about what's under it */
    public static void render(DrawContext c, MinecraftClient mc) {
        if (!IsoCam.active() || mc.currentScreen != null || mc.options.hudHidden) return;
        int x = (int) (u * mc.getWindow().getScaledWidth()), y = (int) (v * mc.getWindow().getScaledHeight());
        boolean usable = inReach && (entity != null || (block != null && IsoActions.interactable(mc.world.getBlockState(block.getBlockPos()))));
        int col = entity != null && inReach ? 0xFFFF6A5A : usable ? 0xFF7CFF8A : inReach ? 0xFFFFFFFF : 0xFFFFD45A;
        // a small arrow with a dark rim
        for (int i = -1; i < 9; i++) c.fill(x - 1, y + i, x + Math.min(i, 6) + 2, y + i + 1, 0xC0000000);
        for (int i = 0; i < 8; i++) c.fill(x, y + i, x + Math.min(i, 6) + 1 - (i >= 7 ? 4 : 0), y + i + 1, col);
        Text label = null;
        if (entity != null) label = entity.getDisplayName();
        else if (block != null) label = mc.world.getBlockState(block.getBlockPos()).getBlock().getName();
        if (label != null) {
            Text line = inReach || IsoWalker.active() ? label : Text.translatable("msg.vehicleforge.iso.far", label);
            c.drawTextWithShadow(mc.textRenderer, line, x + 8, y + 8, inReach ? 0xFFE8E8E8 : 0xFFFFD45A);
        }
    }
}
