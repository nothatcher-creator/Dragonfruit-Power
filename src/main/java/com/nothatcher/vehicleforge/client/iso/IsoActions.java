package com.nothatcher.vehicleforge.client.iso;

import com.nothatcher.vehicleforge.Net;
import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.client.IsoCam;
import com.nothatcher.vehicleforge.mixin.client.VfClientAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What clicks do in the isometric view.
 *
 * Left click: break or hit what the cursor's on, if it's in reach. Out of reach, it opens the options menu
 * (walk there, break it, use it, place here).
 *
 * Right click, in order:
 *   1. something in reach under the cursor that you can use (a mob, a vehicle, a door, a chest): use it;
 *   2. holding a block: place it exactly where the cursor points, at any distance;
 *   3. holding something you use in the air (food, a gun, a bow): use it, aimed at the cursor;
 *   4. otherwise: use the closest thing within reach that can be used (door, chest, lever, vehicle, villager...);
 *   5. nothing near and the cursor's on something far away: the options menu.
 */
public final class IsoActions {
    private static final Class<?>[] USE_SIG = {BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockHitResult.class};
    private static final Class<?>[] ITEM_USE_SIG = {World.class, PlayerEntity.class, Hand.class};
    private static final Map<Class<?>, Boolean> BLOCK_USE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> ITEM_USE = new ConcurrentHashMap<>();
    private static final double PLACE_RANGE = 64;

    /** a block being broken on your behalf after walking to it */
    private static BlockPos breakPos;
    private static Direction breakSide;
    private static int breakTicks;

    private IsoActions() {}

    // ---- what counts as usable ----

    /** does this kind of block do anything when you right-click it? (it overrides onUse, found by signature) */
    public static boolean interactable(BlockState s) {
        Block b = s.getBlock();
        // these respond to a click but you'd rarely want it by accident (retuning, lighting, eating the pumpkin...)
        if (b instanceof NoteBlock || b instanceof RedstoneWireBlock || b instanceof TntBlock || b instanceof PumpkinBlock
                || b instanceof AbstractCauldronBlock || b instanceof AbstractCandleBlock || b instanceof FlowerPotBlock
                || b instanceof AbstractRedstoneGateBlock || b instanceof DaylightDetectorBlock || b instanceof BeehiveBlock
                || b instanceof RespawnAnchorBlock || b instanceof ComposterBlock) return false;
        return BLOCK_USE.computeIfAbsent(b.getClass(), c -> overrides(c, AbstractBlock.class, USE_SIG, ActionResult.class));
    }

    private static boolean usableInAir(ItemStack st) {
        if (st.isEmpty() || st.getItem() instanceof BlockItem) return false;
        return ITEM_USE.computeIfAbsent(st.getItem().getClass(), c -> overrides(c, Item.class, ITEM_USE_SIG, null));
    }

