package com.nothatcher.dragonfruit.client.render;

import com.nothatcher.dragonfruit.DragonfruitMod;
import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.client.ClientPowerData;
import com.nothatcher.dragonfruit.client.vfx.VfxManager;
import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

/**
 * Fruit body effects drawn over the player model (original looks, one per fruit):
 * glowing, animated patterns (flame flicker, frost lines, magma cracks, crackling electricity...),
 * plus black hardened arms while Willpower Armor is active.
 */
public class FruitAuraFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final int FRAMES = 4;
    private static final Map<FruitType, Identifier[]> TEXTURES = new EnumMap<>(FruitType.class);
    private static final Identifier ARMOR = DragonfruitMod.id("textures/entity/aura/armor.png");

    static {
        for (FruitType f : FruitType.values()) {
            if (f == FruitType.NONE || f == FruitType.RUBBER) continue;
            Identifier[] ids = new Identifier[FRAMES];
            for (int i = 0; i < FRAMES; i++) ids[i] = DragonfruitMod.id("textures/entity/aura/" + f.id + "_" + i + ".png");
            TEXTURES.put(f, ids);
        }
    }

    public FruitAuraFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        PlayerEntityModel<AbstractClientPlayerEntity> model = getContextModel();

        if (VfxManager.hasAura(player.getId(), Vfx.A_ARMOR)) {
            model.render(matrices, vcp.getBuffer(RenderLayer.getEntityTranslucent(ARMOR)), light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
        }

        FruitType fruit = ClientPowerData.fruitOf(player);
        Identifier[] frames = TEXTURES.get(fruit);
        if (frames == null) return;
        int frame = (int) ((player.age + tickDelta) / 3f) % FRAMES;
        float pulse = 0.75f + 0.25f * (float) Math.sin((player.age + tickDelta) * 0.15f);
        model.render(matrices, vcp.getBuffer(RenderLayer.getEyes(frames[frame])), 0xF000F0, OverlayTexture.DEFAULT_UV,
                pulse, pulse, pulse, 1f);
    }
}
