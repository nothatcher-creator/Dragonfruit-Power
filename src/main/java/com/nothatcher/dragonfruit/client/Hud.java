package com.nothatcher.dragonfruit.client;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Ability bar centred above the hotbar (out of the way of phone touch controls on both sides),
 * plus full-screen overlays: impact flash, Second Gear / Flame Body vignette, seawater warning.
 */
public final class Hud {
    private static final int CHIP_W = 74, CHIP_H = 18, GAP = 3;

    private Hud() {}

    public static void render(DrawContext ctx, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float time = CameraFx.clock() + td;

        // ---- overlays
        FruitType fruit = ClientPowerData.fruit;
        if (ClientPowerData.buffActive()) {
            int rgb = fruit == FruitType.FLAME ? 0xFF6A00 : 0xFF3A60;
            float pulse = 0.28f + 0.12f * (float) Math.sin(time * (fruit == FruitType.RUBBER ? 0.42 : 0.2));
            int edge = argb(rgb, pulse);
            ctx.fillGradient(0, 0, w, h / 5, edge, argb(rgb, 0));
            ctx.fillGradient(0, h - h / 5, w, h, argb(rgb, 0), edge);
        }
        float flash = CameraFx.flashAlpha(td);
        if (flash > 0.01f) ctx.fill(0, 0, w, h, argb(0xFFFFFF, Math.min(0.85f, flash)));

        if (fruit == FruitType.NONE) return;
        TextRenderer tr = mc.textRenderer;

        if (mc.player.isTouchingWater() && !mc.player.isCreative()) {
            Text warn = Text.translatable("hud.dragonfruit.water");
            int tw = tr.getWidth(warn);
            ctx.drawTextWithShadow(tr, warn, (w - tw) / 2, h / 2 + 18, 0x6FD6FF);
        }

        // ---- ability bar
        int n = fruit.abilities.length;
        int total = 20 + n * CHIP_W + (n - 1) * GAP;
        int x0 = (w - total) / 2;
        int y0 = h - 68 - CHIP_H;
        if (mc.player.isCreative()) y0 += 16; // no hearts in creative

        // fruit icon
        ctx.fill(x0 - 1, y0 - 1, x0 + 18, y0 + CHIP_H + 1, 0xAA000000);
        ItemStack icon = new ItemStack(ModItems.FRUITS.get(fruit));
        ctx.drawItem(icon, x0 + 1, y0 + 1);

        for (int i = 0; i < n; i++) {
            int x = x0 + 20 + i * (CHIP_W + GAP);
            boolean sel = i == ClientPowerData.selected;
            float cd = ClientPowerData.cooldownFraction(i, td);
            int border = sel ? argb(fruit.color, 1f) : 0x88FFFFFF;
            ctx.fill(x - 1, y0 - 1, x + CHIP_W + 1, y0 + CHIP_H + 1, border);
            ctx.fill(x, y0, x + CHIP_W, y0 + CHIP_H, sel ? 0xE0101018 : 0xC0101018);
            if (sel) ctx.fill(x, y0, x + CHIP_W, y0 + 2, argb(fruit.color, 0.9f));
            if (cd > 0) {
                int cw = (int) (CHIP_W * cd);
                ctx.fill(x, y0, x + cw, y0 + CHIP_H, 0xB0303040);
            }

            Text name = fruit.abilityName(i);
            ctx.getMatrices().push();
            float scale = 0.72f;
            ctx.getMatrices().scale(scale, scale, 1f);
            int textW = tr.getWidth(name);
            int maxW = (int) ((CHIP_W - 4) / scale);
            String label = name.getString();
            if (textW > maxW) label = tr.trimToWidth(label, maxW - tr.getWidth("..")) + "..";
            int color = cd > 0 ? 0xFF9A9AA8 : (sel ? 0xFFFFFFFF : 0xFFD8D8E0);
            ctx.drawText(tr, label, (int) ((x + 3) / scale), (int) ((y0 + 4) / scale), color, true);
            String sub;
            if (cd > 0) sub = String.format("%.1fs", ClientPowerData.cooldownLeft(i) / 20f);
            else sub = sel ? "[" + DragonfruitClient.useKeyName() + "] READY" : "ready";
            ctx.drawText(tr, sub, (int) ((x + 3) / scale), (int) ((y0 + 11) / scale), cd > 0 ? 0xFFFF7A7A : argb(fruit.color, 1f), true);
            ctx.getMatrices().pop();
        }

        // buff timer bar
        if (ClientPowerData.buffActive()) {
            int bw = total - 20;
            int left = ClientPowerData.buffLeft();
            int fw = (int) (bw * Math.min(1f, left / 400f));
            ctx.fill(x0 + 20, y0 + CHIP_H + 3, x0 + 20 + bw, y0 + CHIP_H + 5, 0x80000000);
            ctx.fill(x0 + 20, y0 + CHIP_H + 3, x0 + 20 + fw, y0 + CHIP_H + 5, argb(fruit.color, 1f));
        }

        // big name toast when switching abilities
        if (ClientPowerData.selectAge < 40) {
            float a = ClientPowerData.selectAge < 30 ? 1f : 1f - (ClientPowerData.selectAge - 30 + td) / 10f;
            if (a > 0.05f) {
                Text big = fruit.abilityName(ClientPowerData.selected);
                ctx.getMatrices().push();
                ctx.getMatrices().scale(1.6f, 1.6f, 1f);
                int bw = tr.getWidth(big);
                ctx.drawTextWithShadow(tr, big, (int) ((w / 2f) / 1.6f - bw / 2f), (int) ((y0 - 22) / 1.6f), argb(fruit.color, a));
                ctx.getMatrices().pop();
            }
        }
    }

    private static int argb(int rgb, float alpha) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
