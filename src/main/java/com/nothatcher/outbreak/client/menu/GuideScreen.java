package com.nothatcher.outbreak.client.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** The Survival Guide: a few pages on how Outbreak works, flipped with the buttons (or the arrow keys). */
public class GuideScreen extends Screen {
    private static final int PAGES = 9;
    private final Screen parent;
    private int page = 0;
    private MenuButton prev, next;
    private int boxX, boxY, boxW, boxH;

    public GuideScreen(Screen parent) {
        super(Text.translatable("menu.outbreak.guide"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boxW = MathHelper.clamp(width - 40, 200, 360);
        boxH = MathHelper.clamp(height - 64, 120, 240);
        boxX = (width - boxW) / 2;
        boxY = Math.max(8, (height - boxH) / 2 - 12);
        int by = boxY + boxH + 6, bw = (boxW - 8) / 3;
        prev = addDrawableChild(new MenuButton(boxX, by, bw, 20, Text.translatable("menu.outbreak.prev"), b -> flip(-1), false));
        addDrawableChild(new MenuButton(boxX + bw + 4, by, bw, 20, Text.translatable("menu.outbreak.done"), b -> close(), true));
        next = addDrawableChild(new MenuButton(boxX + (bw + 4) * 2, by, boxW - (bw + 4) * 2, 20, Text.translatable("menu.outbreak.next"), b -> flip(1), false));
        update();
    }

    private void flip(int d) {
        page = MathHelper.clamp(page + d, 0, PAGES - 1);
        update();
    }

    private void update() {
        prev.active = page > 0;
        next.active = page < PAGES - 1;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT) {
            flip(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            flip(1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        renderBackground(c);
        c.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xD00A0C0A);
        c.fill(boxX, boxY, boxX + boxW, boxY + 2, 0xFFC0281E);
        c.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0x40FFFFFF);
        int x = boxX + 12, y = boxY + 10;
        c.drawTextWithShadow(textRenderer, Text.translatable("guide.outbreak." + page + ".title"), x, y, 0xFFE8D6CC);
        String count = (page + 1) + " / " + PAGES;
        c.drawTextWithShadow(textRenderer, count, boxX + boxW - 12 - textRenderer.getWidth(count), y, 0xFF6E766E);
        y += 16;
        int maxY = boxY + boxH - 10;
        for (String para : Text.translatable("guide.outbreak." + page + ".body").getString().split("\n")) {
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(para), boxW - 24);
            for (OrderedText line : lines) {
                if (y + 9 > maxY) break;
                c.drawTextWithShadow(textRenderer, line, x, y, 0xFFB8BEB8);
                y += 10;
            }
            y += 4;
        }
        super.render(c, mouseX, mouseY, delta);
    }
}
