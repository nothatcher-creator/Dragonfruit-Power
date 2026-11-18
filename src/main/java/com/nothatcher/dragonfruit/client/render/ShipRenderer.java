package com.nothatcher.dragonfruit.client.render;

import com.nothatcher.dragonfruit.content.ShipEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Original pirate ship built from vanilla block textures: stepped wooden hull, raised stern castle with
 * lit windows, main mast with a billowing striped sail, crow's nest, waving flag, broadside cannons and
 * a green dragon figurehead with a dragonfruit-pink frill. Local space: x = side, y = up, z = toward the bow.
 */
public class ShipRenderer extends EntityRenderer<ShipEntity> {
    private record Part(float x0, float y0, float z0, float x1, float y1, float z1, String tex) {}

    private static final String DARK = "block/dark_oak_planks", SPRUCE = "block/spruce_planks", OAK = "block/oak_planks",
            LOG = "block/stripped_spruce_log", WOOL = "block/white_wool", PINK = "block/magenta_wool", BLACK = "block/black_wool",
            GREEN = "block/green_concrete", LIME = "block/lime_concrete", JAW = "block/green_terracotta", GOLD = "block/gold_block",
            BONE = "block/bone_block_side", IRON = "block/black_concrete", GLOW = "block/glowstone", ROPE = "block/brown_wool";

    private static final List<Part> PARTS = new ArrayList<>();

    static {
        // hull
        box(-1.0f, -0.8f, -4.0f, 1.0f, -0.4f, 4.0f, DARK);
        box(-1.6f, -0.4f, -4.6f, 1.6f, 0.3f, 4.2f, SPRUCE);
        box(-2.0f, 0.3f, -4.8f, 2.0f, 0.6f, 3.8f, DARK);
        mirrorX(1.8f, 0.6f, -4.8f, 2.0f, 1.5f, 3.6f, SPRUCE);               // side walls
        box(-1.8f, 0.6f, -4.8f, 1.8f, 0.7f, 3.6f, OAK);                    // deck
        mirrorX(1.75f, 1.5f, -2.6f, 2.05f, 1.62f, 3.6f, DARK);              // side rails
        // bow
        box(-1.6f, -0.2f, 3.6f, 1.6f, 1.4f, 4.8f, SPRUCE);
        box(-1.1f, 0.0f, 4.8f, 1.1f, 1.5f, 5.6f, SPRUCE);
        box(-0.6f, 0.2f, 5.6f, 0.6f, 1.6f, 6.3f, DARK);
        box(-0.12f, 1.45f, 6.0f, 0.12f, 1.7f, 9.0f, LOG);                  // bowsprit
        // stern castle
        box(-2.0f, 0.6f, -4.8f, 2.0f, 1.9f, -2.6f, SPRUCE);
        box(-1.9f, 1.9f, -4.7f, 1.9f, 2.0f, -2.7f, OAK);
        mirrorX(1.85f, 2.0f, -4.8f, 2.0f, 2.45f, -2.6f, DARK);
        box(-2.0f, 2.0f, -4.85f, 2.0f, 2.45f, -4.7f, DARK);
        mirrorX(0.5f, 0.95f, -4.88f, 1.3f, 1.55f, -4.8f, GLOW);             // stern windows
        box(-0.15f, 2.45f, -2.85f, 0.15f, 2.8f, -2.55f, GLOW);             // lantern
        box(-0.1f, -0.7f, -5.35f, 0.1f, 1.0f, -4.8f, DARK);                // rudder
        // main mast, yards, crow's nest
        box(-0.18f, 0.7f, 0.2f, 0.18f, 10.2f, 0.56f, LOG);
        box(-2.9f, 7.6f, 0.56f, 2.9f, 7.85f, 0.8f, LOG);
        box(-2.6f, 4.2f, 0.56f, 2.6f, 4.4f, 0.8f, LOG);
        box(-0.75f, 8.4f, -0.35f, 0.75f, 8.95f, 1.1f, SPRUCE);
        box(-0.75f, 8.95f, -0.35f, 0.75f, 9.2f, -0.25f, DARK);
        box(-0.75f, 8.95f, 1.0f, 0.75f, 9.2f, 1.1f, DARK);
        // rear mast
        box(-0.12f, 2.0f, -3.9f, 0.12f, 6.4f, -3.62f, LOG);
        box(-1.6f, 5.6f, -3.62f, 1.6f, 5.75f, -3.45f, LOG);
        // rigging ropes (thin) from mast top to hull sides
        mirrorX(1.7f, 1.6f, 0.3f, 1.78f, 7.6f, 0.38f, ROPE);
        // cannons, three per side
        for (double z : ShipEntity.CANNON_Z) {
            mirrorX(1.95f, 0.95f, (float) z - 0.18f, 2.65f, 1.27f, (float) z + 0.18f, IRON);
            mirrorX(1.8f, 0.7f, (float) z - 0.3f, 2.0f, 0.95f, (float) z + 0.3f, DARK);
        }
        // dragon figurehead
        box(-0.32f, 1.3f, 6.1f, 0.32f, 2.35f, 6.75f, GREEN);               // neck
        box(-0.46f, 2.1f, 6.4f, 0.46f, 2.85f, 7.55f, GREEN);               // head
        box(-0.3f, 2.2f, 7.55f, 0.3f, 2.6f, 8.25f, LIME);                  // snout
        box(-0.27f, 1.95f, 7.3f, 0.27f, 2.18f, 8.05f, JAW);                // lower jaw
        mirrorX(0.46f, 2.5f, 7.05f, 0.52f, 2.66f, 7.25f, GOLD);             // eyes
        mirrorX(0.18f, 2.85f, 6.5f, 0.3f, 3.45f, 6.66f, BONE);              // horns
        mirrorX(0.46f, 2.25f, 6.2f, 0.62f, 2.95f, 6.5f, PINK);              // frill
        box(-0.08f, 2.85f, 6.15f, 0.08f, 3.25f, 6.55f, PINK);              // crest spike
        box(-0.06f, 2.6f, 5.9f, 0.06f, 2.95f, 6.2f, PINK);
    }

