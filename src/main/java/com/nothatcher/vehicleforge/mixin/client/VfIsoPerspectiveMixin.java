package com.nothatcher.vehicleforge.mixin.client;

import com.nothatcher.vehicleforge.client.IsoCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** F5 gets a fourth step: first person, behind, in front, isometric. */
@Mixin(MinecraftClient.class)
public abstract class VfIsoPerspectiveMixin {
    @Redirect(method = "handleInputEvents", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;setPerspective(Lnet/minecraft/client/option/Perspective;)V"))
    private void vf$isoStep(GameOptions options, Perspective next) {
        IsoCam.cycle(options, next);
    }
}
