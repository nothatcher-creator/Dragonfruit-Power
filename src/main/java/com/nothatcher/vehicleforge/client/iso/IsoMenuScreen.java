package com.nothatcher.vehicleforge.client.iso;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The options for something out of reach in the isometric view, opened where you clicked: walk there, break it,
 * use it, place your block there, or for a creature or vehicle walk to it, attack it or use it.
 */
public class IsoMenuScreen extends Screen {
    private record Option(Text label, Runnable run) {}

    private final BlockHitResult block;
    private final Entity entity;
    private final double u, v;
    private final List<Option> options = new ArrayList<>();
    private int boxX, boxY, boxW, rowH = 18;
    private Text title;

    public IsoMenuScreen(BlockHitResult block, Entity entity, double u, double v) {
        super(Text.translatable("menu.vehicleforge.iso.title"));
        this.block = block;
        this.entity = entity;
        this.u = u;
        this.v = v;
    }

    private void build() {
        options.clear();
        var mc = client;
        if (entity != null) {
            title = entity.getDisplayName();
            if (entity instanceof com.nothatcher.vehicleforge.VehicleEntity ve) {
                options.add(new Option(Text.translatable("menu.vehicleforge.iso.trunk", entity.getDisplayName()), () -> IsoActions.walkAndOpenCargo(mc, ve)));
            }
            if (IsoActions.usable(entity)) {
                options.add(new Option(Text.translatable("menu.vehicleforge.iso.use", entity.getDisplayName()), () -> IsoActions.walkAndUseEntity(mc, entity)));
            }
            if (entity.isAttackable() && !(entity instanceof com.nothatcher.vehicleforge.VehicleEntity)) {
                options.add(new Option(Text.translatable("menu.vehicleforge.iso.attack", entity.getDisplayName()), () -> IsoActions.walkAndAttack(mc, entity)));
            }
            options.add(new Option(Text.translatable("menu.vehicleforge.iso.walk_to", entity.getDisplayName()), () -> IsoActions.walkToEntity(mc, entity)));
        } else if (block != null) {
            BlockPos pos = block.getBlockPos();
            BlockState s = mc.world.getBlockState(pos);
            title = s.getBlock().getName();
            BlockPos spot = IsoWalker.spotNear(mc.world, block);
            if (spot != null) options.add(new Option(Text.translatable("menu.vehicleforge.iso.walk"), () -> IsoActions.walkTo(mc, spot)));
            if (IsoActions.interactable(s)) {
                options.add(new Option(Text.translatable("menu.vehicleforge.iso.use", s.getBlock().getName()), () -> IsoActions.walkAndUse(mc, pos)));
            }
            if (s.getHardness(mc.world, pos) >= 0 && !s.isAir()) {
                options.add(new Option(Text.translatable("menu.vehicleforge.iso.break", s.getBlock().getName()), () -> IsoActions.walkAndBreak(mc, block)));
            }
            for (Hand hand : Hand.values()) {
                ItemStack held = mc.player.getStackInHand(hand);
                if (held.getItem() instanceof BlockItem) {
                    options.add(new Option(Text.translatable("menu.vehicleforge.iso.place", held.getName()), () -> IsoActions.place(mc, block, hand)));
                    break;
                }
            }
        }
        options.add(new Option(Text.translatable("menu.vehicleforge.iso.cancel"), () -> { }));
    }

    @Override
    protected void init() {
        build();
        boxW = 120;
        for (Option o : options) boxW = Math.max(boxW, textRenderer.getWidth(o.label) + 20);
        if (title != null) boxW = Math.max(boxW, textRenderer.getWidth(title) + 16);
        int boxH = 14 + options.size() * (rowH + 2);
        boxX = MathHelper.clamp((int) (u * width) + 6, 4, Math.max(4, width - boxW - 4));
        boxY = MathHelper.clamp((int) (v * height) + 6, 4, Math.max(4, height - boxH - 4));
        for (int i = 0; i < options.size(); i++) {
            Option o = options.get(i);
            addDrawableChild(new Row(boxX + 3, boxY + 13 + i * (rowH + 2), boxW - 6, rowH, o.label, b -> {
                close();
                o.run.run();
            }));
        }
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        int boxH = 14 + options.size() * (rowH + 2) + 2;
        c.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE0101010);
        c.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFFB08A3A);
        if (title != null) c.drawTextWithShadow(textRenderer, title, boxX + 5, boxY + 3, 0xFFB0B0B0);
        super.render(c, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        close();                                            // a click anywhere else dismisses it
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** a flat menu row */
    private static class Row extends ButtonWidget {
        Row(int x, int y, int w, int h, Text label, PressAction press) {
            super(x, y, w, h, label, press, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        public void renderButton(DrawContext c, int mouseX, int mouseY, float delta) {
            boolean hot = isSelected();
            c.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), hot ? 0xC0403018 : 0x80202020);
            if (hot) c.fill(getX(), getY(), getX() + 2, getY() + getHeight(), 0xFFE0A840);
            c.drawTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer, getMessage(), getX() + 7, getY() + (getHeight() - 8) / 2,
                    hot ? 0xFFFFFFFF : 0xFFD0D0D0);
        }
    }
}
