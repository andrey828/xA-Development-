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

    // Cooldowns separados: tick normal vs paquete de emergencia
    private long lastTick = 0;
    private long lastPacket = 0;
    private static final long TICK_COOLDOWN = 50;    // 1 tick = ~50ms
    private static final long PACKET_COOLDOWN = 5;   // Mínimo entre paquetes

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "xTotem", "AutoTotem con Double Hand, Strict Mode y Hotbar Refill");
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTick < TICK_COOLDOWN) return;
        lastTick = now;

        // FIX: primero equipar, luego refill — así find() encuentra totems reales
        if (should()) {
            equip();
        }
        refillHotbar();
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive e) {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();
        if (now - lastPacket < PACKET_COOLDOWN) return;

        boolean shouldEquip = false;

        if (e.packet instanceof ExplosionS2CPacket) {
            shouldEquip = true;
        } else if (e.packet instanceof EntityStatusS2CPacket p) {
            if (p.getEntity(mc.world) == mc.player && p.getStatus() == 2) {
                shouldEquip = true;
            }
        }

        if (shouldEquip) {
            equip();
            lastPacket = now;
        }
    }

    private boolean should() {
        float h = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return strict.get() ? (h <= 12 || danger()) : (h <= hp.get().floatValue() || danger());
    }

    private void equip() {
        if (mc.player == null) return;

        // Offhand siempre tiene prioridad
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (!totem.found()) return;
            InvUtils.move().from(totem.slot()).toOffhand();
        }

        // FIX: Double Hand — busca totem fresco DESPUÉS de mover al offhand
        if (!strict.get() && doubleHand.get()) {
            if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                FindItemResult totem2 = InvUtils.find(Items.TOTEM_OF_UNDYING);
                if (!totem2.found()) return;

                int slot = mc.player.getInventory().selectedSlot;
                // Si el slot activo tiene algo que no es totem, busca slot vacío en hotbar
                if (!mc.player.getMainHandStack().isEmpty()) {
                    slot = getEmptyHotbarSlot();
                    if (slot == -1) return; // No hay slot libre, no forzar
                    InvUtils.swap(slot, false);
                }
                InvUtils.move().from(totem2.slot()).toHotbar(slot);
            }
        }
    }

    // FIX: refill solo rellena slots vacíos, sin mover el mismo totem dos veces
    private void refillHotbar() {
        if (mc.player == null) return;
        for (int i = 0; i < 9; i++) {
            if (!mc.player.getInventory().getStack(i).isEmpty()) continue;
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            // Solo mover si el totem no está ya en hotbar (slot >= 9)
            if (totem.found() && totem.slot() >= 9) {
                InvUtils.move().from(totem.slot()).toHotbar(i);
            }
        }
    }

    private int getEmptyHotbarSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    // FIX: usa AABB en lugar de iterar todas las entidades
    private boolean danger() {
        if (mc.player == null || mc.world == null) return false;

        // Cristales cercanos — búsqueda por caja, mucho más eficiente
        net.minecraft.util.math.Box box = mc.player.getBoundingBox().expand(8);
        for (Entity e : mc.world.getOtherEntities(mc.player, box)) {
            if (e instanceof EndCrystalEntity) return true;
        }

        // Respawn Anchors cercanos
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
        if (mc.player == null) return "0";
        return String.valueOf(InvUtils.find(Items.TOTEM_OF_UNDYING).count());
    }
}
