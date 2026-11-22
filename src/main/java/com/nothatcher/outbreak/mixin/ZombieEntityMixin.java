package com.nothatcher.outbreak.mixin;

import com.nothatcher.outbreak.InfectionEffect;
import com.nothatcher.outbreak.NoiseGoal;
import com.nothatcher.outbreak.Outbreak;
import com.nothatcher.outbreak.OutbreakEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity implements com.nothatcher.outbreak.ZombieJob.Holder {
    @org.spongepowered.asm.mixin.Unique
    private static final net.minecraft.entity.data.TrackedData<Integer> OUTBREAK_JOB =
            net.minecraft.entity.data.DataTracker.registerData(ZombieEntity.class, net.minecraft.entity.data.TrackedDataHandlerRegistry.INTEGER);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void outbreak$trackJob(CallbackInfo ci) {
        this.dataTracker.startTracking(OUTBREAK_JOB, 0);
    }

    @Override
    public com.nothatcher.outbreak.ZombieJob outbreak$job() {
        return com.nothatcher.outbreak.ZombieJob.byOrdinal(this.dataTracker.get(OUTBREAK_JOB) & 15);
    }

    @Override
    public void outbreak$setJob(com.nothatcher.outbreak.ZombieJob job) {
        this.dataTracker.set(OUTBREAK_JOB, (this.dataTracker.get(OUTBREAK_JOB) & ~15) | job.ordinal());
    }

    @Override
    public int outbreak$variant() {
        return this.dataTracker.get(OUTBREAK_JOB) >> 4;
    }

    @Override
    public void outbreak$setVariant(int variant) {
        this.dataTracker.set(OUTBREAK_JOB, (this.dataTracker.get(OUTBREAK_JOB) & 15) | (variant << 4));
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void outbreak$saveJob(net.minecraft.nbt.NbtCompound nbt, CallbackInfo ci) {
        nbt.putString("OutbreakJob", outbreak$job().id());
        nbt.putInt("OutbreakVariant", outbreak$variant());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void outbreak$loadJob(net.minecraft.nbt.NbtCompound nbt, CallbackInfo ci) {
        outbreak$setVariant(nbt.getInt("OutbreakVariant"));
        if (!nbt.contains("OutbreakJob")) return;
        for (com.nothatcher.outbreak.ZombieJob j : com.nothatcher.outbreak.ZombieJob.values())
            if (j.id().equals(nbt.getString("OutbreakJob"))) outbreak$setJob(j);
    }

    protected ZombieEntityMixin(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    /** the dead don't burn in the sun */
    @Inject(method = "burnsInDaylight", at = @At("HEAD"), cancellable = true)
    private void outbreak$noSunburn(CallbackInfoReturnable<Boolean> cir) {
        if (this.getWorld().getGameRules().getBoolean(Outbreak.DAYLIGHT)) cir.setReturnValue(false);
    }

    /** they come to noise */
    @Inject(method = "initCustomGoals", at = @At("TAIL"))
    private void outbreak$noiseGoal(CallbackInfo ci) {
        this.goalSelector.add(4, new NoiseGoal((ZombieEntity) (Object) this));
        this.goalSelector.add(1, new com.nothatcher.outbreak.BarricadeGoal((ZombieEntity) (Object) this));
    }

    /** bites can infect: less likely through armour */
    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void outbreak$bite(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || this.getWorld().isClient) return;
        if (!(target instanceof PlayerEntity p) || p.getAbilities().creativeMode || p.isDead()) return;
        if (!this.getWorld().getGameRules().getBoolean(Outbreak.INFECTION) || p.hasStatusEffect(OutbreakEffects.INFECTION)) return;
        float chance = 0.10f * Math.max(0.25f, 1f - p.getArmor() / 26f);
        if (this.random.nextFloat() >= chance) return;
        p.addStatusEffect(new StatusEffectInstance(OutbreakEffects.INFECTION, InfectionEffect.DURATION, 0, false, false, true));
        p.sendMessage(Text.translatable("msg.outbreak.bitten").formatted(Formatting.DARK_RED), true);
    }
}
