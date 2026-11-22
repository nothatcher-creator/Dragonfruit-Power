package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.Skills;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** your character sheet: name, occupation, traits, the ten-level skill bars and your safehouse */
public class SkillsScreen extends Screen {
    public SkillsScreen() {
        super(Text.translatable("skills.outbreak.title"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close()).dimensions(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        renderBackground(c);
        int w = Math.min(320, width - 20), x0 = (width - w) / 2, y0 = 10, y1 = height - 32;
        c.fill(x0, y0, x0 + w, y1, 0xD0141618);
        c.drawBorder(x0, y0, w, y1 - y0, 0xFF3A3F44);
        CharacterClient.Info me = CharacterClient.mine();
        int y = y0 + 8;
        Text head = me == null ? Text.translatable("skills.outbreak.title")
                : Text.literal(me.name()).formatted(Formatting.WHITE).append(Text.literal("  ").append(Text.translatable("occupation.outbreak." + me.occupation())).formatted(Formatting.GOLD));
        c.drawTextWithShadow(textRenderer, head, x0 + 8, y, 0xFFFFFF);
        y += 12;
        if (me != null && !me.traits().isEmpty()) {
            int tx = x0 + 8;
            for (String t : me.traits()) {
                Text tt = Text.translatable("trait.outbreak." + t).formatted(com.nothatcher.outbreak.CharacterDefs.TRAITS.get(t).cost() > 0 ? Formatting.GREEN : Formatting.RED);
                int tw = textRenderer.getWidth(tt) + 8;
                if (tx + tw > x0 + w - 8) {
                    tx = x0 + 8;
                    y += 10;
                }
                c.drawTextWithShadow(textRenderer, tt, tx, y, 0xFFFFFF);
                tx += tw;
            }
            y += 12;
        }
        y += 4;
        int rowH = Math.max(14, Math.min(20, (y1 - y - 30) / Skills.Skill.values().length));
        int labelW = 78, barX = x0 + 8 + labelW, barW = w - labelW - 24;
        for (Skills.Skill s : Skills.Skill.values()) {
            int xp = SkillsClient.xp[s.ordinal()], lvl = Skills.levelOf(xp);
            c.drawTextWithShadow(textRenderer, Text.translatable("skill.outbreak." + s.id()), x0 + 8, y + 2, 0xFFD8D8D0);
            int seg = barW / Skills.MAX;
            for (int i = 0; i < Skills.MAX; i++) {
                int sx = barX + i * seg;
                c.fill(sx, y + 1, sx + seg - 2, y + 9, i < lvl ? 0xFFD8B04A : 0xFF2C3034);
                if (i == lvl && lvl < Skills.MAX) {
                    float f = (xp - Skills.xpFor(lvl)) / (float) (Skills.xpFor(lvl + 1) - Skills.xpFor(lvl));
                    c.fill(sx, y + 1, sx + (int) ((seg - 2) * f), y + 9, 0xFF7A6A3A);
                }
            }
            y += rowH;
        }
        y += 4;
        boolean inside = client != null && client.player != null && SkillsClient.inSafehouse(client.player.getBlockPos());
        Text safe = SkillsClient.safehouse == null ? Text.translatable("skills.outbreak.no_safehouse").formatted(Formatting.GRAY)
                : Text.translatable(inside ? "skills.outbreak.in_safehouse" : "skills.outbreak.safehouse_at",
                SkillsClient.safehouse.getCenter().getX(), SkillsClient.safehouse.getCenter().getZ()).formatted(Formatting.GREEN);
        c.drawTextWithShadow(textRenderer, safe, x0 + 8, Math.min(y, y1 - 12), 0xFFFFFF);
        super.render(c, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
