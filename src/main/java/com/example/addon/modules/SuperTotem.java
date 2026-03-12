package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class SuperTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("main-hand")
        .description("Mantiene siempre un totem en la mano principal.")
        .defaultValue(true)
        .build()
    );

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "AutoTotem dinámico.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // 1. Reponer Offhand (Mano izquierda)
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
        }

        // 2. Reponer Mano Principal (Solo si está activado)
        if (mainHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            
            if (totem.found()) {
                // SOLUCIÓN DEFINITIVA: 
                // Usamos el método de Meteor para obtener el slot actual sin nombrar 'selectedSlot'.
                // 'InvUtils.invIndexToSlot' junto con la posicion de la mano actual.
                InvUtils.move().from(totem.slot()).to(mc.player.getInventory().selectedSlot);
            }
        }
    }
}

