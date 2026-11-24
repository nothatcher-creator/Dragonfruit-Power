package com.nothatcher.vehicleforge.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

/** What a neon sign says and how it looks. Text lines are split with '|'. */
public class NeonSignBlockEntity extends BlockEntity {
    public static final int[] COLORS = {0xFF3FA6, 0xFF2A2A, 0xFF8A1E, 0xFFE83A, 0x4BFF5A, 0x3AF2FF, 0x3A7BFF, 0xB04BFF, 0xFFFFFF};
    public static final String[] COLOR_NAMES = {"Pink", "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Purple", "White"};
    public static final int MAX_SIZE = 8, MAX_LEN = 48;

    public String text = "OPEN";
    /** any colour, 0xRRGGBB */
    public int color = COLORS[0], size = 2;
    public boolean flicker;

    public NeonSignBlockEntity(BlockPos pos, BlockState st) {
        super(CityExtras.NEON_BE, pos, st);
    }

    public void set(String text, int color, int size, boolean flicker) {
        this.text = text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text;
        this.color = color & 0xFFFFFF;
        this.size = Math.max(1, Math.min(MAX_SIZE, size));
        this.flicker = flicker;
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putString("Text", text);
        nbt.putInt("Rgb", color);
        nbt.putInt("Size", size);
        nbt.putBoolean("Flicker", flicker);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("Text")) text = nbt.getString("Text");
        color = nbt.contains("Rgb") ? nbt.getInt("Rgb") & 0xFFFFFF : COLORS[Math.floorMod(nbt.getInt("Color"), COLORS.length)];
        size = nbt.contains("Size") ? Math.max(1, Math.min(MAX_SIZE, nbt.getInt("Size"))) : 2;
        flicker = nbt.getBoolean("Flicker");
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
