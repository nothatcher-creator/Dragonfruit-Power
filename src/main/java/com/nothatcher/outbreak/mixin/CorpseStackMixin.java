package com.nothatcher.outbreak.mixin;

import com.nothatcher.outbreak.Corpses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While a zombie is dropping its things, they go into its corpse-to-be rather than onto the ground. */
@Mixin(Entity.class)
public abstract class CorpseStackMixin {
    @Inject(method = "dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void outbreak$intoCorpse(ItemStack stack, float yOffset, CallbackInfoReturnable<ItemEntity> cir) {
        if (Corpses.capturing != (Object) this || stack.isEmpty()) return;
        var list = ((Corpses.Holder) Corpses.capturing).outbreak$loot();
        if (list == null) return;
        list.add(stack.copy());
        cir.setReturnValue(null);
    }
}
