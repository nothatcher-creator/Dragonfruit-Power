package com.nothatcher.vehicleforge.client;

import com.nothatcher.vehicleforge.VehicleForge;
import com.nothatcher.vehicleforge.power.PowerBlock;
import com.nothatcher.vehicleforge.power.PowerBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the wind turbine's tower, nacelle and three turning blades above its base block. */
public class TurbineRenderer implements BlockEntityRenderer<PowerBlockEntity> {
    private static final Identifier TEX = VehicleForge.id("textures/entity/wind_turbine.png");
    private static final float TOWER = 11f;

    @Override
    public void render(PowerBlockEntity be, float td, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        if (!(be.getCachedState().getBlock() instanceof PowerBlock pb) || pb.kind != PowerBlock.Kind.WIND || be.getWorld() == null) return;
        int top = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().up(12));
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
        ms.push();
        ms.translate(0.5, 0, 0.5);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-be.getCachedState().get(PowerBlock.FACING).asRotation()));
        // tapering tower in three sections
        box(ms, vc, -0.2f, 1f, -0.2f, 0.2f, 5f, 0.2f, light, overlay);
        box(ms, vc, -0.16f, 5f, -0.16f, 0.16f, 9f, 0.16f, top, overlay);
        box(ms, vc, -0.13f, 9f, -0.13f, 0.13f, TOWER, 0.13f, top, overlay);
        // nacelle and hub
        box(ms, vc, -0.22f, TOWER, -0.55f, 0.22f, TOWER + 0.45f, 0.45f, top, overlay);
        box(ms, vc, -0.12f, TOWER + 0.1f, -0.75f, 0.12f, TOWER + 0.35f, -0.55f, top, overlay);
        // blades
        float a = MathHelper.lerp(td, be.prevAngle, be.angle);
        ms.translate(0, TOWER + 0.225, -0.7);
        for (int i = 0; i < 3; i++) {
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(a + i * 120f));
            box(ms, vc, -0.07f, 0.1f, -0.03f, 0.07f, 1.2f, 0.03f, top, overlay);
            box(ms, vc, -0.055f, 1.2f, -0.025f, 0.055f, 3.6f, 0.025f, top, overlay);
            box(ms, vc, -0.035f, 3.6f, -0.02f, 0.035f, 4.6f, 0.02f, top, overlay);
            ms.pop();
        }
        ms.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(PowerBlockEntity be) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 192;
    }

    private static void box(MatrixStack ms, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int light, int overlay) {
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();
        quad(vc, m, n, light, overlay, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0.92f);
        quad(vc, m, n, light, overlay, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0.92f);
        quad(vc, m, n, light, overlay, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, 0.8f);
        quad(vc, m, n, light, overlay, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 0.8f);
        quad(vc, m, n, light, overlay, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 1f);
        quad(vc, m, n, light, overlay, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0.6f);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int overlay, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float shade) {
        int c = (int) (255 * shade);
        vc.vertex(m, ax, ay, az).color(c, c, c, 255).texture(0, 1).overlay(overlay).light(light).normal(n, nx, ny, nz).next();
        vc.vertex(m, bx, by, bz).color(c, c, c, 255).texture(1, 1).overlay(overlay).light(light).normal(n, nx, ny, nz).next();
        vc.vertex(m, cx, cy, cz).color(c, c, c, 255).texture(1, 0).overlay(overlay).light(light).normal(n, nx, ny, nz).next();
        vc.vertex(m, dx, dy, dz).color(c, c, c, 255).texture(0, 0).overlay(overlay).light(light).normal(n, nx, ny, nz).next();
    }
}
