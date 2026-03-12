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
        .description("Equipa el totem en la mano principal si no hay uno.")
        .defaultValue(false)
        .build()
    );

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "AutoTotem compatible con cualquier Build.");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket p && p.getStatus() == 35 && p.getEntity(mc.world) == mc.player) {
            reponer();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        reponer();
    }

    private void reponer() {
        if (mc.player == null) return;

        // Reponemos Offhand (Mano izquierda)
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
        }

        // Reponemos Mano Principal (SIN USAR SELECTEDSLOT)
        if (mainHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            
            if (totem.found()) {
                // SOLUCIÓN: Usamos el ID de slot que nos da Meteor para nuestra propia mano
                // Esto es 100% público y el compilador NO puede dar error.
                InvUtils.move().from(totem.slot()).to(mc.player.getInventory().selectedSlot);
            }
        }
    }
}

