package com.nothatcher.dragonfruit.client.render;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.client.vfx.Gfx;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Beast forms for the animal fruits, drawn as low-poly shaded models that walk, look around and
 * (for the dragon) flap their wings. Original designs.
 */
public final class BeastRenderer {
    private BeastRenderer() {}

    /** Per-frame drawing context. x = right, y = up, z = forward in body space. */
    private static final class Ctx {
        VertexConsumer solid;
        Matrix4f m;
        Vec3d base, f, r, up = Gfx.UP;
        Vec3d fH, rH, uH; // head frame (look direction)
        float shade;
        boolean hurt;
        double t, limb, limbAmt;

        Vec3d P(double x, double y, double z) {
            return base.add(r.multiply(x)).add(up.multiply(y)).add(f.multiply(z));
        }

        int col(int rgb) {
            int c = Gfx.scaleRgb(rgb, shade);
            if (hurt) c = (Math.min(255, ((c >> 16) & 0xFF) + 110) << 16) | ((((c >> 8) & 0xFF) / 2) << 8) | ((c & 0xFF) / 2);
            return c;
        }

        void box(Vec3d c, double hx, double hy, double hz, int rgb) {
            Gfx.orientedBox(solid, m, c, f, r, up, hx, hy, hz, col(rgb));
        }

        void headBox(Vec3d c, double hx, double hy, double hz, int rgb) {
            Gfx.orientedBox(solid, m, c, fH, rH, uH, hx, hy, hz, col(rgb));
        }

        void stick(Vec3d a, Vec3d b, double w, int rgb) {
            Gfx.stick(solid, m, a, b, w, col(rgb));
        }

        /** Leg hanging from the hip, swinging forward/back by angle a (radians). */
        void leg(Vec3d hip, double len, double w, double a, int rgb, int hoof) {
            Vec3d down = up.multiply(-Math.cos(a)).add(f.multiply(Math.sin(a)));
            Vec3d foot = hip.add(down.multiply(len));
            stick(hip, foot, w, rgb);
            if (hoof != -1) stick(foot.subtract(down.multiply(0.08)), foot, w * 1.1, hoof);
        }

        double swing(double phase, double amp) {
            return Math.sin(limb * 0.6662 + phase) * amp * limbAmt;
        }
    }

    public static void render(AbstractClientPlayerEntity pl, FruitType beast, float td, MatrixStack ms,
                              VertexConsumerProvider vcp, int light) {
        Vec3d saved = Gfx.cam;
        Ctx c = new Ctx();
        c.base = pl.getLerpedPos(td);
        Gfx.cam = c.base; // matrix stack is already at the entity
        try {
            float bodyYaw = MathHelper.lerp(td, pl.prevBodyYaw, pl.bodyYaw);
            double b = Math.toRadians(bodyYaw);
            c.f = new Vec3d(-Math.sin(b), 0, Math.cos(b));
            c.r = new Vec3d(-Math.cos(b), 0, -Math.sin(b));
            c.fH = pl.getRotationVec(td);
            Vec3d rh = c.fH.crossProduct(Gfx.UP);
            c.rH = rh.lengthSquared() < 1.0E-4 ? c.r : rh.normalize();
            c.uH = c.rH.crossProduct(c.fH).normalize();
            c.m = ms.peek().getPositionMatrix();
            c.solid = vcp.getBuffer(RenderLayer.getDebugQuads());
            float bright = Math.max(LightmapTextureManager.getBlockLightCoordinates(light), LightmapTextureManager.getSkyLightCoordinates(light)) / 15f;
            c.shade = 0.3f + 0.7f * bright;
            c.hurt = pl.hurtTime > 0;
            c.t = pl.age + td;
            c.limb = pl.limbAnimator.getPos(td);
            c.limbAmt = Math.min(1.0, pl.limbAnimator.getSpeed(td));
            switch (beast) {
                case WOLF -> wolf(c, pl);
                case REINDEER -> reindeer(c, pl);
                case DRAGON -> dragon(c, pl, vcp);
                default -> { }
            }
            // glowing eyes are drawn by each model via this layer
        } finally {
            Gfx.cam = saved;
        }
    }

    private static void eyes(Ctx c, VertexConsumerProvider vcp, Vec3d head, double fwd, double side, double upOff, double size, int rgb) {
        VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
        for (int s = -1; s <= 1; s += 2) {
            Vec3d e = head.add(c.fH.multiply(fwd)).add(c.rH.multiply(s * side)).add(c.uH.multiply(upOff));
            Gfx.sphere(glow, c.m, e, size, Gfx.argb(rgb, 0.95f), 4, 6);
        }
        c.solid = vcp.getBuffer(RenderLayer.getDebugQuads());
    }

    // -------------------------------------------------------------------- wolf

