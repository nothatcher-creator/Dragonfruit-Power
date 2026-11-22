package com.nothatcher.outbreak;

import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Hitscan firearm. The magazine lives in the stack's NBT ("Ammo"); reload with R (or just pull the trigger on an
 * empty gun). Headshots do double damage, shots shatter glass, and every shot is loud: zombies within earshot
 * come to investigate.
 */
public class GunItem extends Item {
    public final String sound;
    public final int mag, interval, reloadTicks, pellets;
    public final float damage, spread, range, recoil, noise;
    public final boolean auto, scope;
    private final java.util.function.Supplier<Item> ammo;

    /** client-only: when this client's player may next fire (for recoil prediction) */
    private static long clientNext;

    public GunItem(Settings s, String sound, java.util.function.Supplier<Item> ammo, int mag, int interval, int reloadTicks,
                   int pellets, float damage, float spread, float range, float recoil, float noise, boolean auto, boolean scope) {
        super(s.maxCount(1));
        this.sound = sound;
        this.ammo = ammo;
        this.mag = mag;
        this.interval = interval;
        this.reloadTicks = reloadTicks;
        this.pellets = pellets;
        this.damage = damage;
        this.spread = spread;
        this.range = range;
        this.recoil = recoil;
        this.noise = noise;
        this.auto = auto;
        this.scope = scope;
    }

    public Item ammoItem() {
        return ammo.get();
    }

    public static int ammo(ItemStack st) {
        NbtCompound n = st.getNbt();
        return n == null ? 0 : n.getInt("Ammo");
    }

    public ItemStack loaded() {
        ItemStack s = new ItemStack(this);
        s.getOrCreateNbt().putInt("Ammo", mag);
        return s;
    }

    @Override
    public TypedActionResult<ItemStack> use(World w, PlayerEntity p, Hand hand) {
        ItemStack st = p.getStackInHand(hand);
        trigger(w, p, st);
        if (auto) {
            p.setCurrentHand(hand);
            return TypedActionResult.consume(st);
        }
        return TypedActionResult.success(st, false);
    }

    @Override
    public void usageTick(World w, LivingEntity user, ItemStack st, int remaining) {
        if (auto && user instanceof PlayerEntity p) trigger(w, p, st);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return auto ? 72000 : 0;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public boolean isItemBarVisible(ItemStack st) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack st) {
        return Math.round(13f * ammo(st) / mag);
    }

    @Override
    public int getItemBarColor(ItemStack st) {
        return ammo(st) == 0 ? 0xD03030 : 0xE8C060;
    }

    @Override
    public void appendTooltip(ItemStack st, @Nullable World w, List<Text> tip, TooltipContext ctx) {
        tip.add(Text.translatable("tooltip.outbreak.ammo", ammo(st), mag, Text.translatable(ammoItem().getTranslationKey())).formatted(Formatting.GRAY));
        tip.add(Text.translatable("tooltip.outbreak.reload").formatted(Formatting.DARK_GRAY));
        if (scope) tip.add(Text.translatable("tooltip.outbreak.scope").formatted(Formatting.DARK_GRAY));
    }

    private void trigger(World w, PlayerEntity p, ItemStack st) {
        if (w.isClient) {
            // recoil on the shooter's own client; the server does the actual shooting
            long now = w.getTime();
            if (now < clientNext || (ammo(st) <= 0 && !p.getAbilities().creativeMode)) return;
            clientNext = now + interval;
            float kick = recoil * (p.isSneaking() ? 0.6f : 1f);
            p.setPitch(MathHelper.clamp(p.getPitch() - kick, -90, 90));
            p.setYaw(p.getYaw() + (p.getRandom().nextFloat() - 0.5f) * kick * 0.5f);
            return;
        }
        ServerWorld sw = (ServerWorld) w;
        long now = w.getTime();
        NbtCompound n = st.getOrCreateNbt();
        if (n.getLong("Next") > now) return;
        boolean creative = p.getAbilities().creativeMode;
        int left = n.getInt("Ammo");
        if (left <= 0 && !creative) {
            if (!reload(p, st)) {
                w.playSound(null, p.getX(), p.getY(), p.getZ(), Outbreak.sound("empty_click"), SoundCategory.PLAYERS, 0.8f, 1f);
                n.putLong("Next", now + 10);
            }
            return;
        }
        if (!creative) n.putInt("Ammo", left - 1);
        n.putLong("Next", now + interval);
        shoot(sw, p);
    }

