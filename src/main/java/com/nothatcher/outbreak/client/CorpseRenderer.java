package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.CorpseEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a corpse as the zombie it was, lying on its side exactly as the dying animation left it: a stand-in zombie
 * (same UUID, so the same skin; same clothes) drawn with the death tilt applied by hand, so it isn't tinted red.
 */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    private final Map<CorpseEntity, ZombieEntity> standIns = new WeakHashMap<>();

    public CorpseRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    private ZombieEntity standIn(CorpseEntity c) {
        ZombieEntity z = standIns.get(c);
        if (z == null) {
            z = EntityType.ZOMBIE.create(c.getWorld());
            if (z == null) return null;
            standIns.put(c, z);
        }
        c.who().ifPresent(z::setUuid);
        z.setBaby(c.baby());
        for (int i = 0; i < CorpseEntity.SLOTS.length; i++) z.equipStack(CorpseEntity.SLOTS[i], c.worn(i));
        z.setPosition(c.getX(), c.getY(), c.getZ());
        z.bodyYaw = z.prevBodyYaw = z.headYaw = z.prevHeadYaw = 180f;
        z.setYaw(180f);
        z.setPitch(0f);
        return z;
    }

    @Override
    public void render(CorpseEntity c, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        ZombieEntity z = standIn(c);
        if (z == null) return;
        float body = c.bodyYaw();
        double r = Math.toRadians(body), reach = z.getHeight() * 0.47;
        matrices.push();
        // back to where its feet were, a touch off the ground, then the same turn and tilt as the death animation
        matrices.translate(-Math.cos(r) * reach, 0.12, -Math.sin(r) * reach);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - body));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f));
        EntityRenderer<? super ZombieEntity> zr = dispatcher.getRenderer(z);
        zr.render(z, 180f, tickDelta, matrices, consumers, light);
        matrices.pop();
        super.render(c, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(CorpseEntity c) {
        return c.who().map(CivilianZombieRenderer::skinFor).orElse(new Identifier("textures/entity/zombie/zombie.png"));
    }
}
