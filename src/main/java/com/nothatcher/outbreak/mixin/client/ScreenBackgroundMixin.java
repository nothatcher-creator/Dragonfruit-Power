package com.nothatcher.outbreak.mixin.client;

import com.nothatcher.outbreak.client.menu.MenuBackdrop;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The menus outside a world (world list, options, loading) get the Outbreak scene behind a dark veil, not dirt. */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "renderBackgroundTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private void outbreak$backdrop(DrawContext c, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        MenuBackdrop.render(c, self.width, self.height, true);
        c.fill(0, 0, self.width, self.height, 0xB0000000);
        ci.cancel();
    }
}
