package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.Actions;
import com.nothatcher.outbreak.BackpackItem;
import com.nothatcher.outbreak.GunItem;
import com.nothatcher.outbreak.MedItem;
import com.nothatcher.outbreak.Outbreak;
import com.nothatcher.outbreak.OutbreakItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** The Project Zomboid-style context menu: everything you can do with what you're looking at, and with your own gear. */
public class ActionMenuScreen extends Screen {
    record Entry(Text label, Actions.Type type, BlockPos pos, int entity, int slot) {}

    private final Text target;
    private final List<Entry> entries;
    private int panelX, panelY, panelW, panelH;

    private ActionMenuScreen(Text target, List<Entry> entries) {
        super(Text.translatable("action.outbreak.menu"));
        this.target = target;
        this.entries = entries;
    }

    public static void open(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        List<Entry> list = new ArrayList<>();
        Text target = build(mc, list);
        mc.setScreen(new ActionMenuScreen(target, list));
    }

    private static Text tr(String key, Object... args) {
        return Text.translatable("action.outbreak." + key, args);
    }

    private static Text build(MinecraftClient mc, List<Entry> out) {
        ClientPlayerEntity p = mc.player;
        CharacterClient.Info me = CharacterClient.mine();
        Text title = me == null ? p.getName() : Text.literal(me.name()).append(Text.literal("  ").append(Text.translatable("occupation.outbreak." + me.occupation())).formatted(net.minecraft.util.Formatting.GRAY));
        BlockPos none = BlockPos.ORIGIN;
        HitResult hr = mc.crosshairTarget;
        if (hr instanceof EntityHitResult eh) {
            Entity e = eh.getEntity();
            title = e.getDisplayName();
            var tid = Registries.ENTITY_TYPE.getId(e.getType());
            if (tid.getNamespace().equals("vehicleforge") && !tid.getPath().equals("seat")) {
                out.add(new Entry(tr("enter"), Actions.Type.ENTER, none, e.getId(), -1));
                out.add(new Entry(tr("search_vehicle"), Actions.Type.SEARCH_VEHICLE, none, e.getId(), -1));
                out.add(new Entry(tr(Actions.tool(p).isEmpty() ? "salvage_vehicle_no_tool" : "salvage_vehicle"), Actions.Type.SALVAGE_VEHICLE, none, e.getId(), -1));
            }
            if (e instanceof HostileEntity && e.squaredDistanceTo(p) < 9) out.add(new Entry(tr("shove"), Actions.Type.SHOVE, none, -1, -1));
        } else if (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            BlockState st = mc.world.getBlockState(pos);
            Block b = st.getBlock();
            title = b.getName();
            BlockEntity be = mc.world.getBlockEntity(pos);
            boolean openable = b instanceof DoorBlock || b instanceof TrapdoorBlock || b instanceof FenceGateBlock
                    || Registries.BLOCK.getId(b).getPath().equals("garage_door");
            if (openable && st.contains(Properties.OPEN)) {
                out.add(new Entry(tr(st.get(Properties.OPEN) ? "close" : "open"), Actions.Type.USE, pos, -1, -1));
            } else if (b instanceof AbstractFurnaceBlock) {
                out.add(new Entry(tr("cook"), Actions.Type.USE, pos, -1, -1));
                out.add(new Entry(tr("loot_all"), Actions.Type.LOOT_ALL, pos, -1, -1));
            } else if (be instanceof Inventory) {
                out.add(new Entry(tr("search"), Actions.Type.USE, pos, -1, -1));
                out.add(new Entry(tr("loot_all"), Actions.Type.LOOT_ALL, pos, -1, -1));
            } else if (b instanceof CraftingTableBlock) {
                out.add(new Entry(tr("craft"), Actions.Type.USE, pos, -1, -1));
            } else if (b instanceof BedBlock) {
                out.add(new Entry(tr("sleep"), Actions.Type.USE, pos, -1, -1));
            }
            String prop = Actions.propName(st);
            if (Actions.ELECTRONIC_PROPS.contains(prop) || Actions.METAL_PROPS.contains(prop)) {
                String key = Actions.ELECTRONIC_PROPS.contains(prop) ? "salvage_electronics" : "salvage_metal";
                out.add(new Entry(tr(Actions.tool(p).isEmpty() ? key + "_no_tool" : key), Actions.Type.SALVAGE_PROP, pos, -1, -1));
            }
            if (st.getSoundGroup() == BlockSoundGroup.GLASS && st.getLuminance() == 0) {
                float h = st.getHardness(mc.world, pos);
                if (h >= 0 && h < 1) out.add(new Entry(tr("smash"), Actions.Type.SMASH, pos, -1, -1));
            }
        }
        // barricades: board up a door, window or gap, add planks, or pry them off
        BlockPos bp = com.nothatcher.outbreak.Barricades.target(mc.world, p, 4.5);
        if (bp != null) {
            var w = mc.world;
            boolean barricaded = com.nothatcher.outbreak.Barricades.isBarricade(w, bp);
            if (barricaded) title = com.nothatcher.outbreak.Barricades.describe(w, bp);
            if (com.nothatcher.outbreak.Barricades.canBoard(w, bp)) {
                String miss = Actions.barricadeMissing(p, false);
                String key = barricaded ? "add_plank" : "board_up";
                out.add(new Entry(miss == null ? tr(key) : tr(key + "_need", Text.translatable("need.outbreak." + miss)), Actions.Type.BARRICADE, bp, -1, -1));
            }
            if (com.nothatcher.outbreak.Barricades.canMetal(w, bp) && has(p, OutbreakItems.SHEET_METAL) >= 0) {
                String miss = Actions.barricadeMissing(p, true);
                out.add(new Entry(miss == null ? tr("cover_metal") : tr("cover_metal_need", Text.translatable("need.outbreak." + miss)),
                        Actions.Type.BARRICADE_METAL, bp, -1, -1));
            }
            if (barricaded) {
                boolean metal = w.getBlockState(com.nothatcher.outbreak.Barricades.master(w, bp)).get(com.nothatcher.outbreak.BarricadeBlock.METAL);
                String key = metal ? "pry_metal" : "pry_plank";
                out.add(new Entry(tr(Actions.canPry(p) ? key : key + "_need"), Actions.Type.UNBARRICADE, bp, -1, -1));
            }
        }
        // your safehouse, and your character sheet
        if (!mc.world.isSkyVisible(p.getBlockPos().up())) {
            if (SkillsClient.inSafehouse(p.getBlockPos())) out.add(new Entry(tr("abandon_safehouse"), Actions.Type.ABANDON_SAFEHOUSE, BlockPos.ORIGIN, -1, -1));
            else out.add(new Entry(tr(SkillsClient.safehouse == null ? "claim_safehouse" : "move_safehouse"), Actions.Type.CLAIM_SAFEHOUSE, BlockPos.ORIGIN, -1, -1));
        }
        out.add(new Entry(tr("character_sheet"), null, BlockPos.ORIGIN, -1, -1));
        // water to fill a bottle from
        HitResult water = p.raycast(4.5, 1f, true);
        if (water instanceof BlockHitResult wh && water.getType() == HitResult.Type.BLOCK
                && mc.world.getFluidState(wh.getBlockPos()).isIn(FluidTags.WATER) && has(p, Items.GLASS_BOTTLE) >= 0) {
            out.add(new Entry(tr("fill_bottle"), Actions.Type.FILL_BOTTLE, wh.getBlockPos(), -1, -1));
        }
        // your own gear
        ItemStack hand = p.getMainHandStack();
        if (hand.getItem() instanceof GunItem g && GunItem.ammo(hand) < g.mag && has(p, g.ammoItem()) >= 0)
            out.add(new Entry(tr("reload"), Actions.Type.RELOAD, none, -1, -1));
        boolean hurt = p.getHealth() < p.getMaxHealth();
        if (hurt) addItem(p, out, OutbreakItems.BANDAGE, "apply");
        if (hurt) addItem(p, out, OutbreakItems.FIRST_AID_KIT, "apply");
        addItem(p, out, OutbreakItems.PAINKILLERS, "take");
        addItem(p, out, OutbreakItems.ANTIBIOTICS, "take");
        if (p.getHungerManager().isNotFull()) {
            var inv = p.getInventory();
            int shown = 0;
            List<net.minecraft.item.Item> seen = new ArrayList<>();
            for (int i = 0; i < inv.size() && shown < 3; i++) {
                ItemStack s = inv.getStack(i);
                if (!s.isFood() || s.getItem() instanceof MedItem || seen.contains(s.getItem())) continue;
                seen.add(s.getItem());
                out.add(new Entry(tr(s.isOf(OutbreakItems.BOTTLED_WATER) ? "drink" : "eat", s.getName()), Actions.Type.USE_ITEM, none, -1, i));
                shown++;
            }
        }
        var invr = p.getInventory();
        for (int i = 0; i < invr.size(); i++) {
            if (Actions.isRaggable(invr.getStack(i))) {
                out.add(new Entry(tr("rip_clothes", invr.getStack(i).getName()), Actions.Type.RIP_CLOTHES, none, -1, i));
                break;
            }
        }
        int bag = find(p, BackpackItem.class);
        if (bag >= 0) out.add(new Entry(tr("open_backpack"), Actions.Type.OPEN_BACKPACK, none, -1, bag));
        out.add(new Entry(tr("listen"), Actions.Type.LISTEN, none, -1, -1));
        return title;
    }

