package com.nothatcher.outbreak;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** What a barricade is covering, how much the top board (or the metal) has left, and which planks went on. */
public class BarricadeBlockEntity extends BlockEntity {
    /** the door half, window or air that was here */
    public BlockState original = Blocks.AIR.getDefaultState();
    /** health of the outermost board, or of the sheet metal */
    public float hp = Barricades.BOARD_HP;
    /** item ids of the planks nailed on, first to last, so prying one off gives back the same wood */
    public final List<String> planks = new ArrayList<>();

    public BarricadeBlockEntity(BlockPos pos, BlockState state) {
        super(OutbreakBlocks.BARRICADE_BE, pos, state);
    }

    public void sync() {
        markDirty();
        if (world != null && !world.isClient) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("Original", NbtHelper.fromBlockState(original));
        nbt.putFloat("Hp", hp);
        NbtList list = new NbtList();
        for (String s : planks) list.add(NbtString.of(s));
        nbt.put("Planks", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        original = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("Original"));
        hp = nbt.contains("Hp") ? nbt.getFloat("Hp") : Barricades.BOARD_HP;
        planks.clear();
        NbtList list = nbt.getList("Planks", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) planks.add(list.getString(i));
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