    private static void box(float x0, float y0, float z0, float x1, float y1, float z1, String tex) {
        PARTS.add(new Part(x0, y0, z0, x1, y1, z1, tex));
    }

    private static void mirrorX(float x0, float y0, float z0, float x1, float y1, float z1, String tex) {
        box(x0, y0, z0, x1, y1, z1, tex);
        box(-x1, y0, z0, -x0, y1, z1, tex);
    }

    public ShipRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.2f;
    }

    @Override
    public Identifier getTexture(ShipEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(ShipEntity ship, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        ms.push();
        float t = ship.age + tickDelta;
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(t * 0.06) * 2.2f));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) Math.sin(t * 0.045) * 1.2f));
        float wobble = ship.getDamageWobbleTicks() - tickDelta;
        if (wobble > 0) {
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(wobble) * wobble * 0.4f * ship.getDamageWobbleSide()));
        }

        Function<Identifier, Sprite> atlas = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f nm = ms.peek().getNormalMatrix();
        for (Part p : PARTS) {
            drawBox(vc, m, nm, atlas.apply(new Identifier(p.tex())), p, light);
        }
        // billowing sails
        drawSail(vc, m, nm, atlas, -2.7f, 2.7f, 4.4f, 7.6f, 0.86f, 0.75f, t, light);
        drawSail(vc, m, nm, atlas, -1.5f, 1.5f, 3.0f, 5.6f, -3.42f, 0.45f, t, light);
        // waving flag on top of the main mast
        drawFlag(vc, m, nm, atlas.apply(new Identifier(BLACK)), atlas.apply(new Identifier(PINK)), t, light);
        ms.pop();
        super.render(ship, yaw, tickDelta, ms, vcp, light);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f nm, float x, float y, float z, float u, float v,
                             float nx, float ny, float nz, int light) {
        vc.vertex(m, x, y, z).color(1f, 1f, 1f, 1f).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(nm, nx, ny, nz).next();
    }

    /** Each face is tiled with one texture per block so planks don't stretch. */
    private static void drawBox(VertexConsumer vc, Matrix4f m, Matrix3f nm, Sprite s, Part p, int light) {
        float x0 = p.x0(), y0 = p.y0(), z0 = p.z0(), x1 = p.x1(), y1 = p.y1(), z1 = p.z1();
        // face: origin, axis A, axis B (lengths), normal
        face(vc, m, nm, s, x0, y1, z0, 1, 0, 0, x1 - x0, 0, 0, 1, z1 - z0, 0, 1, 0, light); // top
        face(vc, m, nm, s, x0, y0, z1, 1, 0, 0, x1 - x0, 0, 0, -1, z1 - z0, 0, -1, 0, light); // bottom
        face(vc, m, nm, s, x0, y0, z1, 1, 0, 0, x1 - x0, 0, 1, 0, y1 - y0, 0, 0, 1, light); // +z
        face(vc, m, nm, s, x1, y0, z0, -1, 0, 0, x1 - x0, 0, 1, 0, y1 - y0, 0, 0, -1, light); // -z
        face(vc, m, nm, s, x1, y0, z1, 0, 0, -1, z1 - z0, 0, 1, 0, y1 - y0, 1, 0, 0, light); // +x
        face(vc, m, nm, s, x0, y0, z0, 0, 0, 1, z1 - z0, 0, 1, 0, y1 - y0, -1, 0, 0, light); // -x
    }

    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f nm, Sprite s,
                             float ox, float oy, float oz,
                             float ax, float ay, float az, float lenA,
                             float bx, float by, float bz, float lenB,
                             float nx, float ny, float nz, int light) {
        float u0 = s.getMinU(), u1 = s.getMaxU(), v0 = s.getMinV(), v1 = s.getMaxV();
        for (float a = 0; a < lenA - 1.0e-4f; a += 1f) {
            float a2 = Math.min(lenA, a + 1f);
            for (float b = 0; b < lenB - 1.0e-4f; b += 1f) {
                float b2 = Math.min(lenB, b + 1f);
                float ua = u0 + (u1 - u0) * 0, ub = u0 + (u1 - u0) * (a2 - a);
                float va = v1 - (v1 - v0) * 0, vb = v1 - (v1 - v0) * (b2 - b);
                vert(vc, m, nm, ox + ax * a + bx * b, oy + ay * a + by * b, oz + az * a + bz * b, ua, va, nx, ny, nz, light);
                vert(vc, m, nm, ox + ax * a2 + bx * b, oy + ay * a2 + by * b, oz + az * a2 + bz * b, ub, va, nx, ny, nz, light);
                vert(vc, m, nm, ox + ax * a2 + bx * b2, oy + ay * a2 + by * b2, oz + az * a2 + bz * b2, ub, vb, nx, ny, nz, light);
                vert(vc, m, nm, ox + ax * a + bx * b2, oy + ay * a + by * b2, oz + az * a + bz * b2, ua, vb, nx, ny, nz, light);
            }
        }
    }

    /** Curved sail with a dragonfruit-pink stripe band, gently breathing in the wind. */
    private static void drawSail(VertexConsumer vc, Matrix4f m, Matrix3f nm, Function<Identifier, Sprite> atlas,
                                 float xa, float xb, float ya, float yb, float z, float bulge, float t, int light) {
        Sprite white = atlas.apply(new Identifier(WOOL));
        Sprite pink = atlas.apply(new Identifier(PINK));
        int cols = 8, rows = 6;
        float breathe = 1f + 0.12f * (float) Math.sin(t * 0.08);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                Sprite s = (j == 2 || j == 3) && (i % 2 == 0 || j == 2) ? pink : white;
                float fx0 = (float) i / cols, fx1 = (float) (i + 1) / cols;
                float fy0 = (float) j / rows, fy1 = (float) (j + 1) / rows;
                float[][] pts = {{fx0, fy0}, {fx1, fy0}, {fx1, fy1}, {fx0, fy1}};
                float[][] uv = {{s.getMinU(), s.getMaxV()}, {s.getMaxU(), s.getMaxV()}, {s.getMaxU(), s.getMinV()}, {s.getMinU(), s.getMinV()}};
                for (int k = 0; k < 4; k++) {
                    float fx = pts[k][0], fy = pts[k][1];
                    float x = xa + (xb - xa) * fx;
                    float y = ya + (yb - ya) * fy;
                    float zz = z + bulge * breathe * (float) (Math.sin(Math.PI * fx) * (0.35 + 0.65 * Math.sin(Math.PI * fy)));
                    vert(vc, m, nm, x, y, zz, uv[k][0], uv[k][1], 0, 0, 1, light);
                }
            }
        }
    }

    private static void drawFlag(VertexConsumer vc, Matrix4f m, Matrix3f nm, Sprite black, Sprite pink, float t, int light) {
        int cols = 6;
        float x0 = 0.18f, len = 1.5f, y0 = 9.3f, y1 = 10.1f, z = 0.38f;
        for (int i = 0; i < cols; i++) {
            float f0 = (float) i / cols, f1 = (float) (i + 1) / cols;
            float w0 = (float) Math.sin(t * 0.35 - f0 * 4) * 0.18f * f0;
            float w1 = (float) Math.sin(t * 0.35 - f1 * 4) * 0.18f * f1;
            Sprite s = i == 2 || i == 3 ? pink : black;
            vert(vc, m, nm, x0 + len * f0, y0, z + w0, s.getMinU(), s.getMaxV(), 0, 0, 1, light);
            vert(vc, m, nm, x0 + len * f1, y0, z + w1, s.getMaxU(), s.getMaxV(), 0, 0, 1, light);
            vert(vc, m, nm, x0 + len * f1, y1, z + w1, s.getMaxU(), s.getMinV(), 0, 0, 1, light);
            vert(vc, m, nm, x0 + len * f0, y1, z + w0, s.getMinU(), s.getMinV(), 0, 0, 1, light);
        }
    }
}
