package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.CharacterDefs;
import com.nothatcher.outbreak.Characters;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Character creation, Project Zomboid style, in three steps: where you start, your occupation and traits (spend
 * points; bad traits earn more), then your name and look. Shown once, the first time you join a world.
 */
public class CharacterScreen extends Screen {
    private static final String[] FIRST = {"Kate", "Bob", "Marcus", "Josie", "Wade", "Lena", "Dale", "Rosa", "Hank", "Tamara", "Eli", "Nora",
            "Floyd", "Mia", "Curtis", "Ivy", "Bradley", "Dom", "Gus", "Wanda"};
    private static final String[] LAST = {"Pickett", "Gruber", "Delgado", "Whitby", "Novak", "Okafor", "Bell", "Moreau", "Hale", "Fischer",
            "Cordy", "Tan", "Kowalski", "Reyes", "Baptiste", "Lindqvist", "Morrow", "Sato"};
    private static final int PANEL = 0xD0141618, HEADER = 0xF02A1C1C, EDGE = 0xFF3A3F44;

    private int step;
    private int spawn;
    private String occupation = "unemployed";
    private final Set<String> traits = new LinkedHashSet<>();
    private String name;
    private int look;
    private Text info = Text.empty();
    private TextFieldWidget nameField;
    private final Random rnd = new Random();

