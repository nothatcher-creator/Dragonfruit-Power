package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Immediate-mode geometry helpers for POSITION_COLOR render layers (lightning = additive glow,
 * debug quads = plain translucent). All positions are world space; {@link #cam} is subtracted here.
 * Every quad is emitted with both windings so it is visible from either side.
 */
public final class Gfx {
    public static Vec3d cam = Vec3d.ZERO;
    public static final Vec3d UP = new Vec3d(0, 1, 0);

    private Gfx() {}

    public static void vtx(VertexConsumer vc, Matrix4f m, Vec3d p, int argb) {
        vc.vertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .next();
    }

    public static void quad(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int argb) {
        vtx(vc, m, a, argb);
        vtx(vc, m, b, argb);
        vtx(vc, m, c, argb);
        vtx(vc, m, d, argb);
        vtx(vc, m, d, argb);
        vtx(vc, m, c, argb);
        vtx(vc, m, b, argb);
        vtx(vc, m, a, argb);
    }

    public static void tri(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, int argb) {
        quad(vc, m, a, b, c, c, argb);
    }

    /** Camera-facing strip between two points. */
    public static void ribbon(VertexConsumer vc, Matrix4f m, Vec3d from, Vec3d to, double width, int argb) {
        Vec3d axis = to.subtract(from);
        Vec3d mid = from.add(to).multiply(0.5);
        Vec3d view = mid.subtract(cam);
        Vec3d side = axis.crossProduct(view);
        if (side.lengthSquared() < 1.0E-8) side = axis.crossProduct(UP);
        if (side.lengthSquared() < 1.0E-8) side = new Vec3d(1, 0, 0);
        side = side.normalize().multiply(width * 0.5);
        quad(vc, m, from.add(side), from.subtract(side), to.subtract(side), to.add(side), argb);
    }

    /** Flat ring lying in the plane perpendicular to normal. */
    public static void ring(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d normal, double radius, double thickness, int argb, int segments) {
        Vec3d[] b = basis(normal);
        double inner = Math.max(0, radius - thickness * 0.5);
        double outer = radius + thickness * 0.5;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            Vec3d d0 = b[0].multiply(Math.cos(a0)).add(b[1].multiply(Math.sin(a0)));
            Vec3d d1 = b[0].multiply(Math.cos(a1)).add(b[1].multiply(Math.sin(a1)));
            quad(vc, m, c.add(d0.multiply(inner)), c.add(d0.multiply(outer)), c.add(d1.multiply(outer)), c.add(d1.multiply(inner)), argb);
        }
    }

    public static void sphere(VertexConsumer vc, Matrix4f m, Vec3d c, double r, int argb, int lat, int lon) {
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat - Math.PI / 2;
            double t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon;
                double p1 = Math.PI * 2 * (j + 1) / lon;
                quad(vc, m, sp(c, r, t0, p0), sp(c, r, t0, p1), sp(c, r, t1, p1), sp(c, r, t1, p0), argb);
            }
        }
    }

    /** Sphere lit from one direction (cheap per-face shading), for solid-looking rocks. */
    public static void shadedSphere(VertexConsumer vc, Matrix4f m, Vec3d c, double r, int rgb, float alpha, int lat, int lon, Vec3d light) {
        Vec3d l = light.normalize();
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat - Math.PI / 2;
            double t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon;
                double p1 = Math.PI * 2 * (j + 1) / lon;
                Vec3d a = sp(c, r, t0, p0), b = sp(c, r, t0, p1), cc = sp(c, r, t1, p1), d = sp(c, r, t1, p0);
                Vec3d n = a.add(b).add(cc).add(d).multiply(0.25).subtract(c).normalize();
                double shade = 0.35 + 0.65 * Math.max(0, n.dotProduct(l));
                // little craters: darken some faces
                if (((i * 7 + j * 13) % 9) == 0) shade *= 0.7;
                int rr = (int) Math.min(255, ((rgb >> 16) & 0xFF) * shade);
                int gg = (int) Math.min(255, ((rgb >> 8) & 0xFF) * shade);
                int bb = (int) Math.min(255, (rgb & 0xFF) * shade);
                quad(vc, m, a, b, cc, d, argb((rr << 16) | (gg << 8) | bb, alpha));
            }
        }
    }

    private static Vec3d sp(Vec3d c, double r, double theta, double phi) {
        double cos = Math.cos(theta);
        return c.add(r * cos * Math.cos(phi), r * Math.sin(theta), r * cos * Math.sin(phi));
    }

    public static void box(VertexConsumer vc, Matrix4f m, Box b, int argb) {
        Vec3d a = new Vec3d(b.minX, b.minY, b.minZ), B = new Vec3d(b.maxX, b.minY, b.minZ),
                C = new Vec3d(b.maxX, b.minY, b.maxZ), D = new Vec3d(b.minX, b.minY, b.maxZ),
                E = new Vec3d(b.minX, b.maxY, b.minZ), F = new Vec3d(b.maxX, b.maxY, b.minZ),
                G = new Vec3d(b.maxX, b.maxY, b.maxZ), H = new Vec3d(b.minX, b.maxY, b.maxZ);
        quad(vc, m, a, B, C, D, argb);
        quad(vc, m, E, F, G, H, argb);
        quad(vc, m, a, B, F, E, argb);
        quad(vc, m, D, C, G, H, argb);
        quad(vc, m, a, D, H, E, argb);
        quad(vc, m, B, C, G, F, argb);
    }

    /** Six-sided crystal: base hexagon at base, tip at base + axis * height, small foot below. */
    public static void crystal(VertexConsumer vc, Matrix4f m, Vec3d base, Vec3d axis, double height, double radius, int argb) {
        Vec3d dir = axis.normalize();
        Vec3d[] b = basis(dir);
        Vec3d tip = base.add(dir.multiply(height));
        Vec3d foot = base.subtract(dir.multiply(height * 0.18));
        Vec3d mid = base.add(dir.multiply(height * 0.22));
        Vec3d[] ring = new Vec3d[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            ring[i] = mid.add(b[0].multiply(Math.cos(a) * radius)).add(b[1].multiply(Math.sin(a) * radius));
        }
        for (int i = 0; i < 6; i++) {
            Vec3d r0 = ring[i], r1 = ring[(i + 1) % 6];
            tri(vc, m, r0, r1, tip, argb);
            tri(vc, m, r1, r0, foot, argb);
        }
    }

    /**
     * Box aligned to a frame (f = forward, r = right, u = up), with simple per-face shading baked into the colour.
     * hr / hu / hf are half sizes along r / u / f.
     */
    public static void orientedBox(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d f, Vec3d r, Vec3d u,
                                   double hr, double hu, double hf, int rgb) {
        Vec3d[] k = new Vec3d[8];
        int idx = 0;
        for (int i = -1; i <= 1; i += 2) for (int j = -1; j <= 1; j += 2) for (int l = -1; l <= 1; l += 2) {
            k[idx++] = c.add(r.multiply(i * hr)).add(u.multiply(j * hu)).add(f.multiply(l * hf));
        }
        int top = argb(rgb, 1f), side = argb(scaleRgb(rgb, 0.8f), 1f), front = argb(scaleRgb(rgb, 0.9f), 1f), bottom = argb(scaleRgb(rgb, 0.55f), 1f);
        quad(vc, m, k[2], k[3], k[7], k[6], top);
        quad(vc, m, k[0], k[1], k[5], k[4], bottom);
        quad(vc, m, k[4], k[5], k[7], k[6], side);
        quad(vc, m, k[0], k[1], k[3], k[2], side);
        quad(vc, m, k[1], k[3], k[7], k[5], front);
        quad(vc, m, k[0], k[2], k[6], k[4], bottom);
    }

    /** A box stretched between two points (legs, horns, bones). */
    public static void stick(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, double halfWidth, int rgb) {
        Vec3d axis = b.subtract(a);
        double len = axis.length();
        if (len < 1.0E-4) return;
        Vec3d u = axis.multiply(1 / len);
        Vec3d[] bs = basis(u);
        orientedBox(vc, m, a.add(b).multiply(0.5), bs[1], bs[0], u, halfWidth, len / 2, halfWidth, rgb);
    }

    public static int scaleRgb(int rgb, float s) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * s), g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * s), b = (int) Math.min(255, (rgb & 0xFF) * s);
        return (r << 16) | (g << 8) | b;
    }

    /** Two unit vectors perpendicular to n (and to each other). */
    public static Vec3d[] basis(Vec3d n) {
        Vec3d nn = n.lengthSquared() < 1.0E-8 ? UP : n.normalize();
        Vec3d ref = Math.abs(nn.y) < 0.95 ? UP : new Vec3d(1, 0, 0);
        Vec3d u = nn.crossProduct(ref).normalize();
        Vec3d v = nn.crossProduct(u).normalize();
        return new Vec3d[]{u, v};
    }

    public static int argb(int rgb, float alpha) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static double easeOutCubic(double x) {
        double t = 1 - Math.max(0, Math.min(1, x));
        return 1 - t * t * t;
    }

    public static double clamp01(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
