package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;

public class MegaAutoTotem extends Module {

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> strict = sg.add(new BoolSetting.Builder()
        .name("Strict Mode")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> doubleHand = sg.add(new BoolSetting.Builder()
        .name("Double Hand")
        .defaultValue(true)
        .visible(() -> !strict.get())
        .build());

    private final Setting<Double> hp = sg.add(new DoubleSetting.Builder()
        .name("HP Threshold")
        .defaultValue(10)
        .min(0)
        .sliderMax(36)
        .visible(() -> !strict.get())
        .build());

    private long last = 0;

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "xTotem", "AutoTotem con Double Hand, Strict Mode y Hotbar Refill");
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null) return;
        if (System.currentTimeMillis() - last < 10) return;

        refillHotbar();

        if (should()) {
            equip();
            last = System.currentTimeMillis();
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive e) {
        if (e.packet instanceof ExplosionS2CPacket) forceEquip();
        if (e.packet instanceof EntityStatusS2CPacket p) {
            if (p.getEntity(mc.world) == mc.player && p.getStatus() == 2) forceEquip();
        }
    }

    private boolean should() {
        float h = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return strict.get() ? (h <= 12 || danger()) : (h <= hp.get() || danger());
    }

    private void forceEquip() {
        equip();
    }

    private void equip() {
        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totem.found()) return;

        // Equip offhand always
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(totem.slot()).toOffhand();
        }

        // Equip mainhand if Double Hand active and Strict Mode allows
        if (!strict.get() && doubleHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            int emptySlot = getEmptyHotbarSlot();
            if (emptySlot != -1) {
                InvUtils.move().from(totem.slot()).toHotbar(emptySlot);
                InvUtils.swap(emptySlot, false);
            }
        }
    }

    // Refill empty hotbar slots
    private void refillHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
                if (totem.found()) {
                    InvUtils.move().from(totem.slot()).toHotbar(i);
                }
            }
        }
    }

    // Busca slot vacío en hotbar
    private int getEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private boolean danger() {
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity && mc.player.distanceTo(e) <= 8) return true;
        }

        BlockPos p = mc.player.getBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (mc.world.getBlockState(p.add(x, 0, z)).getBlock() == Blocks.RESPAWN_ANCHOR) return true;
            }
        }

        return false;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(InvUtils.find(Items.TOTEM_OF_UNDYING).count());
    }
}
