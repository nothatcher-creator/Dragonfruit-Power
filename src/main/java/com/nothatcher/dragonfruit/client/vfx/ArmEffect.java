package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.client.CameraFx;
import com.nothatcher.dragonfruit.client.ClientPowerData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The rubber arm. Drawn with the owner's real skin texture as a bendy, wobbling tube from the shoulder
 * (or the lower corner of the screen in first person) to the target. It overshoots, jiggles, thins out
 * as it stretches, bulges at the fist, and snaps back.
 */
public class ArmEffect extends VfxEffect {
    final int ownerId;
    final int side;
    private final Vec3d target;
    private final int extend, hold, retract;
    private final float thickness;
    private final float alpha;
    private final double jx, jy;

    public ArmEffect(int ownerId, int side, Vec3d target, int extend, int hold, int retract, float thickness,
                     float alpha, double jx, double jy) {
        super(extend + hold + retract + 1);
        this.ownerId = ownerId;
        this.side = side >= 0 ? 1 : -1;
        this.target = target;
        this.extend = Math.max(1, extend);
        this.hold = Math.max(0, hold);
        this.retract = Math.max(1, retract);
        this.thickness = thickness;
        this.alpha = alpha;
        this.jx = jx;
        this.jy = jy;
    }

    public boolean visible() {
        return age < extend + hold + retract;
    }

    /** 0 = at shoulder, 1 = at target. Overshoots a little for a rubbery feel. */
    private double reach(double t) {
        if (t < extend) {
            double x = t / extend;
            double c1 = 1.70158 * 0.7, c3 = c1 + 1;
            return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
        }
        t -= extend;
        if (t < hold) return 1 + 0.05 * Math.sin(t * 2.4) * Math.exp(-t * 0.35);
        t -= hold;
        double r = t / retract;
        if (r >= 1) return 0;
        return 1 - r * r;
    }

    private double energy(double t) {
        if (t < extend) return 1.0;
        if (t < extend + hold) return 0.25 + 0.6 * Math.exp(-(t - extend) * 0.5);
        return 0.85;
    }

