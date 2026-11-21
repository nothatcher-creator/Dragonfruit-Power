package com.nothatcher.outbreak;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What's left of a zombie: it lies where it fell, looking like the zombie did (same skin and clothes), and holds
 * whatever the zombie would have dropped. Right-click to search it; sneak and hit it to drag it out of the way.
 * Corpses rot away after a while (outbreakCorpseMinutes), and an area only keeps so many (outbreakMaxCorpses).
 */
public class CorpseEntity extends Entity {
    private static final TrackedData<Optional<UUID>> WHO = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> BODY_YAW = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.FLOAT);
    /** what it was wearing and holding, for looks only (head, chest, legs, feet, main hand, off hand) */
    @SuppressWarnings("unchecked")
    private static final TrackedData<ItemStack>[] WORN = new TrackedData[6];
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    static {
        for (int i = 0; i < WORN.length; i++) WORN[i] = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    }

    private final SimpleInventory loot = new SimpleInventory(27) {
        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return isAlive() && player.squaredDistanceTo(CorpseEntity.this) < 64;
        }
    };
    private int rot;

    public CorpseEntity(EntityType<? extends CorpseEntity> type, World world) {
        super(type, world);
    }

    /** lay out the body of a zombie that just finished dying, with its drops */
    public static void of(ZombieEntity z, List<ItemStack> drops) {
        if (!(z.getWorld() instanceof ServerWorld w)) return;
        CorpseEntity c = new CorpseEntity(OutbreakItems.CORPSE, w);
        // the dying animation tips it onto its side, toward (cos yaw, sin yaw); centre the body there
        float yaw = z.bodyYaw;
        double reach = z.getHeight() * 0.47;
        double r = Math.toRadians(yaw);
        c.refreshPositionAndAngles(z.getX() + Math.cos(r) * reach, z.getY(), z.getZ() + Math.sin(r) * reach, yaw, 0);
        c.dataTracker.set(WHO, Optional.of(z.getUuid()));
        c.dataTracker.set(BABY, z.isBaby());
        c.dataTracker.set(BODY_YAW, yaw);
        for (int i = 0; i < SLOTS.length; i++) c.dataTracker.set(WORN[i], z.getEquippedStack(SLOTS[i]).copy());
        if (z.hasCustomName()) c.setCustomName(z.getCustomName());
        for (ItemStack s : drops) {
            ItemStack left = c.loot.addStack(s);
            if (!left.isEmpty()) ItemScatterer.spawn(w, z.getX(), z.getY(), z.getZ(), left);
        }
        thinOut(w, c.getPos());
        w.spawnEntity(c);
        if (w.random.nextFloat() < 0.7f) Blood.splash(w, c.getBlockPos(), OutbreakBlocks.BLOOD_POOL.getDefaultState(), 0);
    }

    /** an area keeps at most outbreakMaxCorpses bodies: the oldest go first */
    private static void thinOut(ServerWorld w, Vec3d at) {
        int max = w.getGameRules().getInt(Outbreak.MAX_CORPSES);
        List<CorpseEntity> near = w.getEntitiesByClass(CorpseEntity.class, new Box(at, at).expand(48), Entity::isAlive);
        if (near.size() < max) return;
        near.sort((a, b) -> Integer.compare(b.rot, a.rot));
        for (int i = 0; i <= near.size() - max; i++) near.get(i).discard();
    }

    // ---- looks

    public Optional<UUID> who() {
        return dataTracker.get(WHO);
    }

    public boolean baby() {
        return dataTracker.get(BABY);
    }

    public float bodyYaw() {
        return dataTracker.get(BODY_YAW);
    }

    public ItemStack worn(int i) {
        return dataTracker.get(WORN[i]);
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(WHO, Optional.empty());
        dataTracker.startTracking(BABY, false);
        dataTracker.startTracking(BODY_YAW, 0f);
        for (TrackedData<ItemStack> t : WORN) dataTracker.startTracking(t, ItemStack.EMPTY);
    }

    // ---- behaviour

    @Override
    public void tick() {
        super.tick();
        if (!isOnGround() || getVelocity().lengthSquared() > 1e-6) {        // settle onto the ground
            setVelocity(getVelocity().multiply(0.5, 1, 0.5).add(0, -0.04, 0));
            move(MovementType.SELF, getVelocity());
            if (isOnGround()) setVelocity(Vec3d.ZERO);
        }
        if (getWorld() instanceof ServerWorld w) {
            int minutes = w.getGameRules().getInt(Outbreak.CORPSE_MINUTES);
            if (minutes > 0 && ++rot > minutes * 1200) discard();           // rotted away
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (getWorld().isClient) return ActionResult.SUCCESS;
        Text title = hasCustomName() ? Text.translatable("container.outbreak.corpse_of", getCustomName()) : Text.translatable("container.outbreak.corpse");
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inv, loot), title));
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.5f, 0.7f);
        return ActionResult.CONSUME;
    }

    /** hits don't hurt a corpse; a sneaking player's hit drags it away (its things fall out) */
    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity p && p.isSneaking() && !getWorld().isClient) {
            ItemScatterer.spawn(getWorld(), this, loot);
            getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_WOOL_BREAK, SoundCategory.PLAYERS, 0.7f, 0.6f);
            p.sendMessage(Text.translatable("msg.outbreak.corpse_dragged").formatted(Formatting.GRAY), true);
            discard();
        }
        return true;
    }

    @Override
    public boolean canHit() {
        return !isRemoved();
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---- saving

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        rot = nbt.getInt("Rot");
        if (nbt.containsUuid("Who")) dataTracker.set(WHO, Optional.of(nbt.getUuid("Who")));
        dataTracker.set(BABY, nbt.getBoolean("Baby"));
        dataTracker.set(BODY_YAW, nbt.getFloat("BodyYaw"));
        NbtList worn = nbt.getList("Worn", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < Math.min(worn.size(), WORN.length); i++) dataTracker.set(WORN[i], ItemStack.fromNbt(worn.getCompound(i)));
        loot.readNbtList(nbt.getList("Loot", NbtElement.COMPOUND_TYPE));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Rot", rot);
        who().ifPresent(u -> nbt.putUuid("Who", u));
        nbt.putBoolean("Baby", baby());
        nbt.putFloat("BodyYaw", bodyYaw());
        NbtList worn = new NbtList();
        for (TrackedData<ItemStack> t : WORN) worn.add(dataTracker.get(t).writeNbt(new NbtCompound()));
        nbt.put("Worn", worn);
        nbt.put("Loot", loot.toNbtList());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
