package com.nothatcher.outbreak;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A 27-slot bag you carry in your inventory. Use it to open. */
public class BackpackItem extends Item {
    public BackpackItem(Settings s) {
        super(s.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity p, Hand hand) {
        ItemStack st = p.getStackInHand(hand);
        if (!w.isClient) {
            open(p, st, hand == Hand.MAIN_HAND ? p.getInventory().selectedSlot : 40);
        }
        return TypedActionResult.success(st, w.isClient);
    }

    /** open the bag sitting in inventory slot {@code slot} (0-35, or 40 for the offhand) */
    public static void open(PlayerEntity p, ItemStack st, int slot) {
        p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8f, 0.9f);
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, pl) -> new Handler(syncId, inv, st, slot), st.getName()));
    }

    @Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public void appendTooltip(ItemStack st, @Nullable World w, List<Text> tip, TooltipContext ctx) {
        NbtCompound n = st.getNbt();
        int used = n == null ? 0 : n.getList("Backpack", NbtElement.COMPOUND_TYPE).size();
        tip.add(Text.translatable("tooltip.outbreak.backpack", used).formatted(Formatting.GRAY));
    }

    static SimpleInventory load(ItemStack st) {
        SimpleInventory inv = new SimpleInventory(27);
        NbtCompound n = st.getNbt();
        if (n != null) {
            NbtList list = n.getList("Backpack", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                int slot = c.getByte("Slot") & 255;
                if (slot < inv.size()) inv.setStack(slot, ItemStack.fromNbt(c));
            }
        }
        return inv;
    }

    static void save(SimpleInventory inv, ItemStack st) {
        NbtList list = new NbtList();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            NbtCompound c = new NbtCompound();
            c.putByte("Slot", (byte) i);
            s.writeNbt(c);
            list.add(c);
        }
        st.getOrCreateNbt().put("Backpack", list);
    }

    static class Handler extends GenericContainerScreenHandler {
        private final ItemStack bag;
        private final int invSlot;
        private final SimpleInventory inv;
        private final int heldIndex;

        Handler(int syncId, PlayerInventory pinv, ItemStack bag, int invSlot) {
            this(syncId, pinv, bag, invSlot, load(bag));
        }

        private Handler(int syncId, PlayerInventory pinv, ItemStack bag, int invSlot, SimpleInventory inv) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, pinv, inv, 3);
            this.bag = bag;
            this.invSlot = invSlot;
            this.inv = inv;
            // container 0-26, player main (inv 9-35) at 27-53, hotbar (inv 0-8) at 54-62; the offhand isn't shown
            this.heldIndex = invSlot < 9 ? 54 + invSlot : invSlot < 36 ? 27 + (invSlot - 9) : -1;
            inv.addListener(i -> save(inv, bag));
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (heldIndex >= 0 && slot == heldIndex) return; // don't let the open bag be moved
            if (action == SlotActionType.SWAP && button == invSlot) return;
            super.onSlotClick(slot, button, action, player);
            // no bags inside bags
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.getItem() instanceof BackpackItem) {
                    inv.setStack(i, ItemStack.EMPTY);
                    player.getInventory().offerOrDrop(s);
                }
            }
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player.getInventory().getStack(invSlot) == bag;
        }

        @Override
        public void onClosed(PlayerEntity player) {
            super.onClosed(player);
            save(inv, bag);
        }
    }
}
