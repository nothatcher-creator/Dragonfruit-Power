package com.nothatcher.outbreak.client.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The animated scene behind the Outbreak menus, drawn entirely from flat shapes (no textures, cheap on a phone):
 * a blood moon over a dead city at dusk, two layers of skyline drifting past (a few windows still lit, some
 * flickering), smoke rising from broken towers, ground fog, a burning wreck, the dead shambling along the street,
 * falling ash and rising embers, and now and then a flash of lightning.
 */
public final class MenuBackdrop {
    private record Building(float x, float w, float h, int roof, int seed) {}

    private static final float TILE = 560f;
    private static final List<Building> FAR = skyline(11L, 0.20f, 0.46f, 24, 58);
    private static final List<Building> MID = skyline(23L, 0.26f, 0.58f, 30, 72);

    private MenuBackdrop() {}

    private static List<Building> skyline(long seed, float minH, float maxH, int minW, int maxW) {
        Random r = new Random(seed);
        List<Building> out = new ArrayList<>();
        float x = 0;
        while (x < TILE) {
            float w = minW + r.nextInt(maxW - minW);
            out.add(new Building(x, w, minH + r.nextFloat() * (maxH - minH), r.nextInt(6), r.nextInt()));
            x += w + 2 + r.nextInt(12);
        }
        return out;
    }

    /** seconds, for the animation */
    public static float time() {
        return (Util.getMeasuringTimeMs() % 3_600_000L) / 1000f;
    }

    private static int hash(int a, int b) {
        int h = a * 0x27D4EB2D ^ b * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return h & 0x7fffffff;
    }

    public static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    /** lightning: 0 most of the time, near 1 during a strike (two quick flashes, then a fade) */
    public static float lightning(float t) {
        float period = 12f;
        int n = (int) (t / period);
        float d = t - n * period - (hash(n, 7) % 700) / 100f;
        if (d < 0 || d > 0.7f) return 0f;
        if (d < 0.07f) return 1f;
        if (d < 0.15f) return 0.15f;
        if (d < 0.22f) return 0.85f;
        return 0.85f * (1f - (d - 0.22f) / 0.48f);
    }

    /** the number of the strike happening now, or -1 (lets the title screen play one thunderclap per strike) */
    public static int bolt(float t) {
        float period = 12f;
        int n = (int) (t / period);
        float d = t - n * period - (hash(n, 7) % 700) / 100f;
        return d >= 0 && d < 0.1f ? n : -1;
    }

    /**
     * @param calm for the menus behind the title (world list, options): no lightning, and drawn under a dark veil by
     *             the caller so the text on top stays readable
     */
    public static void render(DrawContext c, int w, int h, boolean calm) {
        float t = time();
        c.draw(() -> scene(c, w, h, t, calm));
    }

    private static void scene(DrawContext c, int w, int h, float t, boolean calm) {
        int gy = (int) (h * 0.84f);
        float flash = calm ? 0f : lightning(t);

        // sky: a sick green dusk going black overhead
        c.fillGradient(0, 0, w, gy, 0xFF060807, 0xFF26301F);
        c.fillGradient(0, gy - h / 5, w, gy, 0x00000000, 0x30704A20);          // the glow of fires on the horizon
        moon(c, w * 0.78f, h * 0.2f, Math.max(10f, h * 0.085f), t);
        if (flash > 0) c.fill(0, 0, w, gy, argb((int) (flash * 110), 0xD8E4FF));

        layer(c, FAR, w, h, gy, t * 3.5f, 0xFF121619, 0x55C9A84A, 6, 8, 2, 3, 7, t, false);
        fog(c, w, gy - (int) (h * 0.2f), (int) (h * 0.22f), 0x9AA69A, 0x2C + (int) (Math.sin(t * 0.4) * 10));
        layer(c, MID, w, h, gy, t * 9f, 0xFF0A0C0E, 0x80D9B458, 8, 10, 3, 4, 12, t, true);
        fog(c, w, gy - (int) (h * 0.09f), (int) (h * 0.13f), 0x8C988C, 0x42 + (int) (Math.sin(t * 0.55 + 1) * 12));

        // the street
        c.fill(0, gy, w, h, 0xFF050605);
        c.fill(0, gy, w, gy + 1, 0xFF1C1F1B);
        wreck(c, w * 0.64f, gy, Math.max(1, h / 150), t);
        zombies(c, w, gy, h, t);
        ash(c, w, h, gy, t, w * 0.64f);

        // vignette
        c.fillGradient(0, 0, w, h / 4, 0xB0000000, 0x00000000);
        c.fillGradient(0, (int) (h * 0.72f), w, h, 0x00000000, 0xD8000000);
    }