    /** load from the player's inventory; returns false if there was nothing to load */
    public boolean reload(PlayerEntity p, ItemStack st) {
        NbtCompound n = st.getOrCreateNbt();
        int have = n.getInt("Ammo"), need = mag - have;
        if (need <= 0) return false;
        int got = 0;
        if (p.getAbilities().creativeMode) got = need;
        else {
            var inv = p.getInventory();
            for (int i = 0; i < inv.size() && got < need; i++) {
                ItemStack s = inv.getStack(i);
                if (s.isOf(ammoItem())) {
                    int take = Math.min(need - got, s.getCount());
                    s.decrement(take);
                    got += take;
                }
            }
        }
        if (got == 0) return false;
        n.putInt("Ammo", have + got);
        n.putLong("Next", p.getWorld().getTime() + reloadTicks);
        p.getItemCooldownManager().set(this, reloadTicks);
        p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), Outbreak.sound("reload"), SoundCategory.PLAYERS, 0.9f, 1f);
        return true;
    }

    private void shoot(ServerWorld w, PlayerEntity p) {
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
        right = right.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : right.normalize();
        Vec3d muzzle = eye.add(look.multiply(0.9)).add(right.multiply(0.28)).add(0, -0.18, 0);

        w.playSound(null, p.getX(), p.getY(), p.getZ(), Outbreak.sound(sound), SoundCategory.PLAYERS, 4f, 0.94f + w.random.nextFloat() * 0.12f);
        w.spawnParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.01);
        w.spawnParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.05, 0.05, 0.05, 0.01);
        Noise.emit(w, p.getPos(), noise);

        boolean steady = p.isSneaking();
        int aim = Skills.level(p, Skills.Skill.AIMING);
        float sp = spread * (steady ? (scope ? 0.1f : 0.5f) : 1f) * (p.isSprinting() ? 1.8f : 1f) * (1f - 0.05f * aim);
        for (int i = 0; i < pellets; i++) {
            float pitch = p.getPitch() + (float) w.random.nextGaussian() * sp;
            float yaw = p.getYaw() + (float) w.random.nextGaussian() * sp;
            pellet(w, p, eye, muzzle, Vec3d.fromPolar(pitch, yaw));
        }
    }

    private void pellet(ServerWorld w, PlayerEntity p, Vec3d eye, Vec3d muzzle, Vec3d dir) {
        Vec3d end = eye.add(dir.multiply(range));
        BlockHitResult bh = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        Vec3d stop = bh.getType() == HitResult.Type.MISS ? end : bh.getPos();
        Box sweep = p.getBoundingBox().stretch(dir.multiply(range)).expand(1);
        EntityHitResult eh = ProjectileUtil.raycast(p, eye, stop, sweep,
                e -> !e.isSpectator() && e.canHit() && !p.isConnectedThroughVehicle(e), eye.squaredDistanceTo(stop));
        Vec3d hit = stop;
        if (eh != null) {
            Entity t = eh.getEntity();
            Optional<Vec3d> at = t.getBoundingBox().expand(0.3).raycast(eye, stop);
            hit = at.orElse(t.getBoundingBox().getCenter());
            float dmg = damage * (float) (1 - 0.35 * eye.distanceTo(hit) / range);
            boolean head = t instanceof LivingEntity le && hit.y > le.getEyeY() - 0.3;
            if (head) dmg *= 2f;
            dmg *= 1f + 0.04f * aim;
            if (t instanceof net.minecraft.entity.mob.HostileEntity && p instanceof net.minecraft.server.network.ServerPlayerEntity sp2)
                Skills.add(sp2, Skills.Skill.AIMING, head ? 3 : 1);
            t.timeUntilRegen = 0;
            t.damage(w.getDamageSources().playerAttack(p), dmg);
            if (t instanceof LivingEntity le) le.takeKnockback(pellets == 1 ? 0.25 : 0.06, -dir.x, -dir.z);
            w.spawnParticles(new DustParticleEffect(new Vector3f(0.45f, 0.02f, 0.02f), 1.2f), hit.x, hit.y, hit.z, head ? 10 : 5, 0.12, 0.12, 0.12, 0);
            if (head) w.playSound(null, hit.x, hit.y, hit.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.7f, 1.3f);
        } else if (bh.getType() == HitResult.Type.BLOCK) {
            BlockState st = w.getBlockState(bh.getBlockPos());
            if (st.getSoundGroup() == BlockSoundGroup.GLASS && st.getHardness(w, bh.getBlockPos()) >= 0 && st.getHardness(w, bh.getBlockPos()) < 1) {
                w.breakBlock(bh.getBlockPos(), false, p);
            } else {
                w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, st), hit.x, hit.y, hit.z, 6, 0.05, 0.05, 0.05, 0.1);
                w.spawnParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 1, 0, 0, 0, 0.01);
            }
        }
        // tracer
        double len = muzzle.distanceTo(hit);
        int dots = (int) Math.min(pellets > 1 ? 6 : 24, len / 1.6);
        DustParticleEffect tracer = new DustParticleEffect(new Vector3f(1f, 0.86f, 0.45f), 0.45f);
        for (int i = 1; i <= dots; i++) {
            Vec3d q = muzzle.lerp(hit, i / (double) (dots + 1));
            w.spawnParticles(tracer, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }
}
