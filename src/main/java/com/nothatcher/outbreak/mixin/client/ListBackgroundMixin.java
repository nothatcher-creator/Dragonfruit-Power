package com.nothatcher.outbreak.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scrolling lists (worlds, servers, controls) draw their own dirt panel; outside a world, let the scene show through. */
@Mixin(EntryListWidget.class)
public abstract class ListBackgroundMixin {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void outbreak$seeThrough(DrawContext c, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null) return;
        EntryListWidget<?> self = (EntryListWidget<?>) (Object) this;
        self.setRenderBackground(false);
        self.setRenderHorizontalShadows(false);
    }
}
