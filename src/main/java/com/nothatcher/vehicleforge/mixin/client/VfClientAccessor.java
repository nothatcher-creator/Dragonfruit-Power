package com.nothatcher.vehicleforge.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface VfClientAccessor {
    @Accessor("itemUseCooldown")
    void vehicleforge$setItemUseCooldown(int ticks);
}
