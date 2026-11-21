package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Car keys for the cars Vehicle Forge parks in the city.
 *
 * Most have their keys in the ignition: the first time you get in, you pocket them. About a third are locked, and
 * their key is close by: on a zombie (it holds it, and drops it when killed) or lying on the ground near the car,
 * where it won't despawn. Without the key in your inventory a locked car won't let you in or open its trunk.
 *
 * With the key in hand, sneak and use it on the car to lock or unlock it. Lost it? Sneak and use a crowbar,
 * hammer, wrench, sledgehammer or fire axe to smash a window in. That's loud.
 *
 * Everything is kept in command tags on the vehicle, so this needs nothing from Vehicle Forge at compile time;
 * cars a player placed themselves never get keys or locks.
 */
public final class CarKeys {
    /** set by Vehicle Forge on the cars its city generator parks */
    private static final String PARKED = "vehicleforge_parked";
    private static final String DONE = "outbreak_keyed", LOCKED = "outbreak_locked", TAKEN = "outbreak_keytaken", WRECK = "outbreak_wreck";
    private static final float LOCKED_SHARE = 0.32f, ON_ZOMBIE = 0.6f;
    private static final List<Entity> PENDING = new ArrayList<>();

    private CarKeys() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || player.isSpectator()) return ActionResult.PASS;
            return use(player, (ServerWorld) world, entity, hit == null);
        });
    }

    /** new city cars arrive with their chunk; they're set up on the next tick (not mid-load) */
    public static void onLoad(Entity e, ServerWorld w) {
        Set<String> tags = e.getCommandTags();
        if (tags.contains(PARKED) && !tags.contains(DONE)) PENDING.add(e);
    }

    public static void tick() {
        if (PENDING.isEmpty()) return;
        List<Entity> batch = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Entity e : batch) if (!e.isRemoved() && !e.getCommandTags().contains(DONE) && e.getWorld() instanceof ServerWorld w) setUp(w, e);
    }

    private static void setUp(ServerWorld w, Entity car) {
        car.addCommandTag(DONE);
        if (car.writeNbt(new NbtCompound()).getBoolean("Wreck")) {
            car.addCommandTag(WRECK);
            return;
        }
        Random r = new Random(car.getUuid().getLeastSignificantBits() ^ 0x6b65797300L);
        if (r.nextFloat() >= LOCKED_SHARE) return;                         // keys in the ignition
        BlockPos spot = groundNear(w, car.getBlockPos(), r);
        if (spot == null) return;
        car.addCommandTag(LOCKED);
        ItemStack key = CarKeyItem.forVehicle(car);
        if (r.nextFloat() < ON_ZOMBIE) {
            ZombieEntity z = EntityType.ZOMBIE.create(w);
            if (z != null) {
                z.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, r.nextFloat() * 360f, 0);
                z.initialize(w, w.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
                z.equipStack(EquipmentSlot.OFFHAND, key);
                z.setEquipmentDropChance(EquipmentSlot.OFFHAND, 2.0f);      // always drops
                z.setPersistent();                                          // or the key would vanish with it
                z.addCommandTag("outbreak_keyholder");
                w.spawnEntity(z);
                return;
            }
        }
        ItemEntity item = new ItemEntity(w, spot.getX() + 0.5, spot.getY() + 0.05, spot.getZ() + 0.5, key, 0, 0, 0);
        item.setNeverDespawn();
        w.spawnEntity(item);
    }

    /** an open spot on the ground 3 to 10 blocks from the car, about level with it */
    private static BlockPos groundNear(ServerWorld w, BlockPos car, Random r) {
        for (int i = 0; i < 16; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 3 + r.nextDouble() * 7;
            int x = car.getX() + (int) Math.round(Math.cos(a) * d), z = car.getZ() + (int) Math.round(Math.sin(a) * d);
            BlockPos p = new BlockPos(x, w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!w.isChunkLoaded(p) || Math.abs(p.getY() - car.getY()) > 4) continue;
            if (w.getBlockState(p.down()).isSolidBlock(w, p.down()) && w.isAir(p) && w.isAir(p.up())) return p;
        }
        return null;
    }

    public static boolean hasKey(PlayerEntity p, Entity car) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (CarKeyItem.fits(inv.getStack(i), car)) return true;
        return false;
    }

    private static ActionResult use(PlayerEntity p, ServerWorld w, Entity car, boolean getIn) {
        Set<String> tags = car.getCommandTags();
        if (!tags.contains(DONE) || tags.contains(WRECK)) return ActionResult.PASS;
        ItemStack held = p.getMainHandStack();
        boolean locked = tags.contains(LOCKED);

        // lock or unlock with its key
        if (p.isSneaking() && CarKeyItem.fits(held, car)) {
            if (locked) car.removeCommandTag(LOCKED);
            else car.addCommandTag(LOCKED);
            w.playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 0.5f, locked ? 1.6f : 1.3f);
            tell(p, locked ? "msg.outbreak.car_unlock" : "msg.outbreak.car_lock", Formatting.GRAY);
            return ActionResult.SUCCESS;
        }
        if (locked && !hasKey(p, car)) {
            if (p.isSneaking() && Actions.isTool(held)) {                 // break in
                car.removeCommandTag(LOCKED);
                w.playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.9f);
                Noise.emit(w, car.getPos(), 24);
                held.damage(2, p, pl -> pl.sendToolBreakStatus(Hand.MAIN_HAND));
                tell(p, "msg.outbreak.car_smashed", Formatting.GOLD);
                return ActionResult.SUCCESS;
            }
            w.playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.PLAYERS, 0.35f, 1.7f);
            tell(p, "msg.outbreak.car_locked", Formatting.RED);
            return ActionResult.FAIL;
        }
        // getting in an unlocked car that still has its keys in it
        if (getIn && !locked && !tags.contains(TAKEN) && !p.hasVehicle()) {
            car.addCommandTag(TAKEN);
            p.getInventory().offerOrDrop(CarKeyItem.forVehicle(car));
            tell(p, "msg.outbreak.keys_in_ignition", Formatting.GRAY);
        }
        return ActionResult.PASS;
    }

    private static void tell(PlayerEntity p, String key, Formatting f) {
        if (p instanceof ServerPlayerEntity sp) sp.sendMessage(Text.translatable(key).formatted(f), true);
    }

    static boolean isVehicle(Entity e) {
        Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
        return id.getNamespace().equals("vehicleforge");
    }
}
