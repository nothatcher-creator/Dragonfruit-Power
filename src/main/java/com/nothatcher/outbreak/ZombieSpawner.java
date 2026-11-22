package com.nothatcher.outbreak;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Keeps zombies around: every few seconds, near each player, it may add a small group (out of sight) if there are
 * fewer than outbreakZombieCap nearby and the area hasn't been cleared out (see {@link Population}).
 * Zombies wear ordinary clothes (the odd cop, soldier, firefighter or builder among them), mostly shamble,
 * and a few sprint.
 */
public final class ZombieSpawner {
    private static final int[] CLOTHES = {0x3B5E8C, 0x8C3B3B, 0x3B8C4A, 0x555555, 0xD8D8D8, 0x2A2A2A, 0x8C7A3B, 0x6A3B8C, 0xC07A3B, 0x1E3A5F};

    private ZombieSpawner() {}

    static void tick(ServerWorld w) {
        if (w.getTime() % 60 != 0 || w.getRegistryKey() != World.OVERWORLD) return;
        if (!w.getGameRules().getBoolean(Outbreak.ZOMBIES) || w.getDifficulty() == Difficulty.PEACEFUL) return;
        cullStragglers(w);
        int cap = w.getGameRules().getInt(Outbreak.CAP);
        if (w.isNight()) cap = cap * 7 / 5;
        Population pop = Population.of(w);
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.isSpectator()) continue;
            int near = w.getEntitiesByClass(ZombieEntity.class, p.getBoundingBox().expand(64, 40, 64), z -> z.isAlive()).size();
            if (near >= cap) continue;
            // the emptier it is around you, the more likely a group shuffles in; cleared areas stay quiet
            float fill = near / (float) Math.max(1, cap);
            if (w.random.nextFloat() > 0.65f * (1f - fill * 0.6f)) continue;
            spawnGroup(w, p, pop);
        }
    }

    /**
     * The cap only counts zombies within 64 blocks, and vanilla only clears ordinary zombies once they're 128 blocks
     * away (and barely at all at night), so as you travel a trail of them built up behind you, all still running
     * their AI. Ones well out of reach, not chasing anyone, are let go. Horde, named, and turned zombies are kept.
     */
    private static void cullStragglers(ServerWorld w) {
        for (ZombieEntity z : w.getEntitiesByType(net.minecraft.util.TypeFilter.instanceOf(ZombieEntity.class),
                z -> z.isAlive() && !z.isPersistent() && !z.hasCustomName() && z.getTarget() == null && !z.getCommandTags().contains(Airport.TAG))) {
            if (w.getClosestPlayer(z, 96) == null) z.discard();
        }
    }

    private static void spawnGroup(ServerWorld w, ServerPlayerEntity p, Population pop) {
        Random r = w.random;
        double a = r.nextDouble() * Math.PI * 2, dist = 28 + r.nextDouble() * 30;
        int x = MathHelper.floor(p.getX() + Math.cos(a) * dist), z = MathHelper.floor(p.getZ() + Math.sin(a) * dist);
        if (!w.isChunkLoaded(new BlockPos(x, 0, z))) return;
        BlockPos spot = findSpot(w, x, p.getBlockY(), z);
        if (spot == null) return;
        if (w.getClosestPlayer(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 24, false) != null) return;
        if (r.nextFloat() > pop.remaining(w, spot)) return;
        if (Airport.at(w, spot)) return;                                     // the airport's dead are all already there
        if (inSight(w, p, spot)) return;
        int n = 1 + r.nextInt(2) + (w.isNight() && r.nextBoolean() ? 1 : 0);
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

    /** don't pop zombies into existence where the player is looking */
    private static boolean inSight(ServerWorld w, ServerPlayerEntity p, BlockPos spot) {
        Vec3d eye = p.getEyePos(), to = Vec3d.ofBottomCenter(spot).add(0, 1.4, 0);
        Vec3d dir = to.subtract(eye);
        if (dir.normalize().dotProduct(p.getRotationVec(1f)) < 0.2) return false;
        return w.raycast(new RaycastContext(eye, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p)).getType() == HitResult.Type.MISS;
    }

    /** a free spot near the player's own height (so zombies turn up inside buildings too), else the surface */
    static BlockPos findSpot(ServerWorld w, int x, int y, int z) {
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
        if (follow != null) follow.setBaseValue(36);
        EntityAttributeInstance speed = z.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        boolean sprinter = r.nextInt(100) < w.getGameRules().getInt(Outbreak.SPRINTERS);
        if (speed != null) speed.setBaseValue(sprinter ? 0.30 : 0.14 + r.nextDouble() * 0.07);
        // the job they had: a uniform (their skin), the gear that went with it, and what's in their pockets
        ZombieJob job = ZombieJob.pick(w, z.getX(), z.getY(), z.getZ(), r);
        if (z instanceof ZombieJob.Holder h) h.outbreak$setJob(job);
        ZombieVariant variant = sprinter ? ZombieVariant.NORMAL : ZombieVariant.roll(r);
        switch (job) {
            case COP -> {
                wear(z, EquipmentSlot.CHEST, new ItemStack(OutbreakItems.POLICE_VEST), 0.35f);
                if (r.nextInt(4) == 0) wear(z, EquipmentSlot.HEAD, new ItemStack(OutbreakItems.RIOT_HELMET), 0.3f);
                int k = r.nextInt(10);
                if (k < 4) hold(z, new ItemStack(OutbreakItems.POLICE_BATON), 0.4f);
                else if (k < 5) hold(z, new ItemStack(OutbreakItems.PISTOL), 0.25f);
                else if (k < 8) hold(z, new ItemStack(OutbreakItems.AMMO_9MM, 4 + r.nextInt(8)), 1f);
            }
            case SOLDIER -> {
                wear(z, EquipmentSlot.CHEST, new ItemStack(OutbreakItems.MILITARY_VEST), 0.3f);
                wear(z, EquipmentSlot.HEAD, new ItemStack(OutbreakItems.MILITARY_HELMET), 0.3f);
                wear(z, EquipmentSlot.LEGS, new ItemStack(OutbreakItems.CARGO_PANTS), 0.2f);
                wear(z, EquipmentSlot.FEET, new ItemStack(OutbreakItems.WORK_BOOTS), 0.2f);
                int k = r.nextInt(10);
                if (k < 4) hold(z, new ItemStack(OutbreakItems.AMMO_RIFLE, 4 + r.nextInt(10)), 1f);
                else if (k < 6) hold(z, new ItemStack(OutbreakItems.MRE), 1f);
                else if (k < 7) hold(z, new ItemStack(OutbreakItems.ASSAULT_RIFLE), 0.15f);
            }
            case FIREFIGHTER -> {
                wear(z, EquipmentSlot.HEAD, new ItemStack(OutbreakItems.FIREFIGHTER_HELMET), 0.25f);
                wear(z, EquipmentSlot.CHEST, new ItemStack(OutbreakItems.FIREFIGHTER_JACKET), 0.25f);
                wear(z, EquipmentSlot.LEGS, new ItemStack(OutbreakItems.FIREFIGHTER_PANTS), 0.25f);
                if (r.nextInt(4) == 0) hold(z, new ItemStack(OutbreakItems.FIRE_AXE), 0.3f);
            }
            case BUILDER -> {
                wear(z, EquipmentSlot.HEAD, new ItemStack(OutbreakItems.HARD_HAT), 0.2f);
                wear(z, EquipmentSlot.CHEST, new ItemStack(OutbreakItems.HI_VIS_VEST), 0.2f);
                wear(z, EquipmentSlot.FEET, new ItemStack(OutbreakItems.WORK_BOOTS), 0.15f);
                int k = r.nextInt(10);
                if (k < 2) hold(z, new ItemStack(OutbreakItems.CLAW_HAMMER), 0.35f);
                else if (k < 3) hold(z, new ItemStack(OutbreakItems.SLEDGEHAMMER), 0.3f);
                else if (k < 6) hold(z, new ItemStack(OutbreakItems.NAILS, 4 + r.nextInt(12)), 1f);
            }
            case DOCTOR -> {
                int k = r.nextInt(10);
                if (k < 4) hold(z, new ItemStack(OutbreakItems.BANDAGE, 1 + r.nextInt(2)), 1f);
                else if (k < 6) hold(z, new ItemStack(OutbreakItems.PAINKILLERS), 1f);
                else if (k < 7) hold(z, new ItemStack(OutbreakItems.ANTIBIOTICS), 1f);
                if (r.nextInt(5) == 0) wear(z, EquipmentSlot.HEAD, new ItemStack(OutbreakItems.GAS_MASK), 0.3f);
            }
            case PILOT -> {
                int k = r.nextInt(10);
                if (k < 3) hold(z, new ItemStack(OutbreakItems.FLASHLIGHT), 0.5f);
                else if (k < 5) hold(z, new ItemStack(OutbreakItems.ENERGY_DRINK), 1f);
            }
            case MECHANIC -> {
                wear(z, EquipmentSlot.FEET, new ItemStack(OutbreakItems.WORK_BOOTS), 0.15f);
                int k = r.nextInt(10);
                if (k < 3) hold(z, new ItemStack(OutbreakItems.PIPE_WRENCH), 0.35f);
                else if (k < 5) hold(z, new ItemStack(OutbreakItems.SCRAP_METAL, 1 + r.nextInt(3)), 1f);
                else if (k < 6) hold(z, new ItemStack(OutbreakItems.CAR_BATTERY), 1f);
            }
            case PRISONER -> {
                if (r.nextInt(5) == 0) hold(z, new ItemStack(OutbreakItems.KITCHEN_KNIFE), 0.3f);
            }
            default -> {
                if (r.nextInt(40) == 0) wear(z, EquipmentSlot.HEAD, new ItemStack(OutbreakItems.GAS_MASK), 0.3f);
            }
        }
        variant.apply(z);
        boolean uniform = job == ZombieJob.PILOT || job == ZombieJob.DOCTOR || job == ZombieJob.MECHANIC || job == ZombieJob.PRISONER || job == ZombieJob.COP;
        if (uniform) return;                                   // the skin is the outfit: no random clothes over it
        // everyday clothes for whatever's left
        if (z.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
            int c = r.nextInt(10);
            wear(z, EquipmentSlot.CHEST, c < 3 ? new ItemStack(OutbreakItems.HOODIE) : c < 5 ? new ItemStack(OutbreakItems.LEATHER_JACKET)
                    : dyed(Items.LEATHER_CHESTPLATE, r), 0.05f);
        }
        if (z.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) {
            int c = r.nextInt(10);
            wear(z, EquipmentSlot.LEGS, c < 5 ? new ItemStack(OutbreakItems.JEANS) : c < 7 ? new ItemStack(OutbreakItems.CARGO_PANTS)
                    : dyed(Items.LEATHER_LEGGINGS, r), 0.05f);
        }
        if (z.getEquippedStack(EquipmentSlot.FEET).isEmpty() && r.nextInt(3) > 0)
            wear(z, EquipmentSlot.FEET, new ItemStack(r.nextBoolean() ? OutbreakItems.SNEAKERS : OutbreakItems.WORK_BOOTS), 0.05f);
        if (z.getEquippedStack(EquipmentSlot.HEAD).isEmpty() && r.nextInt(6) == 0)
            wear(z, EquipmentSlot.HEAD, new ItemStack(r.nextBoolean() ? OutbreakItems.BEANIE : OutbreakItems.BASEBALL_CAP), 0.08f);
    }

    private static void hold(ZombieEntity z, ItemStack s, float drop) {
        z.equipStack(EquipmentSlot.MAINHAND, s);
        z.setEquipmentDropChance(EquipmentSlot.MAINHAND, drop);
    }

    private static void wear(ZombieEntity z, EquipmentSlot slot, ItemStack s, float drop) {
        z.equipStack(slot, s);
        z.setEquipmentDropChance(slot, drop);
    }

    private static ItemStack dyed(Item item, Random r) {
        ItemStack s = new ItemStack(item);
        if (item instanceof DyeableItem d) d.setColor(s, CLOTHES[r.nextInt(CLOTHES.length)]);
        return s;
    }
}