    @Override
    protected void onTick(ClientWorld world) {
        Entity e = world.getEntityById(ownerId);
        if (e == null) return;
        // wind trail at the fist while it flies out
        if (age < extend && alpha >= 0.99f) {
            Vec3d start = Poses.shoulder(e, 1f, side, jx, jy);
            Vec3d tip = start.add(target.subtract(start).multiply(Gfx.clamp01(reach(age + 1))));
            Particles.add(ParticleTypes.CLOUD, tip, Vec3d.ZERO);
        }
        // the "thwap" when the arm snaps back into place
        if (age == extend + hold + retract - 1 && alpha >= 0.99f && e == mc().player) {
            world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_SLIME_SQUISH_SMALL, SoundCategory.PLAYERS, 0.5f, 1.6f, false);
            CameraFx.kickFov(0.04f);
        }
    }

    /** Gear Third: how inflated the arm is right now (0..1). */
    private double inflation(double t) {
        if (t < extend) return Gfx.easeOutCubic(t / extend);
        t -= extend;
        if (t < hold) return 1;
        t -= hold;
        return Gfx.clamp01(1 - t / retract);
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        if (mc().world == null) return;
        Entity e = mc().world.getEntityById(ownerId);
        if (!(e instanceof AbstractClientPlayerEntity pl)) return;
        double t = age + td;
        double reach = reach(t);
        if (reach <= 0.001) return;
        Vec3d start = Poses.shoulder(pl, td, side, jx, jy);
        Vec3d full = target.subtract(start);
        if (full.lengthSquared() < 0.0025) return;
        Vec3d tip = start.add(full.multiply(reach));

        Identifier skin = pl.getSkinTexture();
        int light = WorldRenderer.getLightmapCoordinates(mc().world, BlockPos.ofFloored(start));
        boolean slim = "slim".equals(pl.getModel());
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f nm = ms.peek().getNormalMatrix();

        float r = 1f, g = 1f, b = 1f;
        if (pl == mc().player && ClientPowerData.fruit == FruitType.RUBBER && ClientPowerData.buffActive()) {
            // Second Gear: flushed pink skin
            g = 0.72f;
            b = 0.74f;
        }
        if (VfxManager.hasAura(ownerId, com.nothatcher.dragonfruit.net.Vfx.A_ARMOR)) {
            // Willpower Armor: hardened, near-black arm
            r = 0.16f;
            g = 0.15f;
            b = 0.2f;
        }

        // motion after-images trailing the fist while it shoots out
        if (t < extend && alpha >= 0.99f) {
            VertexConsumer ghost = vcp.getBuffer(RenderLayer.getEntityTranslucent(skin));
            for (int k = 1; k <= 2; k++) {
                double tg = Math.max(0, t - k * 0.9);
                double rg = reach(tg);
                if (rg <= 0.001) continue;
                drawArm(ghost, m, nm, slim, start, start.add(full.multiply(rg)), tg, energy(tg), light, r, g, b,
                        k == 1 ? 0.35f : 0.16f, false);
            }
        }

        RenderLayer layer = alpha >= 0.99f ? RenderLayer.getEntityCutoutNoCull(skin) : RenderLayer.getEntityTranslucent(skin);
        drawArm(vcp.getBuffer(layer), m, nm, slim, start, tip, t, energy(t), light, r, g, b, alpha, false);

        // sleeve / second skin layer, like the real player model
        PlayerModelPart sleeve = side > 0 ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE;
        if (pl.isPartVisible(sleeve)) {
            drawArm(vcp.getBuffer(RenderLayer.getEntityTranslucent(skin)), m, nm, slim, start, tip, t, energy(t), light,
                    r, g, b, alpha, true);
        }
    }

    private void drawArm(VertexConsumer vc, Matrix4f m, Matrix3f nm, boolean slim, Vec3d start, Vec3d tip, double t,
                         double energy, int light, float r, float g, float b, float a, boolean overlay) {
        Vec3d axis = tip.subtract(start);
        double len = axis.length();
        if (len < 0.02) return;
        Vec3d d = axis.multiply(1.0 / len);
        Vec3d u = d.crossProduct(Gfx.UP);
        if (u.lengthSquared() < 1.0E-4) u = Poses.rightOf(Poses.look(mc().player, 1f));
        u = u.normalize();
        Vec3d v = u.crossProduct(d).normalize();

        boolean giant = thickness > 1.6f;
        double uniform = giant ? 1.0 : thickness;
        double swell = giant ? (thickness - 1) * inflation(t) : 0;
        double layerGrow = overlay ? 1.12 : 1.0;
        double halfU = (slim ? 1.5 : 2.0) / 16.0 * uniform * layerGrow;
        double halfV = 2.0 / 16.0 * uniform * layerGrow;
        double amp = Math.min(0.55, 0.04 + 0.05 * Math.sqrt(len)) * energy * (giant ? 0.5 : 1);
        double thin = 0.32 * Math.min(1.0, len / 10.0) * (giant ? 0.2 : 1);

        // skin UV layout (pixels on a 64x64 skin); overlay = sleeve layer
        int w = slim ? 3 : 4;
        int x0 = side > 0 ? 40 : (overlay ? 48 : 32);
        int y0 = side > 0 ? (overlay ? 36 : 20) : 52;
        int capY = side > 0 ? (overlay ? 32 : 16) : 48;
        float[] faceX = {x0, x0 + 4, x0 + 4 + w, x0 + 8 + w, x0 + 8 + 2 * w}; // +u | +v(front) | -u | -v(back)
        float topX = x0 + 4, botX = x0 + 4 + w;

        // segment plan: shoulder piece, tiled middle pieces, hand piece
        double shoulderLen = Math.min(0.30, len * 0.25);
        double handLen = Math.min(giant ? 0.36 + swell * 0.25 : 0.36, len * 0.30);
        double midLen = Math.max(0.0, len - shoulderLen - handLen);
        int mid = Math.max(1, (int) Math.round(midLen / 0.55));
        int segs = mid + 2;
        double[] from = new double[segs], to = new double[segs];
        float[] vy0 = new float[segs], vy1 = new float[segs];
        from[0] = 0;
        to[0] = shoulderLen;
        vy0[0] = y0;
        vy1[0] = y0 + 3;
        for (int i = 0; i < mid; i++) {
            from[i + 1] = shoulderLen + midLen * i / mid;
            to[i + 1] = shoulderLen + midLen * (i + 1) / mid;
            vy0[i + 1] = y0 + 3;
            vy1[i + 1] = y0 + 8;
        }
        from[segs - 1] = len - handLen;
        to[segs - 1] = len;
        vy0[segs - 1] = y0 + 8;
        vy1[segs - 1] = y0 + 12;

        Vec3d[] prev = null;
        for (int i = 0; i < segs; i++) {
            Vec3d[] ringA = prev != null ? prev : ringAt(start, axis, u, v, from[i] / len, t, amp, thin, halfU, halfV, len, handLen, swell);
            Vec3d[] ringB = ringAt(start, axis, u, v, to[i] / len, t, amp, thin, halfU, halfV, len, handLen, swell);
            side(vc, m, nm, ringA[1], ringA[0], ringB[0], ringB[1], faceX[1], faceX[2], vy0[i], vy1[i], v, light, r, g, b, a);          // front (+v)
            side(vc, m, nm, ringA[0], ringA[3], ringB[3], ringB[0], faceX[0], faceX[1], vy0[i], vy1[i], u, light, r, g, b, a);          // +u side
            side(vc, m, nm, ringA[3], ringA[2], ringB[2], ringB[3], faceX[3], faceX[4], vy0[i], vy1[i], v.negate(), light, r, g, b, a); // back (-v)
            side(vc, m, nm, ringA[2], ringA[1], ringB[1], ringB[2], faceX[2], faceX[3], vy0[i], vy1[i], u.negate(), light, r, g, b, a); // -u side
            if (i == 0) cap(vc, m, nm, ringA, topX, capY, w, d.negate(), light, r, g, b, a);
            if (i == segs - 1) cap(vc, m, nm, ringB, botX, capY, w, d, light, r, g, b, a);
            prev = ringB;
        }
    }

    /** Cross-section corners at fraction s along the arm: [+u+v, -u+v, -u-v, +u-v]. */
    private static Vec3d[] ringAt(Vec3d start, Vec3d axis, Vec3d u, Vec3d v, double s, double t, double amp, double thin,
                                  double halfU, double halfV, double len, double handLen, double swell) {
        double env = Math.sin(Math.PI * s);
        double ox = env * amp * Math.sin(t * 0.9 + s * 7.0);
        double oy = env * amp * 0.7 * Math.cos(t * 0.75 + s * 5.0);
        Vec3d c = start.add(axis.multiply(s)).add(u.multiply(ox)).add(v.multiply(oy));
        double scale = 1.0 - thin * env;
        double handStart = 1.0 - handLen / len;
        if (s > handStart) scale *= 1.0 + 0.22 * Gfx.clamp01((s - handStart) / Math.max(1.0E-4, 1.0 - handStart)); // fist bulge
        if (swell > 0) scale *= 1.0 + swell * Math.pow(s, 1.3); // Gear Third: balloons toward the fist
        double hu = halfU * scale, hv = halfV * scale;
        return new Vec3d[]{
                c.add(u.multiply(hu)).add(v.multiply(hv)),
                c.subtract(u.multiply(hu)).add(v.multiply(hv)),
                c.subtract(u.multiply(hu)).subtract(v.multiply(hv)),
                c.add(u.multiply(hu)).subtract(v.multiply(hv))
        };
    }

    private void side(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3d a0, Vec3d a1, Vec3d b1, Vec3d b0,
                      float ux0, float ux1, float vy0, float vy1, Vec3d n, int light, float r, float g, float b, float a) {
        vert(vc, m, nm, a0, ux0, vy0, n, light, r, g, b, a);
        vert(vc, m, nm, a1, ux1, vy0, n, light, r, g, b, a);
        vert(vc, m, nm, b1, ux1, vy1, n, light, r, g, b, a);
        vert(vc, m, nm, b0, ux0, vy1, n, light, r, g, b, a);
    }

    private void cap(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3d[] ring, float ux, float uy, int w, Vec3d n,
                     int light, float r, float g, float b, float a) {
        vert(vc, m, nm, ring[0], ux + w, uy, n, light, r, g, b, a);
        vert(vc, m, nm, ring[1], ux, uy, n, light, r, g, b, a);
        vert(vc, m, nm, ring[2], ux, uy + 4, n, light, r, g, b, a);
        vert(vc, m, nm, ring[3], ux + w, uy + 4, n, light, r, g, b, a);
    }

    private void vert(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3d p, float uPx, float vPx, Vec3d n, int light,
                      float r, float g, float b, float a) {
        vc.vertex(m, (float) (p.x - Gfx.cam.x), (float) (p.y - Gfx.cam.y), (float) (p.z - Gfx.cam.z))
                .color(r, g, b, a)
                .texture(uPx / 64f, vPx / 64f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nm, (float) n.x, (float) n.y, (float) n.z)
                .next();
    }
}
