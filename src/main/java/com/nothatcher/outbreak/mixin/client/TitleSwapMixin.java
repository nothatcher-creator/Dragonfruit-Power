package com.nothatcher.outbreak.mixin.client;

import com.nothatcher.outbreak.client.menu.OutbreakTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wherever the game would show its own title screen, show Outbreak's (unless "Classic menu" was picked). */
@Mixin(MinecraftClient.class)
public abstract class TitleSwapMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void outbreak$title(Screen screen, CallbackInfo ci) {
        if (OutbreakTitleScreen.classic) return;
        MinecraftClient mc = (MinecraftClient) (Object) this;
        // setScreen(null) with no world open is the game's way of asking for the title screen too
        boolean title = screen == null ? mc.world == null : screen.getClass() == TitleScreen.class;
        if (!title) return;
        mc.setScreen(new OutbreakTitleScreen());
        ci.cancel();
    }
}
