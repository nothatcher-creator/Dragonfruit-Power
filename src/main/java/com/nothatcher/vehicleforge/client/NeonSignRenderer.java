package com.nothatcher.vehicleforge.client;

import com.nothatcher.vehicleforge.block.NeonSignBlock;
import com.nothatcher.vehicleforge.block.NeonSignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * Draws a neon sign's text as glowing tubes: bright letters with a deeper-coloured halo around them, at full
 * brightness. Flickering signs stutter off now and then, like a failing transformer.
 */
public class NeonSignRenderer implements BlockEntityRenderer<NeonSignBlockEntity> {
    @Override
    public void render(NeonSignBlockEntity be, float td, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        if (be.getWorld() == null || be.text.isBlank()) return;
        int c = be.color;
        if (be.flicker) {
            long t = be.getWorld().getTime() + be.getPos().asLong() % 97;
            int phase = (int) (t % 120);
            if ((phase > 100 && phase < 104) || phase == 108 || (phase > 60 && phase < 62 && (t / 120) % 3 == 0)) c = dim(c, 0.25f);
        }
        int core = mix(c, 0xFFFFFF, 0.55f), halo = dim(c, 0.85f);
        Direction f = be.getCachedState().get(NeonSignBlock.FACING);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String[] lines = be.text.split("\\|");
        float s = be.size / 48f;                                             // world units per font pixel: size 8 letters are 1.5 blocks tall
        ms.push();
        ms.translate(0.5, 0.5, 0.5);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-f.asRotation()));
        ms.translate(0, 0, -0.40);                                           // just off the wall behind it
        ms.scale(s, -s, s);
        float lineH = 11;
        float y0 = -(lines.length * lineH - 2) / 2f;
        for (int i = 0; i < lines.length; i++) {
            OrderedText t = Text.literal(lines[i]).asOrderedText();
            float w = tr.getWidth(t);
            tr.drawWithOutline(t, -w / 2f, y0 + i * lineH, 0xFF000000 | core, 0xFF000000 | halo, ms.peek().getPositionMatrix(), vcp,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        ms.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(NeonSignBlockEntity be) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 128;
    }

    private static int dim(int c, float k) {
        return ((int) (((c >> 16) & 255) * k) << 16) | ((int) (((c >> 8) & 255) * k) << 8) | (int) ((c & 255) * k);
    }

    private static int mix(int a, int b, float k) {
        int r = (int) (((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = (int) (((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k),
                bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }
}