    private static boolean overrides(Class<?> c, Class<?> base, Class<?>[] sig, Class<?> ret) {
        for (Class<?> k = c; k != null && k != base && k != Object.class; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (Arrays.equals(m.getParameterTypes(), sig) && (ret == null || m.getReturnType() == ret)) return true;
            }
        }
        return false;
    }

    private static boolean usableEntity(Entity e) {
        return e instanceof VehicleEntity || e instanceof MerchantEntity || e instanceof AbstractHorseEntity
                || e instanceof BoatEntity || e instanceof AbstractMinecartEntity || e instanceof ItemFrameEntity;
    }

    // ---- clicks ----

    /** left click. true = handled here (vanilla skips its own) */
    public static boolean onAttack(MinecraftClient mc) {
        if (!IsoCam.active() || mc.player == null) return false;
        IsoWalker.cancel();
        stopBreaking();
        if (!IsoCursor.inReach && (IsoCursor.block != null || IsoCursor.entity != null)) {
            openMenu(mc);
            return true;
        }
        return false;                                       // in reach (or at nothing): the game breaks/hits/swings
    }

    /** right click. true = handled here */
    public static boolean onUse(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (!IsoCam.active() || p == null || mc.interactionManager == null) return false;
        IsoWalker.cancel();
        stopBreaking();
        // 1. a usable thing right under the cursor, in reach
        if (IsoCursor.inReach && IsoCursor.entity != null) return false;
        if (IsoCursor.inReach && IsoCursor.block != null && interactable(mc.world.getBlockState(IsoCursor.block.getBlockPos()))
                && !(p.isSneaking() && !p.getMainHandStack().isEmpty())) return false;
        // 2. a block in hand: put it exactly where the cursor points
        for (Hand hand : Hand.values()) {
            if (p.getStackInHand(hand).getItem() instanceof BlockItem && IsoCursor.block != null) {
                if (p.getEyePos().squaredDistanceTo(IsoCursor.block.getPos()) <= PLACE_RANGE * PLACE_RANGE) place(mc, IsoCursor.block, hand);
                ((VfClientAccessor) mc).vehicleforge$setItemUseCooldown(4);
                return true;
            }
        }
        // 3. something you use in the air (eat, shoot, throw): the game does it, aimed at the cursor
        if (usableInAir(p.getMainHandStack()) || usableInAir(p.getOffHandStack())) return false;
        // in reach but not "usable": let the item act on it (hoe, bone meal, bucket, flint and steel...)
        if (IsoCursor.inReach && IsoCursor.block != null && !p.getMainHandStack().isEmpty()) return false;
        // 4. the nearest usable thing within reach
        ((VfClientAccessor) mc).vehicleforge$setItemUseCooldown(4);
        if (useNearest(mc, p)) return true;
        // 5. far away: options
        if (!IsoCursor.inReach && (IsoCursor.block != null || IsoCursor.entity != null)) {
            openMenu(mc);
            return true;
        }
        return false;
    }

    private static void openMenu(MinecraftClient mc) {
        mc.setScreen(new IsoMenuScreen(IsoCursor.block, IsoCursor.entity, IsoCursor.u(), IsoCursor.v()));
    }

    static void place(MinecraftClient mc, BlockHitResult hit, Hand hand) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockHitResult(hit);
        buf.writeEnumConstant(hand);
        ClientPlayNetworking.send(Net.ISO_PLACE, buf);
        mc.player.swingHand(hand);
    }

    /** use the closest door, chest, lever, vehicle, villager... within reach that you can see */
    private static boolean useNearest(MinecraftClient mc, ClientPlayerEntity p) {
        World w = mc.world;
        Vec3d eye = p.getEyePos();
        double reach = IsoCursor.reach(mc);
        BlockPos bestPos = null;
        Entity bestEntity = null;
        double best = reach * reach;
        int r = (int) Math.ceil(reach);
        BlockPos c = BlockPos.ofFloored(eye);
        for (BlockPos q : BlockPos.iterate(c.add(-r, -r, -r), c.add(r, r, r))) {
            BlockState s = w.getBlockState(q);
            if (s.isAir() || !interactable(s)) continue;
            double d = eye.squaredDistanceTo(Vec3d.ofCenter(q));
            if (d < best && visible(w, p, eye, q)) {
                best = d;
                bestPos = q.toImmutable();
            }
        }
        for (Entity e : w.getOtherEntities(p, new Box(eye, eye).expand(reach), IsoActions::usableEntity)) {
            if (e == p.getVehicle()) continue;
            double d = e.getBoundingBox().squaredMagnitude(eye);
            if (d < best) {
                best = d;
                bestEntity = e;
                bestPos = null;
            }
        }
        if (bestEntity != null) {
            ActionResult res = mc.interactionManager.interactEntity(p, bestEntity, Hand.MAIN_HAND);
            if (res.shouldSwingHand()) p.swingHand(Hand.MAIN_HAND);
            return true;
        }
        if (bestPos != null) {
            useBlock(mc, p, bestPos);
            return true;
        }
        return false;
    }

    private static boolean visible(World w, PlayerEntity p, Vec3d eye, BlockPos q) {
        BlockHitResult hit = w.raycast(new RaycastContext(eye, Vec3d.ofCenter(q), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(q);
    }

    static void useBlock(MinecraftClient mc, ClientPlayerEntity p, BlockPos q) {
        Vec3d eye = p.getEyePos(), center = Vec3d.ofCenter(q);
        Direction side = Direction.getFacing(eye.x - center.x, eye.y - center.y, eye.z - center.z);
        BlockHitResult hit = new BlockHitResult(center.add(Vec3d.of(side.getVector()).multiply(0.5)), side, q, false);
        IsoCursor.lookAt(p, center);
        ActionResult res = mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
        if (res.shouldSwingHand()) p.swingHand(Hand.MAIN_HAND);
    }

    // ---- breaking something you walked over to ----

    static void startBreaking(BlockPos pos, Direction side) {
        breakPos = pos;
        breakSide = side;
        breakTicks = 0;
    }

    public static void stopBreaking() {
        if (breakPos != null && MinecraftClient.getInstance().interactionManager != null) {
            MinecraftClient.getInstance().interactionManager.cancelBlockBreaking();
        }
        breakPos = null;
    }

    public static boolean breaking() {
        return breakPos != null;
    }

    /** the game's per-tick breaking step. true = handled here (keeps the game from cancelling our dig) */
    public static boolean onBreakingTick(MinecraftClient mc, boolean attackHeld) {
        if (breakPos == null) return false;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || attackHeld || !IsoCam.active() || ++breakTicks > 600) {
            stopBreaking();
            return false;
        }
        if (mc.world.getBlockState(breakPos).isAir()) {
            breakPos = null;
            return true;
        }
        IsoCursor.lookAt(p, Vec3d.ofCenter(breakPos));
        if (!mc.interactionManager.isBreakingBlock()) mc.interactionManager.attackBlock(breakPos, breakSide);
        else mc.interactionManager.updateBlockBreakingProgress(breakPos, breakSide);
        p.swingHand(Hand.MAIN_HAND);
        return true;
    }

    // ---- what the options menu runs ----

    /** stand anywhere you could reach this point from */
    static java.util.function.Predicate<BlockPos> within(Vec3d target, double reach) {
        double r = reach - 0.4;
        return feet -> new Vec3d(feet.getX() + 0.5, feet.getY() + 1.62, feet.getZ() + 0.5).squaredDistanceTo(target) <= r * r;
    }

    static void walkAndBreak(MinecraftClient mc, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getSide();
        IsoWalker.start(mc, within(Vec3d.ofCenter(pos), IsoCursor.reach(mc)), Vec3d.ofCenter(pos), () -> startBreaking(pos, side));
    }

    static void walkAndUse(MinecraftClient mc, BlockPos pos) {
        IsoWalker.start(mc, within(Vec3d.ofCenter(pos), IsoCursor.reach(mc)), Vec3d.ofCenter(pos), () -> useBlock(mc, mc.player, pos));
    }

    static void walkAndAttack(MinecraftClient mc, Entity e) {
        IsoWalker.start(mc, within(e.getPos().add(0, e.getHeight() / 2, 0), Math.min(3.0, IsoCursor.reach(mc))), e.getPos(), () -> {
            if (e.isAlive() && mc.player != null) {
                IsoCursor.lookAt(mc.player, e.getPos().add(0, e.getHeight() / 2, 0));
                mc.interactionManager.attackEntity(mc.player, e);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        });
    }

    static void walkAndUseEntity(MinecraftClient mc, Entity e) {
        IsoWalker.start(mc, within(e.getPos().add(0, e.getHeight() / 2, 0), IsoCursor.reach(mc)), e.getPos(), () -> {
            if (e.isAlive() && mc.player != null) {
                ActionResult res = mc.interactionManager.interactEntity(mc.player, e, Hand.MAIN_HAND);
                if (res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
            }
        });
    }

    /** walk round to the back of a vehicle and open its cargo (a click on the back, as if you'd clicked there) */
    static void walkAndOpenCargo(MinecraftClient mc, VehicleEntity v) {
        IsoWalker.start(mc, within(v.cargoPoint(), IsoCursor.reach(mc)), v.cargoPoint(), () -> {
            if (v.isAlive() && mc.player != null) {
                Vec3d at = v.cargoPoint();
                IsoCursor.lookAt(mc.player, at);
                ActionResult res = mc.interactionManager.interactEntityAtLocation(mc.player, v,
                        new net.minecraft.util.hit.EntityHitResult(v, at), Hand.MAIN_HAND);
                if (res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
            }
        });
    }

    static void walkTo(MinecraftClient mc, BlockPos spot) {
        IsoWalker.start(mc, feet -> feet.equals(spot), Vec3d.ofBottomCenter(spot), null);
    }

    static void walkToEntity(MinecraftClient mc, Entity e) {
        IsoWalker.start(mc, within(e.getPos().add(0, 1, 0), 2.5), e.getPos(), null);
    }

    static boolean usable(Entity e) {
        return usableEntity(e);
    }
}
