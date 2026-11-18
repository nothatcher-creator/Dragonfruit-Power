package com.nothatcher.dragonfruit.client.render;

import com.nothatcher.dragonfruit.DragonfruitMod;
import com.nothatcher.dragonfruit.client.vfx.Gfx;
import com.nothatcher.dragonfruit.content.SeaKingEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Sea King: a deep-teal serpent with a pale belly glow, orange dorsal fins, a crested head,
 * glowing yellow eyes and a snapping jaw. The body follows the path the head swam.
 */
public class SeaKingRenderer extends EntityRenderer<SeaKingEntity> {
    private static final int SEGMENTS = 30;

    public SeaKingRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public Identifier getTexture(SeaKingEntity entity) {
        return DragonfruitMod.id("textures/entity/sea_king.png");
    }

    @Override
    public boolean shouldRender(SeaKingEntity entity, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.getBoundingBox().expand(18));
    }

    @Override
    public void render(SeaKingEntity e, float yaw, float td, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        Vec3d base = e.getLerpedPos(td);
        Vec3d savedCam = Gfx.cam;
        Gfx.cam = base; // the matrix stack is already at the entity, so draw relative to it
        try {
            draw(e, base, td, ms, vcp, light);
        } finally {
            Gfx.cam = savedCam;
        }
        super.render(e, yaw, td, ms, vcp, light);
    }

    private void draw(SeaKingEntity e, Vec3d base, float td, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        float bright = Math.max(LightmapTextureManager.getBlockLightCoordinates(light), LightmapTextureManager.getSkyLightCoordinates(light)) / 15f;
        float shade = 0.25f + 0.75f * bright;
        boolean hurt = e.hurtTime > 0;
        double t = e.age + td;

        Vec3d fwd = e.getRotationVec(td);
        Vec3d right = fwd.crossProduct(Gfx.UP);
        if (right.lengthSquared() < 1.0E-4) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(fwd).normalize();
        Vec3d head = base.add(0, 1.2, 0);

        // body spine: follow the trail, padding backward along the facing if it's short
        List<Vec3d> spine = new ArrayList<>();
        List<Vec3d> trail = new ArrayList<>(e.trail);
        for (int k = 1; k <= SEGMENTS; k++) {
            int idx = k * 2;
            Vec3d p;
            if (idx < trail.size()) {
                Vec3d tp = trail.get(idx);
                p = tp.add(0, 1.2, 0);
            } else {
                Vec3d last = spine.isEmpty() ? head : spine.get(spine.size() - 1);
                p = last.subtract(fwd.multiply(0.6));
            }
            // keep segments spaced so a still serpent doesn't bunch up
            Vec3d prev = spine.isEmpty() ? head : spine.get(spine.size() - 1);
            Vec3d d = p.subtract(prev);
            if (d.length() < 0.55) p = prev.add(d.lengthSquared() < 1.0E-4 ? fwd.multiply(-0.6) : d.normalize().multiply(0.6));
            // swim undulation
            double sway = Math.sin(t * 0.25 - k * 0.45) * 0.25 * Math.min(1, k / 4.0);
            spine.add(p.add(right.multiply(sway)));
        }

        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer solid = vcp.getBuffer(RenderLayer.getDebugQuads());
        int body = tint(0x1C6E78, shade, hurt);
        int fin = tint(0xFF7A1A, shade, hurt);
        Vec3d light3 = new Vec3d(0.3, 1, 0.2);
        for (int k = spine.size() - 1; k >= 0; k--) {
            double r = 1.15 * (1 - k / (double) (SEGMENTS + 6)) + 0.12;
            Gfx.shadedSphere(solid, m, spine.get(k), r, body, 1f, 6, 10, light3);
            if (k % 2 == 0 && k < SEGMENTS - 2) {
                Vec3d p = spine.get(k);
                Vec3d next = k > 0 ? spine.get(k - 1) : head;
                Vec3d dir = next.subtract(p).normalize();
                Vec3d top = p.add(0, r * 0.9, 0);
                Gfx.tri(solid, m, top.subtract(dir.multiply(r * 0.5)), top.add(dir.multiply(r * 0.5)),
                        top.add(0, r * 0.9 + 0.3, 0).subtract(dir.multiply(r * 0.4)), fin);
            }
        }
        // tail fin
        Vec3d tail = spine.get(spine.size() - 1);
        Vec3d tailDir = tail.subtract(spine.get(spine.size() - 2)).normalize();
        Gfx.tri(solid, m, tail.add(0, 0.3, 0), tail.add(tailDir.multiply(1.6)).add(0, 1.0, 0), tail.add(tailDir.multiply(1.2)), fin);
        Gfx.tri(solid, m, tail.add(0, -0.3, 0), tail.add(tailDir.multiply(1.6)).add(0, -1.0, 0), tail.add(tailDir.multiply(1.2)), fin);

        // head: skull, snout, snapping jaw, crest, teeth
        double jaw = e.jawTicks > 0 ? Math.sin((12 - e.jawTicks + td) / 12.0 * Math.PI) : 0.15 + 0.05 * Math.sin(t * 0.1);
        orientedBox(solid, m, head.add(fwd.multiply(0.4)), fwd, right, up, 0.95, 0.75, 1.2, body, shade);
        orientedBox(solid, m, head.add(fwd.multiply(1.9)).add(up.multiply(0.15)), fwd, right, up, 0.65, 0.32, 0.85, tint(0x247F8A, shade, hurt), shade);
        Vec3d jawFwd = fwd.multiply(Math.cos(jaw * 0.7)).subtract(up.multiply(Math.sin(jaw * 0.7)));
        Vec3d jawUp = right.crossProduct(jawFwd).normalize();
        Vec3d hinge = head.add(fwd.multiply(0.9)).subtract(up.multiply(0.35));
        orientedBox(solid, m, hinge.add(jawFwd.multiply(0.95)), jawFwd, right, jawUp, 0.58, 0.18, 0.9, tint(0xD8C9A0, shade, hurt), shade);
        int toothCol = tint(0xFFFFFF, shade, false);
        for (int i = -2; i <= 2; i++) {
            Vec3d at = head.add(fwd.multiply(1.6 + Math.abs(i) * -0.15)).add(right.multiply(i * 0.22)).subtract(up.multiply(0.15));
            Gfx.tri(solid, m, at.add(fwd.multiply(0.08)), at.subtract(fwd.multiply(0.08)), at.subtract(up.multiply(0.3)), toothCol);
        }
        for (int i = 0; i < 4; i++) {
            Vec3d at = head.add(up.multiply(0.75)).subtract(fwd.multiply(0.2 + i * 0.35));
            Gfx.tri(solid, m, at.add(fwd.multiply(0.2)), at.subtract(fwd.multiply(0.2)), at.add(up.multiply(0.7 - i * 0.12)).subtract(fwd.multiply(0.35)), fin);
        }

        // glowing eyes
        VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
        for (int s = -1; s <= 1; s += 2) {
            Vec3d eye = head.add(fwd.multiply(0.9)).add(right.multiply(s * 0.8)).add(up.multiply(0.35));
            Gfx.sphere(glow, m, eye, 0.16, Gfx.argb(0xFFE14A, 0.95f), 5, 8);
            Gfx.sphere(glow, m, eye, 0.32, Gfx.argb(0xFFB000, 0.25f), 5, 8);
        }
    }

    private static int tint(int rgb, float shade, boolean hurt) {
        int r = (int) (((rgb >> 16) & 0xFF) * shade), g = (int) (((rgb >> 8) & 0xFF) * shade), b = (int) ((rgb & 0xFF) * shade);
        if (hurt) {
            r = Math.min(255, r + 120);
            g = g / 2;
            b = b / 2;
        }
        return (r << 16) | (g << 8) | b;
    }

    /** Box aligned to the given frame, faces shaded by direction. */
    private static void orientedBox(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d f, Vec3d r, Vec3d u,
                                    double hf, double hu, double hl, int rgb, float shade) {
        // note: hf = half width (right), hu = half height (up), hl = half length (forward)
        Vec3d[] corners = new Vec3d[8];
        int idx = 0;
        for (int i = -1; i <= 1; i += 2) for (int j = -1; j <= 1; j += 2) for (int k = -1; k <= 1; k += 2) {
            corners[idx++] = c.add(r.multiply(i * hf)).add(u.multiply(j * hu)).add(f.multiply(k * hl));
        }
        // index = (i,j,k) -> ((i+1)/2)*4 + ((j+1)/2)*2 + (k+1)/2
        int top = Gfx.argb(rgb, 1f), side = Gfx.argb(scale(rgb, 0.8f), 1f), bottom = Gfx.argb(scale(rgb, 0.55f), 1f);
        Gfx.quad(vc, m, corners[2], corners[3], corners[7], corners[6], top);     // +u
        Gfx.quad(vc, m, corners[0], corners[1], corners[5], corners[4], bottom);  // -u
        Gfx.quad(vc, m, corners[4], corners[5], corners[7], corners[6], side);    // +r
        Gfx.quad(vc, m, corners[0], corners[1], corners[3], corners[2], side);    // -r
        Gfx.quad(vc, m, corners[1], corners[3], corners[7], corners[5], top);     // +f
        Gfx.quad(vc, m, corners[0], corners[2], corners[6], corners[4], bottom);  // -f
    }

    private static int scale(int rgb, float s) {
        int r = (int) (((rgb >> 16) & 0xFF) * s), g = (int) (((rgb >> 8) & 0xFF) * s), b = (int) ((rgb & 0xFF) * s);
        return (r << 16) | (g << 8) | b;
    }
}
