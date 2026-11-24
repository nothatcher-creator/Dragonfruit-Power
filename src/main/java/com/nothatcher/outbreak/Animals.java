package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The infection reached the animals too. Feral dogs run in packs out in the open, mostly at night, and they're
 * fast. Infected rats swarm out of dark buildings. Both can pass the infection on with a bite.
 */
public final class Animals {
    public static final EntityType<FeralDog> FERAL_DOG = Registry.register(Registries.ENTITY_TYPE, Outbreak.id("feral_dog"),
            FabricEntityTypeBuilder.<FeralDog>create(SpawnGroup.MONSTER, FeralDog::new).dimensions(EntityDimensions.fixed(0.6f, 0.85f)).trackRangeBlocks(64).build());
    public static final EntityType<InfectedRat> INFECTED_RAT = Registry.register(Registries.ENTITY_TYPE, Outbreak.id("infected_rat"),
            FabricEntityTypeBuilder.<InfectedRat>create(SpawnGroup.MONSTER, InfectedRat::new).dimensions(EntityDimensions.fixed(0.4f, 0.3f)).trackRangeBlocks(48).build());

    private Animals() {}

    static void register() {
        FabricDefaultAttributeRegistry.register(FERAL_DOG, MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 14).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28));
        FabricDefaultAttributeRegistry.register(INFECTED_RAT, HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16));
    }

    /** a bite from an infected animal */
    static void bite(Entity target, Random r, float chance) {
        if (!(target instanceof PlayerEntity p) || p.getAbilities().creativeMode || p.getWorld().isClient) return;
        if (!p.getWorld().getGameRules().getBoolean(Outbreak.INFECTION) || p.hasStatusEffect(OutbreakEffects.INFECTION)) return;
        if (r.nextFloat() >= chance * Math.max(0.25f, 1f - p.getArmor() / 26f) * Characters.infectionMul(p)) return;
        p.addStatusEffect(new StatusEffectInstance(OutbreakEffects.INFECTION, InfectionEffect.DURATION, 0, false, false, true));
        p.sendMessage(Text.translatable("msg.outbreak.bitten").formatted(Formatting.DARK_RED), true);
    }

    public static class FeralDog extends WolfEntity {
        public FeralDog(EntityType<? extends WolfEntity> t, World w) {
            super(t, w);
        }

        @Override
        protected void initGoals() {
            goalSelector.add(1, new SwimGoal(this));
            goalSelector.add(3, new PounceAtTargetGoal(this, 0.4f));
            goalSelector.add(4, new MeleeAttackGoal(this, 1.25, true));
            goalSelector.add(7, new WanderAroundFarGoal(this, 0.9));
            goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 10f));
            goalSelector.add(9, new LookAroundGoal(this));
            targetSelector.add(1, new RevengeGoal(this));
            targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        }

        @Override
        public boolean tryAttack(Entity target) {
            boolean hit = super.tryAttack(target);
            if (hit) bite(target, random, 0.05f);
            return hit;
        }

        @Override
        public ActionResult interactMob(PlayerEntity player, Hand hand) {
            return ActionResult.PASS;                       // no taming these
        }

        @Override
        public boolean isBreedingItem(ItemStack stack) {
            return false;
        }

        @Override
        public WolfEntity createChild(ServerWorld w, PassiveEntity mate) {
            return null;
        }

        @Override
        public boolean canImmediatelyDespawn(double distanceSquared) {
            return true;
        }

        @Override
        public boolean isDisallowedInPeaceful() {
            return true;
        }
    }

    public static class InfectedRat extends SilverfishEntity {
        public InfectedRat(EntityType<? extends SilverfishEntity> t, World w) {
            super(t, w);
        }

        @Override
        protected void initGoals() {
            goalSelector.add(1, new SwimGoal(this));
            goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
            goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
            targetSelector.add(1, new RevengeGoal(this));
            targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        }

        @Override
        public boolean tryAttack(Entity target) {
            boolean hit = super.tryAttack(target);
            if (hit) bite(target, random, 0.02f);
            return hit;
        }
    }

    // ------------------------------------------------------------------ spawning

    static void tick(ServerWorld w) {
        if (w.getTime() % 400 != 17 || !w.getGameRules().getBoolean(Outbreak.EVENTS) || w.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.isSpectator() || p.isCreative() || w.random.nextInt(4) != 0) continue;
            boolean indoors = !w.isSkyVisible(p.getBlockPos().up());
            Box near = p.getBoundingBox().expand(48);
            if (indoors) {
                if (w.getEntitiesByType(INFECTED_RAT, near, e -> true).size() < 8) pack(w, p, INFECTED_RAT, 2 + w.random.nextInt(3), 10, 18, true);
            } else if (w.isNight() || w.random.nextInt(3) == 0) {
                if (w.getEntitiesByType(FERAL_DOG, near, e -> true).size() < 5) pack(w, p, FERAL_DOG, 2 + w.random.nextInt(2), 32, 48, false);
            }
        }
    }

    private static <T extends MobEntity> void pack(ServerWorld w, ServerPlayerEntity p, EntityType<T> type, int n, int lo, int hi, boolean dark) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double a = w.random.nextDouble() * Math.PI * 2, d = lo + w.random.nextDouble() * (hi - lo);
            int x = MathHelper.floor(p.getX() + Math.cos(a) * d), z = MathHelper.floor(p.getZ() + Math.sin(a) * d);
            BlockPos spot = ZombieSpawner.findSpot(w, x, p.getBlockY(), z);
            if (spot == null || (dark && w.isSkyVisible(spot.up())) || w.getLightLevel(spot) > 10 && dark) continue;
            for (int i = 0; i < n; i++) {
                T m = type.create(w);
                if (m == null) continue;
                m.refreshPositionAndAngles(spot.getX() + 0.5 + w.random.nextGaussian() * 0.6, spot.getY(), spot.getZ() + 0.5 + w.random.nextGaussian() * 0.6, w.random.nextFloat() * 360, 0);
                m.initialize(w, w.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
                w.spawnEntity(m);
            }
            return;
        }
    }
}