    private static void wolf(Ctx c, AbstractClientPlayerEntity pl) {
        int grey = 0x8C939A, dark = 0x5F656B, light = 0xB8BEC4;
        boolean crouch = pl.isInSneakingPose();
        double h = crouch ? -0.12 : 0;
        c.box(c.P(0, 0.62 + h, -0.05), 0.22, 0.2, 0.45, grey);
        c.box(c.P(0, 0.68 + h, 0.3), 0.26, 0.25, 0.18, dark);
        Vec3d head = c.P(0, 0.86 + h, 0.55).add(c.fH.multiply(0.12));
        c.headBox(head, 0.18, 0.16, 0.18, grey);
        c.headBox(head.add(c.fH.multiply(0.26)).subtract(c.uH.multiply(0.05)), 0.09, 0.07, 0.12, light);
        c.headBox(head.add(c.fH.multiply(0.39)).subtract(c.uH.multiply(0.02)), 0.04, 0.035, 0.02, 0x202020);
        for (int s = -1; s <= 1; s += 2) {
            c.headBox(head.add(c.uH.multiply(0.2)).add(c.rH.multiply(s * 0.1)).subtract(c.fH.multiply(0.05)), 0.05, 0.08, 0.03, dark);
        }
        double[][] legs = {{-0.13, 0.3, 0}, {0.13, 0.3, Math.PI}, {-0.13, -0.38, Math.PI}, {0.13, -0.38, 0}};
        for (double[] l : legs) c.leg(c.P(l[0], 0.45 + h, l[1]), 0.45 + h, 0.06, c.swing(l[2], 1.1), grey, dark);
        double wag = Math.sin(c.t * 0.35) * 0.35;
        Vec3d tailBase = c.P(0, 0.66 + h, -0.5);
        Vec3d tailDir = c.f.multiply(-0.8).add(Gfx.UP.multiply(0.5)).add(c.r.multiply(wag)).normalize();
        c.stick(tailBase, tailBase.add(tailDir.multiply(0.45)), 0.06, grey);
    }

    // ---------------------------------------------------------------- reindeer

    private static void reindeer(Ctx c, AbstractClientPlayerEntity pl) {
        int brown = 0xA0703A, dark = 0x7A5530, chest = 0x8A5E30, bone = 0xE8DCC0, white = 0xF0EAD8;
        c.box(c.P(0, 1.02, 0), 0.28, 0.28, 0.6, brown);
        c.box(c.P(0, 1.05, 0.45), 0.3, 0.3, 0.2, chest);
        c.box(c.P(0, 0.85, 0), 0.24, 0.1, 0.5, white);
        c.stick(c.P(0, 1.15, 0.55), c.P(0, 1.6, 0.8), 0.13, brown);
        Vec3d head = c.P(0, 1.7, 0.85).add(c.fH.multiply(0.1));
        c.headBox(head, 0.15, 0.15, 0.22, 0xA77A44);
        c.headBox(head.add(c.fH.multiply(0.3)).subtract(c.uH.multiply(0.04)), 0.11, 0.1, 0.12, chest);
        c.headBox(head.add(c.fH.multiply(0.43)).subtract(c.uH.multiply(0.02)), 0.05, 0.045, 0.02, 0x3A2A20);
        for (int s = -1; s <= 1; s += 2) {
            Vec3d a0 = head.add(c.uH.multiply(0.14)).add(c.rH.multiply(s * 0.1)).subtract(c.fH.multiply(0.05));
            Vec3d a1 = a0.add(c.rH.multiply(s * 0.25)).add(c.uH.multiply(0.35)).subtract(c.fH.multiply(0.1));
            Vec3d a2 = a1.add(c.rH.multiply(s * 0.15)).add(c.uH.multiply(0.3)).add(c.fH.multiply(0.05));
            c.stick(a0, a1, 0.035, bone);
            c.stick(a1, a2, 0.03, bone);
            c.stick(a1, a1.add(c.uH.multiply(0.2)).add(c.fH.multiply(0.15)), 0.025, bone);
            c.stick(a2, a2.add(c.fH.multiply(0.15)).add(c.uH.multiply(0.12)), 0.022, bone);
            c.stick(a2, a2.add(c.rH.multiply(s * 0.2)).add(c.uH.multiply(0.15)), 0.022, bone);
            c.headBox(head.add(c.uH.multiply(0.12)).add(c.rH.multiply(s * 0.19)).subtract(c.fH.multiply(0.08)), 0.07, 0.035, 0.03, brown);
        }
        double[][] legs = {{-0.16, 0.42, 0}, {0.16, 0.42, Math.PI}, {-0.16, -0.45, Math.PI}, {0.16, -0.45, 0}};
        for (double[] l : legs) c.leg(c.P(l[0], 0.8, l[1]), 0.8, 0.07, c.swing(l[2], 0.9), dark, 0x2A2018);
        c.box(c.P(0, 1.18, -0.62), 0.08, 0.08, 0.06, white);
    }

    // ------------------------------------------------------------------ dragon

