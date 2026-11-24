package com.nothatcher.outbreak;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/**
 * Skill books, Zomboid style: three volumes for each skill. Hold right-click to read one (it takes a while, and
 * you can't read in the dark). If it's at your level it teaches you a level's worth; each book only once.
 * Volume 1 is for levels 0-3, volume 2 for 4-6, volume 3 for 7-9.
 */
public class SkillBookItem extends Item {
    public final Skills.Skill skill;
    public final int volume;

    public SkillBookItem(Skills.Skill skill, int volume, Settings s) {
        super(s);
        this.skill = skill;
        this.volume = volume;
    }

    public int minLevel() {
        return volume == 1 ? 0 : volume == 2 ? 4 : 7;
    }

    public int maxLevel() {
        return volume == 1 ? 3 : volume == 2 ? 6 : 9;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 140;
    }

    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity p, Hand hand) {
        if (w.getLightLevel(p.getBlockPos()) < 4 && !p.isCreative()) {
            if (!w.isClient) p.sendMessage(Text.translatable("msg.outbreak.too_dark"), true);
            return TypedActionResult.fail(p.getStackInHand(hand));
        }
        p.setCurrentHand(hand);
        if (!w.isClient) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.8f, 1f);
        return TypedActionResult.consume(p.getStackInHand(hand));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World w, LivingEntity user) {
        if (user instanceof ServerPlayerEntity p) Skills.read(p, this);
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.translatable("item.outbreak.skill_book.tip", Text.translatable("skill.outbreak." + skill.id()), minLevel(), maxLevel() + 1).formatted(Formatting.GRAY));
    }
}
