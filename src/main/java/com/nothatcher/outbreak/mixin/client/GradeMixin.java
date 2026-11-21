package com.nothatcher.outbreak.mixin.client;

import com.nothatcher.outbreak.client.ZomboidGrade;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the Zomboid Look colour grade right after the world is drawn, before the HUD goes on top. */
@Mixin(GameRenderer.class)
public abstract class GradeMixin {
    @Inject(method = "render", require = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V"))
    private void outbreak$grade(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        ZomboidGrade.render(tickDelta);
    }
}
