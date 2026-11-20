package com.nothatcher.outbreak;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Thrown pipe bombs (blast on impact: hurts, doesn't wreck buildings), molotovs (a pool of fire) and noise makers
 * (sit where they land beeping for 15 seconds, pulling every zombie in earshot towards them).
 */
public class ThrownGear extends ThrownItemEntity {
    private int beeping = -1;

    public ThrownGear(EntityType<? extends ThrownGear> type, World world) {
        super(type, world);
    }

    public ThrownGear(World world, LivingEntity owner, Item item) {
        super(OutbreakItems.THROWN, owner, world);
        setItem(item.getDefaultStack());
    }

    @Override
    protected Item getDefaultItem() {
        return OutbreakItems.PIPE_BOMB;
    }

    @Override
    public void tick() {
        if (beeping >= 0) {
            setVelocity(Vec3d.ZERO);
            if (getWorld() instanceof ServerWorld w) {
                if (beeping % 10 == 0) {
                    w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 2.5f, beeping % 20 == 0 ? 1.8f : 1.5f);
                    Noise.emit(w, getPos(), 48);
                }
                if (++beeping > 300) discard();
            }
            return;
        }
        super.tick();
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!(getWorld() instanceof ServerWorld w)) return;
        Item item = getStack().getItem();
        Vec3d at = hit.getPos();
        if (item == OutbreakItems.NOISE_MAKER) {
            if (beeping < 0) {
                beeping = 0;
                setNoGravity(true);
                refreshPositionAndAngles(at.x, at.y, at.z, getYaw(), getPitch());
            }
            return;
        }
        if (item == OutbreakItems.MOLOTOV) {
            w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2f, 1f);
            w.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.2f, 0.8f);
            BlockPos c = BlockPos.ofFloored(at);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > 5) continue;
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos p = c.add(dx, dy, dz);
                        if (w.isAir(p) && AbstractFireBlock.canPlaceAt(w, p, net.minecraft.util.math.Direction.UP)) {
                            w.setBlockState(p, AbstractFireBlock.getState(w, p));
                            break;
                        }
                    }
                }
            }
            for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(2.5))) e.setOnFireFor(8);
            w.spawnParticles(ParticleTypes.FLAME, at.x, at.y + 0.3, at.z, 30, 1, 0.3, 1, 0.05);
            Noise.emit(w, at, 24);
        } else {                                                             // pipe bomb
            if (hit instanceof EntityHitResult eh) eh.getEntity().damage(w.getDamageSources().thrown(this, getOwner()), 4f);
            w.createExplosion(this, at.x, at.y, at.z, 3f, World.ExplosionSourceType.NONE);
            Noise.emit(w, at, 96);
        }
        discard();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Beeping", beeping);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        beeping = nbt.contains("Beeping") ? nbt.getInt("Beeping") : -1;
    }
}
