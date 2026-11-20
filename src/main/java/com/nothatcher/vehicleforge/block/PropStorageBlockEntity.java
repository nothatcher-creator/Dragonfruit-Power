package com.nothatcher.vehicleforge.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/** Drawers, fridges, cupboards, shelves and mailboxes: 9, 18 or 27 slots, filled from a loot table the first time they're opened. */
public class PropStorageBlockEntity extends LootableContainerBlockEntity {
    private DefaultedList<ItemStack> items;
    private final int rows;

    public PropStorageBlockEntity(BlockPos pos, BlockState state) {
        super(CityExtras.STORAGE_BE, pos, state);
        this.rows = state.getBlock() instanceof StorageProp sp ? sp.rows : 1;
        this.items = DefaultedList.ofSize(rows * 9, ItemStack.EMPTY);
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return items;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
        this.items = list;
    }

    @Override
    protected Text getContainerName() {
        return getCachedState().getBlock().getName();
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory inv) {
        ScreenHandlerType<GenericContainerScreenHandler> t = switch (rows) {
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            default -> ScreenHandlerType.GENERIC_9X1;
        };
        return new GenericContainerScreenHandler(t, syncId, inv, this, rows);
    }

    @Override
    public int size() {
        return rows * 9;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        items = DefaultedList.ofSize(size(), ItemStack.EMPTY);
        if (!deserializeLootTable(nbt)) Inventories.readNbt(nbt, items);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (!serializeLootTable(nbt)) Inventories.writeNbt(nbt, items);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return super.canPlayerUse(player);
    }
}
