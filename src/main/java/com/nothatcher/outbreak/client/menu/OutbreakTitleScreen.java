package com.nothatcher.outbreak.client.menu;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Outbreak's title screen. The main menu (Play, Settings, Survival Guide, Extras, Quit) opens sub menus that slide
 * in over the same scene; Esc goes back a level. Everything leads to the ordinary Minecraft screens, which get the
 * same backdrop (see ScreenBackgroundMixin).
 */
public class OutbreakTitleScreen extends Screen {
    /** set by "Classic menu": lets the vanilla title screen through for the rest of the session */
    public static boolean classic = false;
    private static boolean firstOpen = true;
    private static final int TIPS = 15;

    private enum Panel { MAIN, PLAY, SETTINGS, EXTRAS }

    private record Entry(String key, Runnable action, boolean primary, boolean enabled) {
        Entry(String key, Runnable action, boolean primary) {
            this(key, action, primary, true);
        }
    }

    private Panel panel = Panel.MAIN;
    private long panelSince = Util.getMeasuringTimeMs();
    private final long openedAt = Util.getMeasuringTimeMs();
    private final boolean fadeIn;
    private final int tip;
    private final List<MenuButton> rows = new ArrayList<>();
    private int rowX, headY, logoX, logoY, logoScale;
    private int lastBolt = -1;

    public OutbreakTitleScreen() {
        super(Text.translatable("menu.outbreak.title"));
        this.fadeIn = firstOpen;
        firstOpen = false;
        this.tip = (int) (Math.random() * TIPS);
    }

