package com.nothatcher.dragonfruit.content;

import com.nothatcher.dragonfruit.power.SwordAbilities;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Right-click: flying slash. Sneak + right-click: spinning tornado slash.
 * Carry all three katanas and every move becomes the three-sword version.
 */
public class KatanaItem extends SwordItem {
    public final int slashColor;

    public KatanaItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings, int slashColor) {
        super(material, attackDamage, attackSpeed, settings);
        this.slashColor = slashColor;
    }

    public static boolean hasAllThree(PlayerEntity player) {
        for (KatanaItem k : ModContent.KATANAS) {
            boolean found = false;
            for (int i = 0; i < player.getInventory().size() && !found; i++) {
                found = player.getInventory().getStack(i).isOf(k);
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            boolean three = hasAllThree(sp);
            if (sp.isSneaking()) SwordAbilities.tornado(sp, three, slashColor);
            else SwordAbilities.slash(sp, three, slashColor);
            int cooldown = sp.isSneaking() ? 60 : (three ? 24 : 18);
            for (KatanaItem k : ModContent.KATANAS) sp.getItemCooldownManager().set(k, cooldown);
        }
        user.swingHand(hand);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.dragonfruit.katana").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.dragonfruit.katana_three").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