    private static void disc(DrawContext c, float cx, float cy, float r, int color, int step) {
        int ri = Math.max(1, (int) r);
        for (int dy = -ri; dy <= ri; dy += step) {
            int half = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
            c.fill((int) cx - half, (int) cy + dy, (int) cx + half + 1, (int) cy + dy + step, color);
        }
    }

    private static void moon(DrawContext c, float cx, float cy, float r, float t) {
        float pulse = 1f + (float) Math.sin(t * 0.6) * 0.04f;
        disc(c, cx, cy, r * 2.3f * pulse, 0x0E8A2A1E, 2);
        disc(c, cx, cy, r * 1.6f * pulse, 0x168A2A1E, 2);
        disc(c, cx, cy, r * 1.2f, 0x22A03424, 1);
        disc(c, cx, cy, r, 0xFF6A1E17, 1);
        disc(c, cx - r * 0.15f, cy - r * 0.15f, r * 0.8f, 0xFF7E271D, 1);
        disc(c, cx + r * 0.3f, cy + r * 0.1f, r * 0.22f, 0xFF5E1A14, 1);
        disc(c, cx - r * 0.35f, cy + r * 0.35f, r * 0.15f, 0xFF5E1A14, 1);
        // a band of cloud drifting across it
        float bx = frac(t * 0.012f) * (r * 8) - r * 4;
        c.fillGradient((int) (cx + bx - r * 2), (int) (cy + r * 0.1f), (int) (cx + bx + r * 2), (int) (cy + r * 0.45f), 0x00101410, 0xB0101410);
    }

    private static void layer(DrawContext c, List<Building> list, int w, int h, int gy, float scroll, int body, int lit,
                              int sx, int sy, int ww, int wh, int litPct, float t, boolean smoke) {
        float off = scroll % TILE;
        for (float base = -off; base < w; base += TILE) {
            for (Building b : list) {
                int x0 = (int) (base + b.x), x1 = (int) (base + b.x + b.w);
                if (x0 > w || x1 < 0) continue;
                int top = gy - (int) (b.h * h);
                switch (b.roof) {
                    case 1 -> {                                                 // antenna, with a red light blinking
                        int ax = (x0 + x1) / 2;
                        c.fill(ax, top - h / 14, ax + 1, top, body);
                        if (frac(t * 0.5f + (b.seed & 15) / 16f) < 0.5f) c.fill(ax - 1, top - h / 14 - 1, ax + 2, top - h / 14 + 1, 0xFFB02018);
                    }
                    case 2 -> {                                                 // broken top, still burning
                        c.fill(x0, top - 6, x0 + (int) (b.w * 0.35f), top, body);
                        c.fill(x0 + (int) (b.w * 0.55f), top - 10, x0 + (int) (b.w * 0.72f), top, body);
                        if (smoke) smoke(c, x0 + b.w * 0.6f, top - 10, h, t, b.seed);
                    }
                    case 3 -> {                                                 // water tank on legs
                        int tx = x0 + (int) (b.w * 0.6f);
                        c.fill(tx, top - 9, tx + 8, top - 4, body);
                        c.fill(tx + 1, top - 4, tx + 2, top, body);
                        c.fill(tx + 6, top - 4, tx + 7, top, body);
                    }
                    case 4 -> c.fill(x0 + (int) (b.w * 0.2f), top - h / 20, x1 - (int) (b.w * 0.2f), top, body);   // stepped
                    default -> { }
                }
                c.fill(x0, top, x1, gy, body);
                // windows: a handful still lit, a few on failing power
                int cols = Math.max(1, (x1 - x0 - 4) / sx), rows = Math.max(1, (gy - top - 6) / sy);
                for (int i = 0; i < cols; i++) {
                    for (int j = 0; j < rows; j++) {
                        int hv = hash(b.seed + i * 31, j);
                        if (hv % 100 >= litPct) continue;
                        if (hv % 9 == 0 && Math.sin(t * (3 + hv % 5) + hv) < -0.2) continue;
                        int wx = x0 + 3 + i * sx, wy = top + 4 + j * sy;
                        c.fill(wx, wy, wx + ww, wy + wh, lit);
                    }
                }
            }
        }
    }

    private static void smoke(DrawContext c, float x, float y, int h, float t, int seed) {
        for (int i = 0; i < 7; i++) {
            float age = frac(t * 0.07f + i / 7f + (seed & 7) / 8f);
            float px = x + age * h * 0.28f + (float) Math.sin(age * 6 + i) * 5;
            float py = y - age * h * 0.5f;
            float r = 3 + age * h * 0.09f;
            disc(c, px, py, r, argb((int) ((1 - age) * 70), 0x1E211E), 2);
        }
    }