    private static void addItem(ClientPlayerEntity p, List<Entry> out, net.minecraft.item.Item item, String verb) {
        int slot = has(p, item);
        if (slot >= 0) out.add(new Entry(tr(verb, item.getName()), Actions.Type.USE_ITEM, BlockPos.ORIGIN, -1, slot));
    }

    private static int has(ClientPlayerEntity p, net.minecraft.item.Item item) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item)) return i;
        return -1;
    }

    private static int find(ClientPlayerEntity p, Class<?> cls) {
        var inv = p.getInventory();
        for (int i = 0; i < 36; i++) if (cls.isInstance(inv.getStack(i).getItem())) return i;
        return -1;
    }

    @Override
    protected void init() {
        int bw = 170, bh = 20, gap = 2;
        panelW = bw + 12;
        panelH = 22 + entries.size() * (bh + gap) + 4;
        panelX = Math.min(width - panelW - 4, width / 2 + 14);
        panelY = Math.max(4, Math.min(height - panelH - 4, height / 2 - panelH / 2));
        int y = panelY + 20;
        for (Entry e : entries) {
            addDrawableChild(ButtonWidget.builder(e.label(), btn -> pick(e)).dimensions(panelX + 6, y, bw, bh).build());
            y += bh + gap;
        }
    }

    private void pick(Entry e) {
        if (e.type() == null) {                                    // client-side: the character sheet
            if (client != null) client.setScreen(new SkillsScreen());
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(e.type().ordinal());
        buf.writeBlockPos(e.pos());
        buf.writeVarInt(e.entity());
        buf.writeVarInt(e.slot());
        ClientPlayNetworking.send(Outbreak.ACTION, buf);
        if (e.type() == Actions.Type.SHOVE && client != null && client.player != null) client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101010);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 16, 0xE0301818);
        ctx.drawText(textRenderer, target, panelX + 6, panelY + 4, 0xFFE8D8B0, true);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