    private static void dragon(Ctx c, AbstractClientPlayerEntity pl, VertexConsumerProvider vcp) {
        int green = 0x2FAF5A, dark = 0x1F7A3E, belly = 0xD8C27A, bone = 0xE8DCC0, pink = 0xE04FB4;
        boolean flying = !pl.isOnGround();
        c.box(c.P(0, 1.2, 0), 0.55, 0.5, 1.1, green);
        c.box(c.P(0, 0.8, 0.05), 0.45, 0.16, 0.95, belly);
        // back spikes (dragonfruit pink)
        for (int i = 0; i < 6; i++) {
            Vec3d at = c.P(0, 1.7, 0.85 - i * 0.38);
            Gfx.tri(c.solid, c.m, at.add(c.f.multiply(0.16)), at.subtract(c.f.multiply(0.16)), at.add(0, 0.38 - i * 0.03, 0).subtract(c.f.multiply(0.12)), Gfx.argb(c.col(pink), 1f));
        }
        // neck and head
        Vec3d head = c.P(0, 2.3, 1.75).add(c.fH.multiply(0.25));
        Vec3d neckBase = c.P(0, 1.45, 0.95);
        for (int i = 0; i < 4; i++) {
            double s = (i + 0.5) / 4;
            Vec3d p = neckBase.lerp(head.subtract(c.fH.multiply(0.3)), s).add(0, Math.sin(Math.PI * s) * 0.15, 0);
            Gfx.shadedSphere(c.solid, c.m, p, 0.33 - i * 0.03, c.col(green), 1f, 5, 8, new Vec3d(0.3, 1, 0.2));
        }
        c.headBox(head, 0.3, 0.25, 0.4, green);
        c.headBox(head.add(c.fH.multiply(0.55)).subtract(c.uH.multiply(0.04)), 0.2, 0.14, 0.3, 0x45C46F);
        c.headBox(head.add(c.fH.multiply(0.5)).subtract(c.uH.multiply(0.24)), 0.18, 0.06, 0.28, dark);
        for (int s = -1; s <= 1; s += 2) {
            Vec3d hb = head.add(c.uH.multiply(0.2)).add(c.rH.multiply(s * 0.15)).subtract(c.fH.multiply(0.2));
            c.stick(hb, hb.subtract(c.fH.multiply(0.55)).add(c.uH.multiply(0.35)).add(c.rH.multiply(s * 0.1)), 0.05, bone);
        }
        eyes(c, vcp, head, 0.25, 0.27, 0.1, 0.06, 0xFFA21A);
        // legs (tucked while flying)
        double[][] legs = {{-0.45, 0.7, 0}, {0.45, 0.7, Math.PI}, {-0.45, -0.7, Math.PI}, {0.45, -0.7, 0}};
        for (double[] l : legs) {
            double a = flying ? -1.1 : c.swing(l[2], 0.8);
            c.leg(c.P(l[0], 0.9, l[1]), 0.9, 0.15, a, green, dark);
        }
        // wings
        double flap = flying ? Math.sin(c.t * 0.35) * 0.9 : 0.15 + 0.05 * Math.sin(c.t * 0.05);
        int membrane = Gfx.argb(c.col(dark), 1f);
        for (int s = -1; s <= 1; s += 2) {
            Vec3d root = c.P(s * 0.5, 1.65, 0.5);
            Vec3d out = c.r.multiply(s * Math.cos(flap)).add(Gfx.UP.multiply(Math.sin(flap)));
            Vec3d tip = root.add(out.multiply(2.8)).subtract(c.f.multiply(0.4));
            Vec3d mid = root.add(out.multiply(1.7)).subtract(c.f.multiply(1.5));
            Vec3d back = root.subtract(c.f.multiply(1.2));
            Gfx.tri(c.solid, c.m, root, tip, mid, membrane);
            Gfx.tri(c.solid, c.m, root, mid, back, membrane);
            c.stick(root, tip, 0.06, green);
            c.stick(root.add(out.multiply(1.4)).subtract(c.f.multiply(0.2)), mid, 0.04, green);
        }
        // tail
        Vec3d prev = c.P(0, 1.2, -1.05);
        for (int k = 1; k <= 6; k++) {
            Vec3d p = c.P(Math.sin(c.t * 0.15 + k * 0.6) * 0.12 * k, 1.15 - k * 0.07, -1.05 - k * 0.45);
            Gfx.shadedSphere(c.solid, c.m, p, 0.32 - k * 0.04, c.col(green), 1f, 4, 8, new Vec3d(0.3, 1, 0.2));
            prev = p;
        }
        Gfx.tri(c.solid, c.m, prev.add(c.r.multiply(0.2)), prev.subtract(c.r.multiply(0.2)), prev.subtract(c.f.multiply(0.6)).add(0, 0.15, 0), Gfx.argb(c.col(pink), 1f));
    }
}
