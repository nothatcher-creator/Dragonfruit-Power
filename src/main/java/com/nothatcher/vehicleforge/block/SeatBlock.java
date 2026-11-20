package com.nothatcher.vehicleforge.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** Something you can sit on: use it with an empty hand (sneak to get up). */
public class SeatBlock extends FacingShapeBlock {
    private final double seatHeight;

    public SeatBlock(Settings settings, VoxelShape shape, double seatHeight) {
        super(settings, shape, false);
        this.seatHeight = seatHeight;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isSneaking() || player.hasVehicle() || !player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;
        if (!world.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true).isEmpty()) return ActionResult.FAIL;   // taken
        SeatEntity seat = CityExtras.SEAT.create(world);
        if (seat == null) return ActionResult.FAIL;
        seat.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + seatHeight - 0.1, pos.getZ() + 0.5, state.get(FACING).asRotation(), 0);
        world.spawnEntity(seat);
        player.startRiding(seat);
        return ActionResult.CONSUME;
    }
}
