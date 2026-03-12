package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class SuperTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("main-hand")
        .description("Equipa totem en la mano derecha.")
        .defaultValue(false)
        .build()
    );

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "AutoTotem rapido y compatible.");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket p && p.getStatus() == 35) {
            if (p.getEntity(mc.world) == mc.player) reponer();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        reponer();
    }

    private void reponer() {
        if (mc.player == null) return;

        // MANO IZQUIERDA
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) {
                InvUtils.move().from(totem.slot()).toOffhand();
            }
        }

        // MANO DERECHA (CORRECCIÓN FINAL)
        if (mainHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            if (totem.found()) {
                // EXPLICACIÓN: Usamos to(mc.player.getInventory().selectedSlot) pero sin llamar a la variable privada.
                // Meteor tiene un acceso directo a través de InvUtils.
                InvUtils.move().from(totem.slot()).to(mc.player.getInventory().selectedSlot);
            }
        }
    }
}
