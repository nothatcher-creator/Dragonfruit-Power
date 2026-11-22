package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.BarricadeBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;

/** Draws the door, glass or pane a barricade is covering; the boards themselves are the block's own model. */
public class BarricadeRenderer implements BlockEntityRenderer<BarricadeBlockEntity> {
    @Override
    public void render(BarricadeBlockEntity be, float td, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        var st = be.original;
        if (st == null || st.isAir() || st.getRenderType() != BlockRenderType.MODEL) return;
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(st, ms, vcp, light, overlay);
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }
}
