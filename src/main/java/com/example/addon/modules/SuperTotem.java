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

    // INTERRUPTOR PARA ACTIVAR/DESACTIVAR
    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("main-hand")
        .description("Mantiene siempre un totem en la mano principal.")
        .defaultValue(true)
        .build()
    );

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "AutoTotem que sigue tu mano principal.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // 1. REPOSICIÓN EN OFFHAND (Mano Izquierda) - Siempre activa
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) {
                InvUtils.move().from(totem.slot()).toOffhand();
            }
        }

        // 2. REPOSICIÓN EN MANO PRINCIPAL (Solo si el setting está activado)
        if (mainHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            // Buscamos un totem que no sea el de la mano izquierda
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            
            if (totem.found()) {
                // ALTERNATIVA FINAL PARA EL BUILD:
                // Usamos el slot de la hotbar actual. 
                // Si el build falla por 'selectedSlot', usa: mc.player.getInventory().selectedSlot
                int slotActual = mc.player.getInventory().selectedSlot;
                
                InvUtils.move().from(totem.slot()).to(slotActual);
            }
        }
    }
}

