package com.nothatcher.vehicleforge.client.render;

import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.VehicleForge;
import com.nothatcher.vehicleforge.VehicleType.Category;
import com.nothatcher.vehicleforge.client.model.Mat;
import com.nothatcher.vehicleforge.client.model.Model;
import com.nothatcher.vehicleforge.client.model.VehicleModels;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws any vehicle from its code-built Model, animating wheels, steering, gauges, rotors, turrets, sirens. */
public class VehicleRenderer extends EntityRenderer<VehicleEntity> {
    public static final Identifier TEXTURE = VehicleForge.id("textures/entity/vehicle_palette.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    /** the traffic driver: a seated person at the wheel of city traffic */
    private final net.minecraft.client.render.entity.model.PlayerEntityModel<net.minecraft.entity.LivingEntity> driver;

    public VehicleRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.2f;
        this.driver = new net.minecraft.client.render.entity.model.PlayerEntityModel<>(
                ctx.getPart(net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER), false);
    }

    private void drawDriver(VehicleEntity e, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        if (e.type.seats.isEmpty() || !e.getPassengerList().isEmpty()) return;
        Vec3d s = e.type.seats.get(0);
        var m = driver;
        m.child = false;
        m.sneaking = false;
        m.riding = true;
        m.head.pitch = 0;
        m.head.yaw = (float) Math.sin((e.age + e.getId() * 37) * 0.02) * 0.35f;     // glancing about
        m.body.pitch = 0;
        m.rightLeg.pitch = -1.4137167f;
        m.rightLeg.yaw = 0.31415927f;
        m.rightLeg.roll = 0.07853982f;
        m.leftLeg.pitch = -1.4137167f;
        m.leftLeg.yaw = -0.31415927f;
        m.leftLeg.roll = -0.07853982f;
        m.rightArm.pitch = -1.05f;                                            // hands on the wheel
        m.rightArm.yaw = -0.25f;
        m.rightArm.roll = 0;
        m.leftArm.pitch = -1.05f;
        m.leftArm.yaw = 0.25f;
        m.leftArm.roll = 0;
        m.hat.copyTransform(m.head);
        m.rightSleeve.copyTransform(m.rightArm);
        m.leftSleeve.copyTransform(m.leftArm);
        m.rightPants.copyTransform(m.rightLeg);
        m.leftPants.copyTransform(m.leftLeg);
        m.jacket.copyTransform(m.body);
        ms.push();
        ms.translate(s.x, s.y - 0.35, s.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        ms.scale(-1, -1, 1);
        ms.translate(0, -1.501, 0);
        Identifier skin = net.minecraft.client.util.DefaultSkinHelper.getTexture(e.getUuid());   // one of the default skins
        m.render(ms, vcp.getBuffer(m.getLayer(skin)), light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
        ms.pop();
    }

    @Override
    public Identifier getTexture(VehicleEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(VehicleEntity e, float yaw, float td, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        Model model = VehicleModels.get(e.type.id);
        float pitch = MathHelper.lerp(td, e.prevPitch, e.getPitch());
        float roll = MathHelper.lerp(td, e.prevRoll, e.roll);
        ms.push();
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        if (e.type.wheeled()) {
            // suspension: terrain, squat/dive and body roll
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(td, e.prevBodyPitch, e.bodyPitch)));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(td, e.prevBodyRoll, e.bodyRoll)));
        }
        if (e.isWreck()) {
            // flat tyres and a broken axle: sitting low and lopsided
            int h = e.getUuid().hashCode();
            ms.translate(0, -0.14, 0);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(((h & 7) - 3.5f) * 1.2f));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((((h >> 3) & 7) - 3.5f) * 0.8f));
        }
        float scale = MathHelper.lerp(td, e.prevScale, e.scaleNow);
        if (scale != 1f) ms.scale(scale, scale, scale);
        float wobble = e.getWobble() - td;
        if (wobble > 0) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(wobble) * wobble * 0.3f));
        draw(model, e, td, yaw, ms, vcp, light);
        if (e.upgrades() != 0) draw(VehicleModels.upgrades(e.type, e.upgrades()), e, td, yaw, ms, vcp, light);
        if (e.type.buildable) {
            // whatever's been built on the frame rides along with it
            var brm = net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager();
            for (VehicleEntity.Built b : e.built()) {
                if (b.state().getRenderType() != net.minecraft.block.BlockRenderType.MODEL) continue;
                ms.push();
                ms.translate(b.x() - 0.5, VehicleEntity.DECK + b.y(), b.z() - 0.5);
                brm.renderBlockAsEntity(b.state(), ms, vcp, light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV);
                ms.pop();
            }
        }
        if (e.aiDriven()) drawDriver(e, ms, vcp, light);
        if (e.lightsOn && !model.lights.isEmpty()) beams(model, ms, vcp);
        if (model.hasPlate) plate(model, e, ms, vcp, light);
        ms.pop();

        if (e.trailer() > 0) {
            Model tm = VehicleModels.get(com.nothatcher.vehicleforge.Upgrades.TRAILER_IDS[e.trailer() - 1]);
            VehicleEntity.CarriagePose pose = e.trailerPose();
            Vec3d off = pose.pos().subtract(e.getLerpedPos(td));
            ms.push();
            ms.translate(off.x, off.y, off.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-pose.yaw()));
            draw(tm, e, td, pose.yaw(), ms, vcp, light);
            ms.pop();
        }
        if (e.type.category == Category.TRAIN) {
            Model car = VehicleModels.get(e.type.carriageModel);
            Vec3d base = e.getLerpedPos(td);
            for (int k = 1; k <= e.getCarriages(); k++) {
                VehicleEntity.CarriagePose pose = e.carriagePose(k);
                Vec3d off = pose.pos().subtract(base);
                ms.push();
                ms.translate(off.x, off.y, off.z);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-pose.yaw()));
                draw(car, e, td, pose.yaw(), ms, vcp, light);
                ms.pop();
            }
        }
        super.render(e, yaw, td, ms, vcp, light);
    }

    private void draw(Model model, VehicleEntity e, float td, float vehicleYaw, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        int paint = e.getPaint();
        float pr = ((paint >> 16) & 0xFF) / 255f, pg = ((paint >> 8) & 0xFF) / 255f, pb = (paint & 0xFF) / 255f;
        int overlay = e.getWobble() > 0 ? OverlayTexture.packUv(OverlayTexture.getU(0f), OverlayTexture.getV(true)) : OverlayTexture.DEFAULT_UV;
        float steer = MathHelper.lerp(td, e.prevSteerVisual, e.steerVisual);
        float spin = MathHelper.lerp(td, e.prevWheelSpin, e.wheelSpin);
        float rotor = MathHelper.lerp(td, e.prevRotorSpin, e.rotorSpin);
        float speedFrac = MathHelper.clamp(e.displaySpeed() / Math.max(0.1f, e.type.maxSpeed), 0, 1.1f);

        float aimYaw = 0, aimPitch = 0;
        LivingEntity driver = e.getControllingPassenger();
        if (driver != null) {
            float head = MathHelper.lerp(td, driver.prevHeadYaw, driver.getHeadYaw());
            aimYaw = MathHelper.wrapDegrees(head - vehicleYaw);
            aimPitch = MathHelper.lerp(td, driver.prevPitch, driver.getPitch());
        }
        boolean sirenPhase = (e.age / 4) % 2 == 0;
        // turn signals blink while steering, hazards when badly damaged
        boolean blink = (e.age / 6) % 2 == 0;
        boolean hazard = e.getHealth() < e.type.health * 0.25f;
        boolean leftOn = blink && (hazard || (e.steerVisual > 0.3f && e.engineRunning()));
        boolean rightOn = blink && (hazard || (e.steerVisual < -0.3f && e.engineRunning()));
        boolean reversing = e.gear < 0 && e.engineRunning();
        boolean ability = e.abilityOn();
        boolean wreck = e.isWreck();

        for (Mat.Layer layer : Mat.Layer.values()) {
            VertexConsumer vc = vcp.getBuffer(switch (layer) {
                case SOLID -> RenderLayer.getEntityCutoutNoCull(TEXTURE);
                case GLASS -> RenderLayer.getEntityTranslucent(TEXTURE);
                case GLOW -> RenderLayer.getEntityTranslucentEmissive(TEXTURE);
            });
            for (Model.Group g : model.groups) {
                if (g.kind == Model.Kind.FLAME && !e.thrusting) continue;
                if (g.kind == Model.Kind.SIREN_A && !(e.sirenOn() && sirenPhase)) continue;
                if (g.kind == Model.Kind.SIREN_B && !(e.sirenOn() && !sirenPhase)) continue;
                if (g.kind == Model.Kind.ABILITY && !ability) continue;
                boolean wave = g.kind == Model.Kind.FLAG;
                ms.push();
                transform(g, ms, steer, spin, rotor, speedFrac, aimYaw, aimPitch, e, td);
                Matrix4f m = ms.peek().getPositionMatrix();
                Matrix3f nm = ms.peek().getNormalMatrix();
                for (Model.Quad q : g.quads) {
                    // lamps only glow when they're on: headlights at night, brake lights when slowing
                    Mat.Layer ql = q.mat.layer;
                    float lamp = 1f;
                    if (q.mat == Mat.HEADLIGHT && !e.lightsOn) {
                        ql = Mat.Layer.SOLID;
                        lamp = 0.8f;
                    } else if (q.mat == Mat.INDICATOR) {
                        if (!(q.side > 0 ? leftOn : rightOn)) {
                            ql = Mat.Layer.SOLID;
                            lamp = 0.7f;
                        }
                    } else if (q.mat == Mat.REVERSE) {
                        if (!reversing) {
                            ql = Mat.Layer.SOLID;
                            lamp = 0.75f;
                        }
                    } else if (q.mat == Mat.TAILLIGHT) {
                        if (e.braking) lamp = 1f;
                        else if (e.lightsOn) lamp = 0.55f;
                        else {
                            ql = Mat.Layer.SOLID;
                            lamp = 0.6f;
                        }
                    }
                    if (wreck && ql == Mat.Layer.GLOW) ql = Mat.Layer.SOLID;      // dead lamps
                    if (ql != layer) continue;
                    int qh = wreck ? System.identityHashCode(q) * 0x9E3779B1 : 0;
                    if (wreck && layer == Mat.Layer.GLASS && (qh >>> 28) < 11) continue;   // most of the glass is smashed
                    float r = lamp, gr = lamp, b = lamp;
                    if (q.mat.tint > 0) {
                        r = pr * q.mat.tint;
                        gr = pg * q.mat.tint;
                        b = pb * q.mat.tint;
                    }
                    if (wreck) {
                        // soot and rust, patchy per panel
                        float soot = 0.35f + ((qh >>> 20) & 15) / 15f * 0.35f;
                        float rust = ((qh >>> 12) & 3) == 0 ? 1f : 0f;
                        r = r * soot + rust * 0.22f;
                        gr = gr * soot * 0.9f + rust * 0.09f;
                        b = b * soot * 0.8f;
                    }
                    int l = layer == Mat.Layer.GLOW ? FULL_BRIGHT : light;
                    for (int i = 0; i < 4; i++) {
                        float vy = q.p[i * 3 + 1];
                        if (wave) vy += (float) Math.sin(q.p[i * 3 + 2] * 2.2 + (e.age + td) * 0.35) * 0.07f
                                * (0.4f + Math.abs(q.p[i * 3]));
                        vc.vertex(m, q.p[i * 3], vy, q.p[i * 3 + 2])
                                .color(r, gr, b, 1f)
                                .texture(q.uv[i * 2], q.uv[i * 2 + 1])
                                .overlay(overlay)
                                .light(l)
                                .normal(nm, q.nx, q.ny, q.nz)
                                .next();
                    }
                }
                ms.pop();
            }
        }
    }

    /** Soft additive cones in front of each headlight. */
    private static void beams(Model model, MatrixStack ms, VertexConsumerProvider vcp) {
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        Matrix4f m = ms.peek().getPositionMatrix();
        float len = 11f, spread = 2.4f, drop = 1.1f;
        int segs = 10;
        for (org.joml.Vector3f l : model.lights) {
            float x0 = l.x, y0 = l.y, z0 = model.lightZ + 0.05f;
            for (int i = 0; i < segs; i++) {
                double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
                float sx0 = (float) Math.cos(a0) * 0.12f, sy0 = (float) Math.sin(a0) * 0.12f;
                float sx1 = (float) Math.cos(a1) * 0.12f, sy1 = (float) Math.sin(a1) * 0.12f;
                float ex0 = (float) Math.cos(a0) * spread, ey0 = (float) Math.sin(a0) * spread * 0.55f;
                float ex1 = (float) Math.cos(a1) * spread, ey1 = (float) Math.sin(a1) * spread * 0.55f;
                float[][] quad = {
                        {x0 + sx0, y0 + sy0, z0, 0.2f}, {x0 + sx1, y0 + sy1, z0, 0.2f},
                        {x0 + ex1, y0 + ey1 - drop, z0 + len, 0f}, {x0 + ex0, y0 + ey0 - drop, z0 + len, 0f}
                };
                for (int k = 0; k < 4; k++) beamVertex(vc, m, quad[k]);
                for (int k = 3; k >= 0; k--) beamVertex(vc, m, quad[k]);
            }
        }
    }

    private void plate(Model model, VehicleEntity e, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        if (dispatcher.camera == null || e.squaredDistanceTo(dispatcher.camera.getPos()) > 24 * 24) return;
        String text = plateText(e);
        ms.push();
        ms.translate(model.plateX, model.plateY, model.plateZ - 0.004f);
        float s = 0.0105f;
        ms.scale(-s, -s, s);
        int w = getTextRenderer().getWidth(text);
        getTextRenderer().draw(text, -w / 2f, -3.5f, 0x1A2A55, false, ms.peek().getPositionMatrix(), vcp,
                net.minecraft.client.font.TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
        ms.pop();
    }

    /** a stable plate number per vehicle, e.g. "VF 482K" */
    private static String plateText(VehicleEntity e) {
        long h = e.getUuid().getLeastSignificantBits() ^ e.getUuid().getMostSignificantBits();
        int n = (int) Math.floorMod(h, 900L) + 100;
        char c = (char) ('A' + Math.floorMod(h >> 12, 26L));
        return "VF " + n + c;
    }

    private static void beamVertex(VertexConsumer vc, Matrix4f m, float[] p) {
        vc.vertex(m, p[0], p[1], p[2]).color(1f, 0.95f, 0.8f, p[3]).next();
    }

    private static void transform(Model.Group g, MatrixStack ms, float steer, float spin, float rotor, float speedFrac,
                                  float aimYaw, float aimPitch, VehicleEntity e, float td) {
        float px = g.pivot.x, py = g.pivot.y, pz = g.pivot.z;
        switch (g.kind) {
            case WHEEL -> {
                ms.translate(px, py, pz);
                if (g.steers) ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(steer * 28f));
                ms.multiply(RotationAxis.POSITIVE_X.rotation(spin * 0.4f / Math.max(0.1f, g.param)));
                ms.translate(-px, -py, -pz);
            }
            case STEER_WHEEL -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-g.param));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-steer * 110f));
                ms.translate(-px, -py, -pz);
            }
            case NEEDLE -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f - speedFrac * 220f));
                ms.translate(-px, -py, -pz);
            }
            case ROTOR_Y -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotor));
                ms.translate(-px, -py, -pz);
            }
            case ROTOR_X -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotor * 1.4f));
                ms.translate(-px, -py, -pz);
            }
            case PROP_Z -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotor * 1.2f));
                ms.translate(-px, -py, -pz);
            }
            case TURRET -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-aimYaw));
                ms.translate(-px, -py, -pz);
            }
            case BARREL, LADDER -> {
                float lo = g.kind == Model.Kind.LADDER ? -45 : -25, hi = g.kind == Model.Kind.LADDER ? 30 : 10;
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-aimYaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(aimPitch, lo, hi)));
                ms.translate(-px, -py, -pz);
            }
            case BIKE_FORK -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(steer * 25f));
                ms.translate(-px, -py, -pz);
            }
            case WIPER -> {
                float sweep = 0;
                if (e.getWorld().isRaining() && e.engineRunning()) {
                    float t = (e.age + td) * 0.22f;
                    sweep = (1 - MathHelper.cos(t)) * 0.5f * 80f;
                }
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g.param));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-sweep));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-g.param));
                ms.translate(-px, -py, -pz);
            }
            case LEG_L, LEG_R -> {
                float gait = spin * 0.55f;
                float amp = MathHelper.clamp(Math.abs(e.clientSpeed) * 70f, 0f, 40f);
                float a = MathHelper.sin(gait + (g.kind == Model.Kind.LEG_R ? MathHelper.PI : 0)) * amp;
                if (!e.isOnGround()) a = g.kind == Model.Kind.LEG_L ? -30 : 20;      // tucked legs in a jump
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(a));
                ms.translate(-px, -py, -pz);
            }
            case NECK -> {
                float gait = spin * 0.55f;
                float bob = MathHelper.sin(gait * 2) * Math.min(8f, Math.abs(e.clientSpeed) * 20f)
                        + Math.min(25f, Math.abs(e.clientSpeed) * 25f);
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bob));
                ms.translate(-px, -py, -pz);
            }
            case BALL -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotation(spin * 0.4f / Math.max(0.1f, g.param)));
                ms.translate(-px, -py, -pz);
            }
            case DRUM -> {
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g.param));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((e.age + td) * 6f));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-g.param));
                ms.translate(-px, -py, -pz);
            }
            case FORKS -> ms.translate(0, MathHelper.lerp(td, e.prevForkHeight, e.forkHeight), 0);
            case BOOM -> {
                float tx = (float) e.type.turret.x, ty = (float) e.type.turret.y, tz = (float) e.type.turret.z;
                ms.translate(tx, ty, tz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-aimYaw));
                ms.translate(-tx, -ty, -tz);
                ms.translate(px, py, pz);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(aimPitch, -60, g.param)));
                ms.translate(-px, -py, -pz);
            }
            case FLAME -> {
                float f = 0.85f + 0.3f * (float) Math.abs(Math.sin(e.age * 1.7 + rotor));
                ms.translate(px, py, pz);
                ms.scale(f, f, f);
                ms.translate(-px, -py, -pz);
            }
            default -> { }
        }
    }
}