    private static void fog(DrawContext c, int w, int y, int bh, int rgb, int alpha) {
        c.fillGradient(0, y, w, y + bh / 2, argb(0, rgb), argb(alpha, rgb));
        c.fillGradient(0, y + bh / 2, w, y + bh, argb(alpha, rgb), argb(0, rgb));
    }

    private static void wreck(DrawContext c, float cx, int gy, int u, float t) {
        int x = (int) cx;
        disc(c, x + 6 * u, gy - 8 * u, 26 * u, 0x14FF7A20, 2);                   // firelight
        c.fill(x - 14 * u, gy - 7 * u, x + 18 * u, gy - 2 * u, 0xFF040404);       // on its side, roof caved in
        c.fill(x - 8 * u, gy - 11 * u, x + 8 * u, gy - 7 * u, 0xFF040404);
        c.fill(x - 11 * u, gy - 2 * u, x - 6 * u, gy, 0xFF040404);
        c.fill(x + 10 * u, gy - 2 * u, x + 15 * u, gy, 0xFF040404);
        for (int k = 0; k < 4; k++) {                                            // flames
            float f = (float) (Math.sin(t * (11 + k * 3) + k * 1.7) * 0.5 + 0.5);
            int fx = x - 4 * u + k * 4 * u, fh = (int) ((3 + f * 6) * u);
            c.fill(fx, gy - 11 * u - fh, fx + 3 * u, gy - 11 * u, 0xC0E0601A);
            c.fill(fx + u, gy - 11 * u - fh / 2, fx + 2 * u, gy - 11 * u, 0xE0FFC040);
        }
        smoke(c, x, gy - 14 * u, 150 * u, t, 3);
    }

    /** the dead, shuffling along the street in both directions; the far ones smaller and greyer */
    private static void zombies(DrawContext c, int w, int gy, int h, float t) {
        int big = Math.max(1, h / 140);
        for (int i = 0; i < 7; i++) {
            boolean far = i >= 4;
            int u = far ? Math.max(1, big - 1) : big;
            int dir = i % 3 == 0 ? 1 : -1;
            float speed = (far ? 4 : 7) + (i * 7 % 5) * 1.3f;
            float span = w + 80;
            float p = (i * 137.5f + t * speed) % span;
            int x = (int) (dir > 0 ? p - 40 : w + 40 - p);
            int foot = gy + (far ? -1 : 2) + (i % 2);
            int col = far ? 0xFF111411 : 0xFF020302;
            float step = (float) Math.sin(t * speed * 0.55f + i * 2.1f);
            int s = (int) (step * 1.6f * u);
            int bob = (int) (Math.abs(step) * u * 0.6f);
            c.fill(x + s, foot - 6 * u, x + s + 2 * u, foot, col);                   // legs
            c.fill(x - s, foot - 6 * u, x - s + 2 * u, foot, col);
            c.fill(x - u + dir * u, foot - 13 * u - bob, x + 3 * u + dir * u, foot - 6 * u, col);            // hunched body
            c.fill(x + dir * 3 * u, foot - 16 * u - bob, x + dir * 3 * u + 3 * u, foot - 13 * u - bob, col);  // head, lolling forward
            int ax0 = dir > 0 ? x + 3 * u : x - 6 * u;
            c.fill(ax0 + dir * u, foot - 12 * u - bob, ax0 + 6 * u + dir * u, foot - 11 * u - bob, col);      // arms out
            c.fill(ax0, foot - 10 * u - bob, ax0 + 5 * u, foot - 9 * u - bob, col);
        }
    }

    private static void ash(DrawContext c, int w, int h, int gy, float t, float fireX) {
        for (int i = 0; i < 56; i++) {
            float sx = frac((float) Math.sin(i * 12.9898) * 43758.547f);
            float sy = frac((float) Math.sin(i * 78.233) * 12345.678f);
            float sp = frac(sx * 7.1f + sy * 3.3f);
            if (i % 6 == 0) {                                                    // embers rising from the wreck
                float a = frac(t * (0.18f + sp * 0.2f) + sy);
                int x = (int) (fireX + (sx - 0.5f) * 40 + Math.sin(t * 2 + i) * 6 + a * 30);
                int y = (int) (gy - 10 - a * h * 0.5f);
                c.fill(x, y, x + 1, y + 1, argb((int) ((1 - a) * 230), 0xFF9A3A));
            } else {                                                             // ash drifting down
                int x = (int) ((sx * w + t * (5 + sp * 9) + Math.sin(t * 0.8 + i) * 7) % w);
                int y = (int) ((sy * h + t * (9 + sp * 12)) % h);
                c.fill(x, y, x + 1, y + 1, argb(70 + (int) (sp * 60), 0xB4B6AE));
            }
        }
    }
}
