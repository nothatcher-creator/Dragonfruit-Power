package com.nothatcher.vehicleforge.block;

import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/**
 * An elevator pad: stand on it and jump to ride up to the next pad above, or sneak to go down to the next one
 * below (up to 48 blocks, as long as there's room to stand). Every floor of a tall city building has one by
 * its ladder shaft.
 */
public class ElevatorBlock extends Block {
    public ElevatorBlock(Settings s) {
        super(s);
    }

    @Override
    public void appendTooltip(net.minecraft.item.ItemStack stack, net.minecraft.world.BlockView world, java.util.List<net.minecraft.text.Text> tooltip,
                              net.minecraft.client.item.TooltipContext ctx) {
        tooltip.add(net.minecraft.text.Text.translatable("tooltip.vehicleforge.elevator").formatted(net.minecraft.util.Formatting.GRAY));
    }

    public static void ride(ServerPlayerEntity p, boolean up) {
        ServerWorld w = p.getServerWorld();
        BlockPos under = p.getBlockPos().down();
        if (!(w.getBlockState(under).getBlock() instanceof ElevatorBlock)) {
            under = p.getBlockPos();                                         // standing on its top face
            if (!(w.getBlockState(under).getBlock() instanceof ElevatorBlock)) return;
        }
        if (!com.nothatcher.vehicleforge.power.PowerGrid.cityPowered(w, under)) {
            p.sendMessage(net.minecraft.text.Text.translatable("msg.vehicleforge.elevator_dead").formatted(net.minecraft.util.Formatting.GRAY), true);
            w.playSound(null, under, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.5f, 0.7f);
            return;
        }
        for (int i = 1; i <= 48; i++) {
            BlockPos pad = under.up(up ? i : -i);
            if (pad.getY() < w.getBottomY() || pad.getY() > w.getTopY()) return;
            if (!(w.getBlockState(pad).getBlock() instanceof ElevatorBlock)) continue;
            if (!w.getBlockState(pad.up()).getCollisionShape(w, pad.up()).isEmpty() || !w.getBlockState(pad.up(2)).getCollisionShape(w, pad.up(2)).isEmpty()) continue;
            p.teleport(w, p.getX(), pad.getY() + 1, p.getZ(), p.getYaw(), p.getPitch());
            w.playSound(null, pad, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.3f, 1.4f);
            w.playSound(null, pad, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.6f, 1.8f);
            return;
        }
    }
}