    private List<Entry> entries() {
        MinecraftClient mc = client;
        return switch (panel) {
            case MAIN -> List.of(
                    new Entry("menu.outbreak.play", () -> show(Panel.PLAY), true),
                    new Entry("menu.outbreak.settings", () -> show(Panel.SETTINGS), true),
                    new Entry("menu.outbreak.guide", () -> mc.setScreen(new GuideScreen(this)), true),
                    new Entry("menu.outbreak.extras", () -> show(Panel.EXTRAS), true),
                    new Entry("menu.outbreak.quit", mc::scheduleStop, false));
            case PLAY -> List.of(
                    new Entry("menu.outbreak.singleplayer", () -> mc.setScreen(new SelectWorldScreen(this)), true),
                    new Entry("menu.outbreak.multiplayer", () -> mc.setScreen(mc.options.skipMultiplayerWarning
                            ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this)), true, mc.isMultiplayerEnabled()),
                    new Entry("menu.outbreak.back", () -> show(Panel.MAIN), false));
            case SETTINGS -> List.of(
                    new Entry("menu.outbreak.all_options", () -> mc.setScreen(new OptionsScreen(this, mc.options)), true),
                    new Entry("menu.outbreak.video", () -> mc.setScreen(new VideoOptionsScreen(this, mc.options)), true),
                    new Entry("menu.outbreak.controls", () -> mc.setScreen(new ControlsOptionsScreen(this, mc.options)), true),
                    new Entry("menu.outbreak.sound", () -> mc.setScreen(new SoundOptionsScreen(this, mc.options)), true),
                    new Entry("menu.outbreak.language", () -> mc.setScreen(new LanguageOptionsScreen(this, mc.options, mc.getLanguageManager())), true),
                    new Entry("menu.outbreak.accessibility", () -> mc.setScreen(new AccessibilityOptionsScreen(this, mc.options)), true),
                    new Entry("menu.outbreak.back", () -> show(Panel.MAIN), false));
            case EXTRAS -> List.of(
                    new Entry("menu.outbreak.credits", () -> mc.setScreen(new CreditsScreen(false, () -> mc.setScreen(this))), true),
                    new Entry("menu.outbreak.classic", () -> {
                        classic = true;
                        mc.setScreen(new TitleScreen(false));
                    }, true),
                    new Entry("menu.outbreak.back", () -> show(Panel.MAIN), false));
        };
    }

    private void show(Panel p) {
        panel = p;
        panelSince = Util.getMeasuringTimeMs();
        clearAndInit();
    }

    @Override
    protected void init() {
        rows.clear();
        TextRenderer tr = textRenderer;
        // logo: as big as fits in the left part of the screen, in whole pixel steps so it stays crisp
        int tw = tr.getWidth("OUTBREAK");
        logoScale = MathHelper.clamp((int) (width * 0.42f / tw), 2, 6);
        if (height < 200) logoScale = Math.min(logoScale, 3);
        logoX = Math.max(14, width / 16);
        logoY = Math.max(10, height / 12);
        int logoBottom = logoY + 9 * logoScale + 14;

        List<Entry> list = entries();
        rowX = logoX;
        int rowW = MathHelper.clamp(width / 3, 130, 190);
        headY = logoBottom + 4;
        int top = headY + (panel == Panel.MAIN ? 4 : 16);
        int gap = 4;
        int rowH = MathHelper.clamp((height - top - 22) / list.size() - gap, 14, 22);
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            MenuButton b = new MenuButton(rowX, top + i * (rowH + gap), rowW, rowH, Text.translatable(e.key()), btn -> e.action().run(), e.primary());
            b.active = e.enabled();
            rows.add(addDrawableChild(b));
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && panel != Panel.MAIN) {
            show(Panel.MAIN);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        float t = MenuBackdrop.time();
        MenuBackdrop.render(c, width, height, false);
        int bolt = MenuBackdrop.bolt(t);
        if (bolt >= 0 && bolt != lastBolt) {
            lastBolt = bolt;
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.55f + (bolt % 3) * 0.1f, 0.22f));
        }

        // shade behind the menu column so it reads over the city
        int shadeW = rowX + (rows.isEmpty() ? 160 : rows.get(0).getWidth()) + 40;
        for (int i = 0; i < 12; i++) {
            int a = (int) (0xA8 * (1f - i / 12f));
            c.fill(shadeW * i / 12, 0, shadeW * (i + 1) / 12, height, a << 24);
        }

        logo(c, t);
        if (panel != Panel.MAIN) {
            c.drawTextWithShadow(textRenderer, Text.translatable("menu.outbreak.panel." + panel.name().toLowerCase()), rowX, headY, 0xFFC0281E);
        }

        // the rows slide in when a menu opens
        float k = Math.min(1f, (Util.getMeasuringTimeMs() - panelSince) / 240f);
        for (int i = 0; i < rows.size(); i++) {
            float ki = MathHelper.clamp(k * 1.6f - i * 0.12f, 0f, 1f);
            float ease = 1f - (1f - ki) * (1f - ki) * (1f - ki);
            rows.get(i).setX(rowX - (int) ((1f - ease) * 50));
        }
        super.render(c, mouseX, mouseY, delta);

        String version = "Minecraft " + SharedConstants.getGameVersion().getName() + "  ·  Outbreak";
        c.drawTextWithShadow(textRenderer, version, 4, height - 12, 0xFF6E766E);
        String copy = "Copyright Mojang AB. Do not distribute!";
        c.drawTextWithShadow(textRenderer, copy, width - textRenderer.getWidth(copy) - 4, height - 12, 0xFF6E766E);

        if (fadeIn) {                                                         // first time in: up out of the dark
            float f = 1f - Math.min(1f, (Util.getMeasuringTimeMs() - openedAt) / 1400f);
            if (f > 0) c.fill(0, 0, width, height, (int) (f * 255) << 24);
        }
    }

    private void logo(DrawContext c, float t) {
        TextRenderer tr = textRenderer;
        String word = "OUTBREAK";
        int s = logoScale;
        // a failing neon: every so often it stutters
        float glitch = MenuBackdrop.frac(t * 0.13f);
        boolean dim = (glitch > 0.62f && glitch < 0.64f) || (glitch > 0.655f && glitch < 0.665f);
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(logoX, logoY, 0);
        m.scale(s, s, 1);
        c.drawText(tr, word, 1, 1, 0xFF240404, false);
        c.drawText(tr, word, 0, 0, dim ? 0xFF4A0E0B : 0xFFC4231B, false);
        m.pop();
        // blood running off a few letters
        int[] letters = {1, 4, 6};
        for (int i = 0; i < letters.length; i++) {
            int lx = logoX + (tr.getWidth(word.substring(0, letters[i])) + 2) * s;
            float phase = MenuBackdrop.frac(t * (0.12f + i * 0.03f) + i * 0.37f);
            int len = (int) (phase * 9 * s);
            int y0 = logoY + 7 * s;
            c.fill(lx, y0, lx + Math.max(1, s / 2), y0 + len, 0xFF9A1812);
            if (phase > 0.85f) c.fill(lx - 1, y0 + len, lx + Math.max(1, s / 2) + 1, y0 + len + 2, 0xFF9A1812);
        }
        c.drawTextWithShadow(tr, Text.translatable("menu.outbreak.tagline"), logoX + 2, logoY + 9 * s + 4, 0xFF8E9C8E);

        // a survival tip where Minecraft puts its splash text
        m.push();
        m.translate(logoX + tr.getWidth(word) * s - 4, logoY + 8 * s, 0);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-14f));
        float pulse = 0.9f + Math.abs(MathHelper.sin(t * 5f)) * 0.08f;
        m.scale(pulse, pulse, 1);
        Text splash = Text.translatable("menu.outbreak.tip." + tip);
        int sw = tr.getWidth(splash);
        float fit = Math.min(1f, (width - (logoX + tr.getWidth(word) * s)) / (float) Math.max(1, sw / 2 + 6));
        m.scale(fit, fit, 1);
        c.drawCenteredTextWithShadow(tr, splash, 0, -8, 0xFFE2C64A);
        m.pop();
    }

    @Override
    public void renderBackground(DrawContext c) {
        // the scene is drawn in render()
    }
}
