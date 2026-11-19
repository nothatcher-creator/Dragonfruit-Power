package com.nothatcher.outbreak;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * Keeps the world crawling with the dead: around every player, day and night, it tops the local zombie count up to
 * the outbreakZombieCap game rule (half as many again at night). Zombies walk in groups, wear ordinary clothes
 * (the occasional cop or soldier among them), mostly shamble, and a few sprint.
 */
public final class ZombieSpawner {
    private static final int[] CLOTHES = {0x3B5E8C, 0x8C3B3B, 0x3B8C4A, 0x555555, 0xD8D8D8, 0x2A2A2A, 0x8C7A3B, 0x6A3B8C, 0xC07A3B, 0x1E3A5F};

    private ZombieSpawner() {}

    static void tick(ServerWorld w) {
        if (w.getTime() % 20 != 0 || w.getRegistryKey() != World.OVERWORLD) return;
        if (!w.getGameRules().getBoolean(Outbreak.ZOMBIES) || w.getDifficulty() == Difficulty.PEACEFUL) return;
        int cap = w.getGameRules().getInt(Outbreak.CAP);
        if (w.isNight()) cap = cap * 3 / 2;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.isSpectator()) continue;
            int near = w.getEntitiesByClass(ZombieEntity.class, p.getBoundingBox().expand(64, 40, 64), z -> z.isAlive()).size();
            if (near >= cap) continue;
            int groups = 1 + w.random.nextInt(2);
            for (int i = 0; i < groups; i++) spawnGroup(w, p);
        }
    }

    private static void spawnGroup(ServerWorld w, ServerPlayerEntity p) {
        Random r = w.random;
        double a = r.nextDouble() * Math.PI * 2, dist = 22 + r.nextDouble() * 32;
        int x = MathHelper.floor(p.getX() + Math.cos(a) * dist), z = MathHelper.floor(p.getZ() + Math.sin(a) * dist);
        if (!w.isChunkLoaded(new BlockPos(x, 0, z))) return;
        BlockPos spot = findSpot(w, x, p.getBlockY(), z);
        if (spot == null) return;
        if (w.getClosestPlayer(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 18, false) != null) return;
        int n = 1 + r.nextInt(3) + (w.isNight() ? r.nextInt(2) : 0);
        for (int i = 0; i < n; i++) {
            EntityType<? extends ZombieEntity> type = r.nextInt(10) == 0 ? EntityType.ZOMBIE_VILLAGER : EntityType.ZOMBIE;
            ZombieEntity zed = type.create(w);
            if (zed == null) continue;
            zed.refreshPositionAndAngles(spot.getX() + 0.5 + r.nextDouble() - 0.5, spot.getY(), spot.getZ() + 0.5 + r.nextDouble() - 0.5, r.nextFloat() * 360, 0);
            zed.initialize(w, w.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
            dress(zed, r, w);
            w.spawnEntityAndPassengers(zed);
        }
    }

    /** a free spot near the player's own height (so zombies turn up inside buildings too), else the surface */
    private static BlockPos findSpot(ServerWorld w, int x, int y, int z) {
        for (int dy = 8; dy >= -16; dy--) {
            BlockPos at = new BlockPos(x, y + dy, z);
            if (ok(w, at)) return at;
        }
        BlockPos top = w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        return ok(w, top) ? top : null;
    }

    private static boolean ok(ServerWorld w, BlockPos at) {
        BlockState below = w.getBlockState(at.down());
        return below.isSideSolidFullSquare(w, at.down(), Direction.UP) && w.isAir(at) && w.isAir(at.up())
                && w.getFluidState(at).isEmpty() && at.getY() > w.getBottomY();
    }

    static void dress(ZombieEntity z, Random r, ServerWorld w) {
        z.setBaby(false);
        z.setCanBreakDoors(true);
        EntityAttributeInstance follow = z.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(40);
        EntityAttributeInstance speed = z.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        boolean sprinter = r.nextInt(100) < w.getGameRules().getInt(Outbreak.SPRINTERS);
        if (speed != null) speed.setBaseValue(sprinter ? 0.32 : 0.15 + r.nextDouble() * 0.07);
        // everyday clothes
        if (z.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) z.equipStack(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, r));
        if (z.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) z.equipStack(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, r));
        if (r.nextInt(3) == 0 && z.getEquippedStack(EquipmentSlot.FEET).isEmpty()) z.equipStack(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, r));
        z.setEquipmentDropChance(EquipmentSlot.CHEST, 0.02f);
        z.setEquipmentDropChance(EquipmentSlot.LEGS, 0.02f);
        z.setEquipmentDropChance(EquipmentSlot.FEET, 0.02f);
        // the odd police officer, soldier or rioter, still wearing their gear
        int job = r.nextInt(100);
        if (job < 3) {
            z.equipStack(EquipmentSlot.CHEST, new ItemStack(OutbreakItems.POLICE_VEST));
            z.setEquipmentDropChance(EquipmentSlot.CHEST, 0.4f);
            if (r.nextInt(3) == 0) {
                z.equipStack(EquipmentSlot.MAINHAND, new ItemStack(OutbreakItems.AMMO_9MM, 4 + r.nextInt(8)));
                z.setEquipmentDropChance(EquipmentSlot.MAINHAND, 1f);
            }
        } else if (job < 5) {
            z.equipStack(EquipmentSlot.CHEST, new ItemStack(OutbreakItems.MILITARY_VEST));
            z.equipStack(EquipmentSlot.HEAD, new ItemStack(OutbreakItems.MILITARY_HELMET));
            z.setEquipmentDropChance(EquipmentSlot.CHEST, 0.3f);
            z.setEquipmentDropChance(EquipmentSlot.HEAD, 0.3f);
        } else if (job < 6) {
            z.equipStack(EquipmentSlot.HEAD, new ItemStack(OutbreakItems.RIOT_HELMET));
            z.setEquipmentDropChance(EquipmentSlot.HEAD, 0.4f);
        }
    }

    private static ItemStack dyed(net.minecraft.item.Item item, Random r) {
        ItemStack s = new ItemStack(item);
        if (item instanceof DyeableItem d) d.setColor(s, CLOTHES[r.nextInt(CLOTHES.length)]);
        return s;
    }
}