    public CharacterScreen() {
        super(Text.translatable("char.outbreak.title"));
        name = FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)];
        look = rnd.nextInt(CharacterDefs.LOOKS);
        info = Text.translatable("spawn.outbreak." + CharacterDefs.SPAWNS.get(0) + ".desc");
    }

    private int points() {
        return CharacterDefs.points(occupation, traits);
    }

    @Override
    protected void init() {
        CharacterClient.previewLook = look;
        int m = 10, top = 34, bottom = height - 24;
        switch (step) {
            case 0 -> {
                int w = Math.min(160, width / 2 - 20);
                for (int i = 0; i < CharacterDefs.SPAWNS.size(); i++) {
                    int idx = i;
                    String id = CharacterDefs.SPAWNS.get(i);
                    Text label = Text.literal(i == spawn ? "» " : "").append(Text.translatable("spawn.outbreak." + id));
                    addDrawableChild(ButtonWidget.builder(label, b -> {
                        spawn = idx;
                        info = Text.translatable("spawn.outbreak." + id + ".desc");
                        clearAndInit();
                    }).dimensions(m + 6, top + 14 + i * 24, w, 20).build());
                }
                addDrawableChild(ButtonWidget.builder(Text.translatable("char.outbreak.next"), b -> {
                    step = 1;
                    info = Text.translatable("occupation.outbreak." + occupation + ".desc");
                    clearAndInit();
                }).dimensions(width - m - 90, bottom, 90, 20).build());
            }
            case 1 -> {
                int avail = height - top - 16 - 62;
                int rowH = Math.max(11, Math.min(16, avail / 8));
                int area = width - 2 * m - 12;
                int occW = area * 22 / 100, trW = area * 28 / 100;
                int x0 = m + 6, y0 = top + 16;
                List<String> occs = new ArrayList<>(CharacterDefs.OCCUPATIONS.keySet());
                for (int i = 0; i < occs.size(); i++) {
                    String id = occs.get(i);
                    int pts = CharacterDefs.OCCUPATIONS.get(id).points();
                    Text label = Text.literal(id.equals(occupation) ? "» " : "").append(Text.translatable("occupation.outbreak." + id))
                            .append(Text.literal(" " + signed(pts)).formatted(pts > 0 ? Formatting.GREEN : pts < 0 ? Formatting.RED : Formatting.GRAY));
                    addDrawableChild(ButtonWidget.builder(label, b -> {
                        occupation = id;
                        info = Text.translatable("occupation.outbreak." + id + ".desc");
                        clearAndInit();
                    }).dimensions(x0 + (i / 6) * (occW + 2), y0 + (i % 6) * rowH, occW, rowH - 1).build());
                }
                int good = 0, bad = 0;
                int tx = x0 + 2 * (occW + 2) + 6;
                for (CharacterDefs.Trait t : CharacterDefs.TRAITS.values()) {
                    boolean positive = t.cost() > 0;
                    int row = positive ? good++ : bad++;
                    boolean on = traits.contains(t.id());
                    Text label = Text.literal(on ? "✔ " : "").append(Text.translatable("trait.outbreak." + t.id())
                            .formatted(positive ? Formatting.GREEN : Formatting.RED)).append(Text.literal(" " + signed(-t.cost())).formatted(Formatting.GRAY));
                    addDrawableChild(ButtonWidget.builder(label, b -> {
                        if (!traits.remove(t.id())) {
                            if (t.opposite() != null) traits.remove(t.opposite());
                            traits.add(t.id());
                        }
                        info = Text.translatable("trait.outbreak." + t.id() + ".desc");
                        clearAndInit();
                    }).dimensions(tx + (positive ? 0 : trW + 2), y0 + row * rowH, trW, rowH - 1).build());
                }
                addDrawableChild(ButtonWidget.builder(Text.translatable("char.outbreak.back"), b -> {
                    step = 0;
                    info = Text.translatable("spawn.outbreak." + CharacterDefs.SPAWNS.get(spawn) + ".desc");
                    clearAndInit();
                }).dimensions(m, bottom, 90, 20).build());
                ButtonWidget next = ButtonWidget.builder(Text.translatable("char.outbreak.next"), b -> {
                    step = 2;
                    clearAndInit();
                }).dimensions(width - m - 90, bottom, 90, 20).build();
                next.active = points() >= 0;
                addDrawableChild(next);
            }
            default -> {
                int x0 = m + 10, y0 = top + 22;
                nameField = new TextFieldWidget(textRenderer, x0, y0 + 10, 160, 18, Text.translatable("char.outbreak.name"));
                nameField.setMaxLength(24);
                nameField.setText(name);
                nameField.setChangedListener(s -> name = s);
                addDrawableChild(nameField);
                addDrawableChild(ButtonWidget.builder(Text.translatable("char.outbreak.random_name"), b -> {
                    name = FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)];
                    clearAndInit();
                }).dimensions(x0 + 164, y0 + 10, 70, 18).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("‹"), b -> {
                    look = look <= -1 ? CharacterDefs.LOOKS - 1 : look - 1;
                    clearAndInit();
                }).dimensions(x0, y0 + 50, 20, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("›"), b -> {
                    look = look >= CharacterDefs.LOOKS - 1 ? -1 : look + 1;
                    clearAndInit();
                }).dimensions(x0 + 140, y0 + 50, 20, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.translatable("char.outbreak.random_look"), b -> {
                    look = rnd.nextInt(CharacterDefs.LOOKS);
                    clearAndInit();
                }).dimensions(x0 + 164, y0 + 50, 70, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.translatable("char.outbreak.back"), b -> {
                    step = 1;
                    clearAndInit();
                }).dimensions(m, bottom, 90, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.translatable("char.outbreak.play").formatted(Formatting.GOLD), b -> play())
                        .dimensions(width - m - 90, bottom, 90, 20).build());
            }
        }
    }

    private static String signed(int v) {
        return v > 0 ? "+" + v : String.valueOf(v);
    }

    private void play() {
        if (points() < 0) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(spawn);
        buf.writeString(occupation);
        buf.writeVarInt(traits.size());
        for (String t : traits) buf.writeString(t);
        buf.writeString(name == null ? "" : name);
        buf.writeVarInt(look);
        ClientPlayNetworking.send(Characters.CREATE, buf);
        CharacterClient.previewLook = -2;
        if (client != null) client.setScreen(null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        int m = 10, top = 34;
        ctx.fill(m, 6, width - m, top - 4, HEADER);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("char.outbreak.title." + step), width / 2, 14, 0xFFE8D6CC);
        ctx.fill(m, top, width - m, height - 30, PANEL);
        ctx.drawBorder(m, top, width - 2 * m, height - 30 - top, EDGE);
        switch (step) {
            case 0 -> {
                int x = Math.min(160, width / 2 - 20) + m + 20, w = width - x - m - 8;
                ctx.drawTextWithShadow(textRenderer, Text.translatable("char.outbreak.where").formatted(Formatting.GRAY), m + 6, top + 4, 0xFFFFFF);
                ctx.drawTextWithShadow(textRenderer, Text.translatable("spawn.outbreak." + CharacterDefs.SPAWNS.get(spawn)).formatted(Formatting.GOLD), x, top + 14, 0xFFFFFF);
                drawWrapped(ctx, info, x, top + 28, w, 0xFFD8D8D0);
            }
            case 1 -> {
                int area = width - 2 * m - 12, occW = area * 22 / 100, trW = area * 28 / 100, x0 = m + 6;
                int tx = x0 + 2 * (occW + 2) + 6;
                ctx.drawTextWithShadow(textRenderer, Text.translatable("char.outbreak.occupation").formatted(Formatting.GRAY), x0, top + 4, 0xFFFFFF);
                ctx.drawTextWithShadow(textRenderer, Text.translatable("char.outbreak.good").formatted(Formatting.GREEN), tx, top + 4, 0xFFFFFF);
                ctx.drawTextWithShadow(textRenderer, Text.translatable("char.outbreak.bad").formatted(Formatting.RED), tx + trW + 2, top + 4, 0xFFFFFF);
                int p = points();
                Text pts = Text.translatable("char.outbreak.points", p).formatted(p < 0 ? Formatting.RED : Formatting.YELLOW);
                ctx.drawTextWithShadow(textRenderer, pts, width - m - 6 - textRenderer.getWidth(pts), top + 4, 0xFFFFFF);
                drawWrapped(ctx, info, m + 6, height - 30 - 24, width - 2 * m - 12, 0xFFD8D8D0);
            }
            default -> {
                int x0 = m + 10, y0 = top + 22;
                ctx.drawTextWithShadow(textRenderer, Text.translatable("char.outbreak.name").formatted(Formatting.GRAY), x0, y0, 0xFFFFFF);
                ctx.drawTextWithShadow(textRenderer, Text.translatable("char.outbreak.look").formatted(Formatting.GRAY), x0, y0 + 40, 0xFFFFFF);
                Text lk = look < 0 ? Text.translatable("char.outbreak.own_skin") : Text.translatable("char.outbreak.look_n", look + 1, CharacterDefs.LOOKS);
                ctx.drawCenteredTextWithShadow(textRenderer, lk, x0 + 80, y0 + 56, 0xFFFFFF);
                // the sheet: occupation and traits
                int sy = y0 + 82;
                ctx.drawTextWithShadow(textRenderer, Text.translatable("occupation.outbreak." + occupation).formatted(Formatting.GOLD), x0, sy, 0xFFFFFF);
                int line = 0;
                for (String t : traits) {
                    boolean good = CharacterDefs.TRAITS.get(t).cost() > 0;
                    ctx.drawTextWithShadow(textRenderer, Text.translatable("trait.outbreak." + t).formatted(good ? Formatting.GREEN : Formatting.RED),
                            x0 + (line % 2) * 110, sy + 12 + (line / 2) * 10, 0xFFFFFF);
                    line++;
                }
                if (client != null && client.player != null) {
                    int px = Math.max(x0 + 260, width - m - 70), size = Math.max(24, Math.min(60, (height - 90) / 3));
                    InventoryScreen.drawEntity(ctx, px, height - 40, size, px - mouseX, height - 40 - size * 2 - mouseY, client.player);
                }
            }
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawWrapped(DrawContext ctx, Text t, int x, int y, int w, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(t, Math.max(40, w));
        for (int i = 0; i < lines.size(); i++) ctx.drawTextWithShadow(textRenderer, lines.get(i), x, y + i * 10, color);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void removed() {
        CharacterClient.previewLook = -2;
        super.removed();
    }
}
