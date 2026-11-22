package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Ctrl action menu (Project Zomboid style). The client lists what can be done with whatever you're looking at
 * and with your own gear; picking an entry sends it here. Longer actions take time: a progress bar fills while you
 * stand still, and moving or getting hurt cancels them.
 */
public final class Actions {
    public enum Type {
        USE(0), LOOT_ALL(30), SMASH(8), FILL_BOTTLE(20), ENTER(0), SEARCH_VEHICLE(70), SHOVE(0),
        RELOAD(0), USE_ITEM(-1), OPEN_BACKPACK(0), LISTEN(30), SALVAGE_VEHICLE(100), SALVAGE_PROP(60), RIP_CLOTHES(30),
        BARRICADE(60), BARRICADE_METAL(100), UNBARRICADE(50);

        public final int ticks;

        Type(int ticks) {
            this.ticks = ticks;
        }

        public String key() {
            return "action.outbreak." + name().toLowerCase();
        }
    }

    /** Vehicle Forge props that can be stripped for parts */
    public static final java.util.Set<String> ELECTRONIC_PROPS = java.util.Set.of("computer", "tv", "vending_machine", "atm", "ac_unit", "electric_light");
    public static final java.util.Set<String> METAL_PROPS = java.util.Set.of("filing_cabinet", "mailbox", "newspaper_box", "parking_meter", "trash_bin",
            "fridge", "stove", "store_shelf", "checkout_counter", "stop_sign", "water_cooler", "fire_hydrant", "bus_stop_sign");

    public static boolean isTool(ItemStack s) {
        Item i = s.getItem();
        return i == OutbreakItems.CROWBAR || i == OutbreakItems.PIPE_WRENCH || i == OutbreakItems.CLAW_HAMMER || i == OutbreakItems.SLEDGEHAMMER
                || i == OutbreakItems.FIRE_AXE;
    }

    public static boolean isRaggable(ItemStack s) {
        Item i = s.getItem();
        return i == OutbreakItems.HOODIE || i == OutbreakItems.JEANS || i == OutbreakItems.CARGO_PANTS || i == OutbreakItems.BEANIE
                || i == OutbreakItems.BASEBALL_CAP || i == OutbreakItems.HI_VIS_VEST;
    }

    /** a salvage tool in the inventory, or EMPTY */
    public static ItemStack tool(net.minecraft.entity.player.PlayerEntity p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (isTool(inv.getStack(i))) return inv.getStack(i);
        return ItemStack.EMPTY;
    }

