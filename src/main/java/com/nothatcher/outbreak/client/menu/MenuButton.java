package com.nothatcher.outbreak.client.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A flat menu row: dark glass, a red bar and a nudge to the right when it's the one you're on. Big enough to tap. */
public class MenuButton extends ButtonWidget {
    private final boolean primary;

    public MenuButton(int x, int y, int w, int h, Text message, PressAction onPress, boolean primary) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.primary = primary;
    }

    @Override
    public void renderButton(DrawContext c, int mouseX, int mouseY, float delta) {
        boolean hot = active && isSelected();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        c.fill(x, y, x + w, y + h, hot ? 0xD02A1414 : 0x96080808);
        c.fill(x, y + h - 1, x + w, y + h, hot ? 0x60C0281E : 0x28FFFFFF);
        if (hot) c.fill(x, y, x + 3, y + h, 0xFFC0281E);
        int color = !active ? 0xFF5A5A5A : hot ? 0xFFFFFFFF : primary ? 0xFFE8D6CC : 0xFFB4B4B4;
        c.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), x + (hot ? 12 : 9), y + (h - 8) / 2, color);
    }
}
