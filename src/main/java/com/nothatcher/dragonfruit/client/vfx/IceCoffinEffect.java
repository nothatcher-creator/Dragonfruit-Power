package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Ice Coffin: the target is sealed in a block of ice that freezes from the feet up. */
public class IceCoffinEffect extends VfxEffect {
    private final int entityId;

    public IceCoffinEffect(int entityId, int life) {
        super(life);
        this.entityId = entityId;
    }

    private Box box(Entity e, float td) {
        Vec3d offset = e.getLerpedPos(td).subtract(e.getPos());
        return e.getBoundingBox().offset(offset).expand(0.25, 0.15, 0.25);
    }

    @Override
    protected void onTick(ClientWorld world) {
        Entity e = world.getEntityById(entityId);
        if (e == null) return;
        Box b = box(e, 1f);
        for (int i = 0; i < 2; i++) {
            Vec3d p = new Vec3d(b.minX + world.random.nextDouble() * (b.maxX - b.minX),
                    b.minY + world.random.nextDouble() * (b.maxY - b.minY),
                    b.minZ + world.random.nextDouble() * (b.maxZ - b.minZ));
            Particles.add(ParticleTypes.SNOWFLAKE, p, Vec3d.ZERO);
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        if (mc().world == null) return;
        Entity e = mc().world.getEntityById(entityId);
        if (e == null) return;
        Box b = box(e, td);
        double grow = Gfx.easeOutCubic((age + td) / 6.0);
        Box shown = new Box(b.minX, b.minY, b.minZ, b.maxX, b.minY + (b.maxY - b.minY) * grow, b.maxZ);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer solid = vcp.getBuffer(RenderLayer.getDebugQuads());
        Gfx.box(solid, m, shown, Gfx.argb(0xB5ECFF, 0.5f));
        Gfx.box(solid, m, shown.contract(0.08), Gfx.argb(0xE8FBFF, 0.25f));
        VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
        Gfx.box(glow, m, shown.expand(0.04), Gfx.argb(0x8FDFFF, 0.18f));
    }
}