    private static int count(net.minecraft.entity.player.PlayerEntity p, Item item) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item)) n += inv.getStack(i).getCount();
        return n;
    }

    private static int plankSlot(net.minecraft.entity.player.PlayerEntity p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (Barricades.isPlank(inv.getStack(i))) return i;
        return -1;
    }

    public static boolean canPry(net.minecraft.entity.player.PlayerEntity p) {
        return count(p, OutbreakItems.CLAW_HAMMER) > 0 || count(p, OutbreakItems.CROWBAR) > 0;
    }

    /** what's missing to barricade ("hammer", "planks", "sheet_metal" or "nails"), or null if you have it all */
    public static String barricadeMissing(net.minecraft.entity.player.PlayerEntity p, boolean metal) {
        if (count(p, OutbreakItems.CLAW_HAMMER) == 0) return "hammer";
        if (metal ? count(p, OutbreakItems.SHEET_METAL) == 0 : plankSlot(p) < 0) return metal ? "sheet_metal" : "planks";
        if (count(p, OutbreakItems.NAILS) < (metal ? Barricades.NAILS_PER_METAL : Barricades.NAILS_PER_BOARD)) return "nails";
        return null;
    }

    private static void take(ServerPlayerEntity p, Item item, int n) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size() && n > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isOf(item)) continue;
            int k = Math.min(n, s.getCount());
            s.decrement(k);
            n -= k;
        }
    }

    private static void wearHammer(ServerPlayerEntity p, Item first) {
        var inv = p.getInventory();
        for (Item it : new Item[]{first, OutbreakItems.CLAW_HAMMER, OutbreakItems.CROWBAR}) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.isOf(it)) {
                    s.damage(1, p, pl -> pl.sendToolBreakStatus(Hand.MAIN_HAND));
                    return;
                }
            }
        }
    }

    /** start an action from outside the menu (sneak-using a hammer on a door or window) */
    public static void begin(ServerPlayerEntity p, Type t, BlockPos pos) {
        start(p, t, pos, -1, -1);
    }

    public static String propName(BlockState st) {
        net.minecraft.util.Identifier id = net.minecraft.registry.Registries.BLOCK.getId(st.getBlock());
        return id.getNamespace().equals("vehicleforge") ? id.getPath() : "";
    }

    private record Pending(Type type, BlockPos pos, int entity, int slot, long end, Vec3d start, float health) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private Actions() {}

    public static void receive(net.minecraft.server.MinecraftServer server, ServerPlayerEntity p, PacketByteBuf buf) {
        int ord = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        int entity = buf.readVarInt(), slot = buf.readVarInt();
        if (ord < 0 || ord >= Type.values().length) return;
        Type t = Type.values()[ord];
        server.execute(() -> start(p, t, pos, entity, slot));
    }

    private static void start(ServerPlayerEntity p, Type t, BlockPos pos, int entity, int slot) {
        if (p.isDead() || p.isSpectator()) return;
        if (!valid(p, t, pos, entity, slot)) return;
        if ((t == Type.SALVAGE_VEHICLE || t == Type.SALVAGE_PROP) && tool(p).isEmpty()) {
            tell(p, "msg.outbreak.need_tool");
            return;
        }
        if (t == Type.BARRICADE || t == Type.BARRICADE_METAL) {
            boolean metal = t == Type.BARRICADE_METAL;
            if (!(metal ? Barricades.canMetal(p.getWorld(), pos) : Barricades.canBoard(p.getWorld(), pos))) {
                tell(p, Barricades.isBarricade(p.getWorld(), pos) ? "msg.outbreak.barricade_full" : "msg.outbreak.cant_barricade");
                return;
            }
            String miss = barricadeMissing(p, metal);
            if (miss != null) {
                tell(p, "msg.outbreak.need_" + miss);
                return;
            }
            Noise.emit(p.getServerWorld(), Vec3d.ofCenter(pos), 20);                 // hammering is loud from the first nail
        }
        if (t == Type.UNBARRICADE) {
            if (!Barricades.isBarricade(p.getWorld(), pos)) return;
            if (!canPry(p)) {
                tell(p, "msg.outbreak.need_pry");
                return;
            }
        }
        int ticks = t == Type.USE_ITEM ? useTicks(p.getInventory().getStack(slot)) : t.ticks;
        cancel(p, false);
        if (ticks <= 0) {
            execute(p, t, pos, entity, slot);
            return;
        }
        PENDING.put(p.getUuid(), new Pending(t, pos, entity, slot, p.getWorld().getTime() + ticks, p.getPos(), p.getHealth()));
        String arg = t == Type.USE_ITEM ? p.getInventory().getStack(slot).getTranslationKey() : "";
        progress(p, ticks, t.key(), arg);
    }

    private static boolean valid(ServerPlayerEntity p, Type t, BlockPos pos, int entity, int slot) {
        switch (t) {
            case USE, LOOT_ALL, SMASH, FILL_BOTTLE, SALVAGE_PROP, BARRICADE, BARRICADE_METAL, UNBARRICADE -> {
                return p.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) < 49 && p.getWorld().isChunkLoaded(pos);
            }
            case ENTER, SEARCH_VEHICLE, SALVAGE_VEHICLE -> {
                Entity e = p.getWorld().getEntityById(entity);
                return e != null && e.isAlive() && e.squaredDistanceTo(p) < 64;
            }
            case USE_ITEM, OPEN_BACKPACK, RIP_CLOTHES -> {
                return slot >= 0 && slot < p.getInventory().size() && !p.getInventory().getStack(slot).isEmpty();
            }
            default -> {
                return true;
            }
        }
    }

    static int useTicks(ItemStack s) {
        if (s.isOf(OutbreakItems.FIRST_AID_KIT)) return 60;
        if (s.isOf(OutbreakItems.BANDAGE)) return 40;
        if (s.getItem() instanceof MedItem) return 20;
        return 32;
    }

    public static void tick(ServerWorld w) {
        if (PENDING.isEmpty()) return;
        for (ServerPlayerEntity p : w.getPlayers()) {
            Pending a = PENDING.get(p.getUuid());
            if (a == null) continue;
            if (p.isDead() || p.getPos().squaredDistanceTo(a.start) > 0.09 || p.getHealth() < a.health - 0.01f) {
                cancel(p, true);
                continue;
            }
            if (w.getTime() >= a.end) {
                PENDING.remove(p.getUuid());
                progress(p, 0, "", "");
                execute(p, a.type, a.pos, a.entity, a.slot);
            }
        }
    }

    public static void cancel(ServerPlayerEntity p, boolean tell) {
        if (PENDING.remove(p.getUuid()) != null) {
            progress(p, 0, "", "");
            if (tell) p.sendMessage(Text.translatable("msg.outbreak.cancelled").formatted(Formatting.GRAY), true);
        }
    }

    private static void progress(ServerPlayerEntity p, int ticks, String key, String arg) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(ticks);
        buf.writeString(key);
        buf.writeString(arg);
        ServerPlayNetworking.send(p, Outbreak.PROGRESS, buf);
    }

    private static void tell(ServerPlayerEntity p, String key, Object... args) {
        p.sendMessage(Text.translatable(key, args).formatted(Formatting.GRAY), true);
    }

    private static void execute(ServerPlayerEntity p, Type t, BlockPos pos, int entityId, int slot) {
        ServerWorld w = p.getServerWorld();
        switch (t) {
            case USE -> {
                BlockState st = w.getBlockState(pos);
                st.onUse(w, p, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
            }
            case LOOT_ALL -> lootAll(p, w, pos);
            case SMASH -> {
                BlockState st = w.getBlockState(pos);
                if (st.getSoundGroup() != BlockSoundGroup.GLASS || st.getHardness(w, pos) < 0 || st.getHardness(w, pos) >= 1) return;
                w.breakBlock(pos, false, p);
                Noise.emit(w, Vec3d.ofCenter(pos), 24);
                if (p.getMainHandStack().isEmpty()) {
                    p.damage(w.getDamageSources().generic(), 2f);
                    tell(p, "msg.outbreak.cut_hand");
                }
            }
            case FILL_BOTTLE -> {
                if (!w.getFluidState(pos).isIn(FluidTags.WATER)) return;
                var inv = p.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack s = inv.getStack(i);
                    if (s.isOf(Items.GLASS_BOTTLE)) {
                        s.decrement(1);
                        inv.offerOrDrop(new ItemStack(OutbreakItems.BOTTLED_WATER));
                        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1f, 1f);
                        return;
                    }
                }
                tell(p, "msg.outbreak.no_bottle");
            }
            case ENTER -> {
                Entity e = w.getEntityById(entityId);
                if (e != null) p.interact(e, Hand.MAIN_HAND);
            }
            case SEARCH_VEHICLE -> searchVehicle(p, w, w.getEntityById(entityId));
            case SHOVE -> Shove.shove(p);
            case RELOAD -> {
                ItemStack s = p.getMainHandStack();
                if (s.getItem() instanceof GunItem g && !g.reload(p, s)) tell(p, "msg.outbreak.no_ammo");
            }
            case USE_ITEM -> {
                var inv = p.getInventory();
                ItemStack s = inv.getStack(slot);
                Item item = s.getItem();
                if (!(item instanceof MedItem) && !s.isFood()) return;
                if (s.isFood() && !p.canConsume(s.getItem().getFoodComponent().isAlwaysEdible())) {
                    tell(p, "msg.outbreak.not_hungry");
                    return;
                }
                ItemStack result = item.finishUsing(s, w, p);
                if (result != s) {
                    if (s.isEmpty()) inv.setStack(slot, result);
                    else inv.offerOrDrop(result);
                }
            }
            case OPEN_BACKPACK -> {
                ItemStack s = p.getInventory().getStack(slot);
                if (s.getItem() instanceof BackpackItem) BackpackItem.open(p, s, slot);
            }
            case LISTEN -> p.sendMessage((Airport.at(w, p.getBlockPos()) ? Airport.report(w, p.getBlockPos())
                    : Population.of(w).describe(w, p.getBlockPos())).copy().formatted(Formatting.GRAY), true);
            case SALVAGE_VEHICLE -> salvageVehicle(p, w, w.getEntityById(entityId));
            case SALVAGE_PROP -> salvageProp(p, w, pos);
            case BARRICADE, BARRICADE_METAL -> barricade(p, w, pos, t == Type.BARRICADE_METAL);
            case UNBARRICADE -> {
                if (!canPry(p)) return;
                ItemStack back = Barricades.removeTop(w, pos);
                if (back.isEmpty()) return;
                p.getInventory().offerOrDrop(back);
                wearHammer(p, OutbreakItems.CROWBAR);
                w.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.4f, 1.6f);
                Noise.emit(w, Vec3d.ofCenter(pos), 12);
                if (Barricades.isBarricade(w, pos)) p.sendMessage(Barricades.describe(w, pos).copy().formatted(Formatting.GRAY), true);
                else tell(p, "msg.outbreak.barricade_removed");
            }
            case RIP_CLOTHES -> {
                ItemStack s = p.getInventory().getStack(slot);
                if (!isRaggable(s)) return;
                s.decrement(1);
                p.getInventory().offerOrDrop(new ItemStack(OutbreakItems.RAG, 3));
                w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 0.8f, 1.2f);
            }
        }
    }

    private static void barricade(ServerPlayerEntity p, ServerWorld w, BlockPos pos, boolean metal) {
        if (barricadeMissing(p, metal) != null) return;
        Direction side = Barricades.side(w, pos, p);
        boolean ok;
        if (metal) {
            ok = Barricades.addMetal(w, pos, side);
            if (ok) {
                take(p, OutbreakItems.SHEET_METAL, 1);
                take(p, OutbreakItems.NAILS, Barricades.NAILS_PER_METAL);
            }
        } else {
            int slot = plankSlot(p);
            ItemStack plank = p.getInventory().getStack(slot);
            ok = Barricades.addBoard(w, pos, side, net.minecraft.registry.Registries.ITEM.getId(plank.getItem()).toString());
            if (ok) {
                plank.decrement(1);
                take(p, OutbreakItems.NAILS, Barricades.NAILS_PER_BOARD);
            }
        }
        if (!ok) {
            tell(p, "msg.outbreak.cant_barricade");
            return;
        }
        wearHammer(p, OutbreakItems.CLAW_HAMMER);
        w.playSound(null, pos, metal ? SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                SoundCategory.PLAYERS, 0.5f, 1.5f);
        Noise.emit(w, Vec3d.ofCenter(pos), 20);
        p.sendMessage(Barricades.describe(w, pos).copy().formatted(Formatting.GRAY), true);
    }

    static Inventory inventoryAt(ServerWorld w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        if (st.getBlock() instanceof ChestBlock cb) {
            Inventory inv = ChestBlock.getInventory(cb, st, w, pos, true);
            if (inv != null) return inv;
        }
        BlockEntity be = w.getBlockEntity(pos);
        return be instanceof Inventory inv ? inv : null;
    }

    private static void lootAll(ServerPlayerEntity p, ServerWorld w, BlockPos pos) {
        Inventory inv = inventoryAt(w, pos);
        if (inv == null) return;
        int moved = 0;
        boolean left = false;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            ItemStack copy = s.copy();
            p.getInventory().insertStack(copy);
            int taken = s.getCount() - copy.getCount();
            if (taken > 0) {
                inv.removeStack(i, taken);
                moved += taken;
            }
            if (!copy.isEmpty()) left = true;
        }
        inv.markDirty();
        if (moved > 0) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1f);
        tell(p, moved == 0 && !left ? "msg.outbreak.empty" : left ? "msg.outbreak.inventory_full" : "msg.outbreak.took", moved);
    }

    private static void give(ServerPlayerEntity p, Item item, int n) {
        if (n > 0) p.getInventory().offerOrDrop(new ItemStack(item, n));
    }

    private static void wearTool(ServerPlayerEntity p) {
        ItemStack t = tool(p);
        if (!t.isEmpty()) t.damage(2, p, pl -> pl.sendToolBreakStatus(Hand.MAIN_HAND));
    }

    private static void salvageVehicle(ServerPlayerEntity p, ServerWorld w, Entity e) {
        if (e == null) return;
        if (e.getCommandTags().contains("outbreak_salvaged")) {
            tell(p, "msg.outbreak.stripped");
            return;
        }
        e.addCommandTag("outbreak_salvaged");
        var r = w.random;
        give(p, OutbreakItems.SCRAP_METAL, 2 + r.nextInt(4));
        if (r.nextFloat() < 0.35f) give(p, OutbreakItems.CAR_BATTERY, 1);
        if (r.nextFloat() < 0.3f) give(p, OutbreakItems.ELECTRONICS, 1 + r.nextInt(2));
        if (r.nextFloat() < 0.3f) give(p, OutbreakItems.RAG, 1 + r.nextInt(2));
        if (r.nextFloat() < 0.2f) give(p, OutbreakItems.DUCT_TAPE, 1);
        if (r.nextFloat() < 0.3f) give(p, OutbreakItems.SHEET_METAL, 1);                 // a door panel or the hood
        wearTool(p);
        w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.5f, 1.5f);
        Noise.emit(w, e.getPos(), 20);
        tell(p, "msg.outbreak.salvaged");
    }

    private static void salvageProp(ServerPlayerEntity p, ServerWorld w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        String name = propName(st);
        boolean elec = ELECTRONIC_PROPS.contains(name), metal = METAL_PROPS.contains(name);
        if (!elec && !metal) return;
        var r = w.random;
        w.breakBlock(pos, false, p);
        if (elec) {
            give(p, OutbreakItems.ELECTRONICS, 1 + r.nextInt(2));
            give(p, OutbreakItems.SCRAP_METAL, r.nextInt(2));
        } else {
            give(p, OutbreakItems.SCRAP_METAL, 1 + r.nextInt(3));
            if (r.nextFloat() < 0.15f) give(p, OutbreakItems.NAILS, 2 + r.nextInt(5));
        }
        wearTool(p);
        w.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.4f, 1.6f);
        Noise.emit(w, Vec3d.ofCenter(pos), 16);
        tell(p, "msg.outbreak.salvaged");
    }

    private static void searchVehicle(ServerPlayerEntity p, ServerWorld w, Entity e) {
        if (e == null) return;
        if (e.getCommandTags().contains("outbreak_searched")) {
            tell(p, "msg.outbreak.vehicle_empty");
            return;
        }
        e.addCommandTag("outbreak_searched");
        LootTable table = w.getServer().getLootManager().getLootTable(Outbreak.id("gameplay/vehicle_search"));
        LootContextParameterSet params = new LootContextParameterSet.Builder(w)
                .add(LootContextParameters.ORIGIN, e.getPos())
                .add(LootContextParameters.THIS_ENTITY, p)
                .build(LootContextTypes.CHEST);
        int n = 0;
        for (ItemStack s : table.generateLoot(params)) {
            n += s.getCount();
            p.getInventory().offerOrDrop(s);
        }
        w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 0.5f, 1.4f);
        tell(p, n == 0 ? "msg.outbreak.vehicle_nothing" : "msg.outbreak.vehicle_found", n);
    }
}
