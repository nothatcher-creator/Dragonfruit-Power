package com.nothatcher.vehicleforge.client;

import com.nothatcher.vehicleforge.Net;
import com.nothatcher.vehicleforge.VehicleForge;
import com.nothatcher.vehicleforge.block.NeonSignBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Type what the neon sign says ('|' starts a new line), then tap anywhere on the colour wheel (or a preset swatch)
 * for its colour, and set the size and flicker. Laid out for touch screens.
 */
public class NeonSignScreen extends Screen {
    private static final Identifier WHEEL = VehicleForge.id("neon_wheel");
    private static final int R = 46;
    private static boolean wheelMade;

    private final BlockPos pos;
    private TextFieldWidget field;
    private int color, size;
    private boolean flicker, dragging;
    private float hue, sat;
    private String text;
    private ButtonWidget sizeBtn;

    public NeonSignScreen(BlockPos pos, NeonSignBlockEntity be) {
        super(Text.translatable("screen.vehicleforge.neon_sign"));
        this.pos = pos;
        this.text = be.text;
        this.size = be.size;
        this.flicker = be.flicker;
        setColor(be.color);
    }

    private void setColor(int rgb) {
        color = rgb & 0xFFFFFF;
        float r = (color >> 16 & 255) / 255f, g = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        sat = max <= 0 ? 0 : d / max;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        hue = (h / 6f + 1f) % 1f;
    }

    private static void makeWheel() {
        if (wheelMade) return;
        int n = R * 2 + 1;
        NativeImage img = new NativeImage(n, n, true);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                double dx = x - R, dy = y - R, d = Math.sqrt(dx * dx + dy * dy);
                if (d > R + 0.5) {
                    img.setColor(x, y, 0);
                    continue;
                }
                float h = (float) ((Math.atan2(dy, dx) / (Math.PI * 2) + 1) % 1), s = (float) Math.min(1, d / R);
                int rgb = MathHelper.hsvToRgb(h, s, 1f);
                int a = d > R - 0.5 ? 140 : 255;
                img.setColor(x, y, (a << 24) | ((rgb & 255) << 16) | (rgb & 0xFF00) | (rgb >> 16 & 255));   // ABGR
            }
        }
        MinecraftClient.getInstance().getTextureManager().registerTexture(WHEEL, new NativeImageBackedTexture(img));
        wheelMade = true;
    }

    private int wx() { return width / 2 + 80; }

    private int wy() { return height / 2 - 12; }

    @Override
    protected void init() {
        makeWheel();
        int cx = width / 2, cy = height / 2;
        int lx = cx - 155;
        field = new TextFieldWidget(textRenderer, lx, cy - 78, 310, 20, Text.translatable("screen.vehicleforge.neon_sign"));
        field.setMaxLength(NeonSignBlockEntity.MAX_LEN);
        field.setText(text);
        field.setChangedListener(s -> text = s);
        addDrawableChild(field);
        setInitialFocus(field);
        addDrawableChild(ButtonWidget.builder(flickLabel(), b -> { flicker = !flicker; b.setMessage(flickLabel()); })
                .dimensions(lx, cy - 48, 150, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> { size = Math.max(1, size - 1); sizeBtn.setMessage(sizeLabel()); })
                .dimensions(lx, cy - 20, 34, 22).build());
        sizeBtn = addDrawableChild(ButtonWidget.builder(sizeLabel(), b -> { }).dimensions(lx + 38, cy - 20, 74, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> { size = Math.min(NeonSignBlockEntity.MAX_SIZE, size + 1); sizeBtn.setMessage(sizeLabel()); })
                .dimensions(lx + 116, cy - 20, 34, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close()).dimensions(lx, cy + 60, 150, 22).build());
    }

    private Text sizeLabel() {
        return Text.translatable("screen.vehicleforge.neon_size", size);
    }

    private Text flickLabel() {
        return Text.translatable(flicker ? "screen.vehicleforge.neon_flicker_on" : "screen.vehicleforge.neon_flicker_off");
    }

    /** picks a colour if (mx, my) is on the wheel or a swatch */
    private boolean pick(double mx, double my) {
        double dx = mx - wx(), dy = my - wy(), d = Math.sqrt(dx * dx + dy * dy);
        if (d <= R + 2) {
            hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2) + 1) % 1);
            sat = (float) Math.min(1, d / R);
            color = MathHelper.hsvToRgb(hue, sat, 1f);
            return true;
        }
        int n = NeonSignBlockEntity.COLORS.length, sx0 = wx() - n * 13 / 2, sy = wy() + R + 8;
        if (my >= sy && my < sy + 12 && mx >= sx0 && mx < sx0 + n * 13) {
            int i = (int) ((mx - sx0) / 13);
            setColor(NeonSignBlockEntity.COLORS[Math.min(n - 1, i)]);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (pick(mx, my)) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (dragging) {
            pick(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        int cx = width / 2, cy = height / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, cy - 108, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vehicleforge.neon_hint"), cx, cy - 94, 0xA0A0A0);
        super.render(ctx, mx, my, delta);
        // the colour wheel, with a ring on the chosen colour
        int x0 = wx() - R, y0 = wy() - R;
        ctx.drawTexture(WHEEL, x0, y0, 0, 0, R * 2 + 1, R * 2 + 1, R * 2 + 1, R * 2 + 1);
        int px = wx() + Math.round((float) Math.cos(hue * Math.PI * 2) * sat * R), py = wy() + Math.round((float) Math.sin(hue * Math.PI * 2) * sat * R);
        ctx.fill(px - 3, py - 3, px + 4, py + 4, 0xFF000000);
        ctx.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFFFFF);
        ctx.fill(px - 1, py - 1, px + 2, py + 2, 0xFF000000 | color);
        // preset swatches
        int n = NeonSignBlockEntity.COLORS.length, sx0 = wx() - n * 13 / 2, sy = wy() + R + 8;
        for (int i = 0; i < n; i++) {
            int c = NeonSignBlockEntity.COLORS[i];
            ctx.fill(sx0 + i * 13, sy, sx0 + i * 13 + 11, sy + 11, c == color ? 0xFFFFFFFF : 0xFF202020);
            ctx.fill(sx0 + i * 13 + 1, sy + 1, sx0 + i * 13 + 10, sy + 10, 0xFF000000 | c);
        }
        // live preview: the first line glowing in the chosen colour
        String first = text.split("\\|", -1)[0];
        int lx = cx - 155;
        ctx.fill(lx, cy + 10, lx + 150, cy + 52, 0xC0101014);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(lx + 75, cy + 31, 0);
        float sc = Math.min(2f, 140f / Math.max(1, textRenderer.getWidth(first)));
        ctx.getMatrices().scale(sc, sc, 1);
        int glow = 0xFF000000 | color;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(first), 0, -4, glow);
        ctx.getMatrices().pop();
    }

    @Override
    public void close() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(text, NeonSignBlockEntity.MAX_LEN * 4);
        buf.writeInt(color);
        buf.writeVarInt(size);
        buf.writeBoolean(flicker);
        ClientPlayNetworking.send(Net.NEON_EDIT, buf);
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
