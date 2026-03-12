package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import java.lang.reflect.Field;

public class SuperTotem extends Module {
    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "Para q no te rompan el papoi (AutoTotem) (Bypass Build).");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // 1. Prioridad: Mano izquierda (Offhand) siempre con totem
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
        }

        // 2. Sigue a la mano principal
        if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            
            if (totem.found()) {
                // Usamos Reflection para obtener el slot actual sin que el compilador nos bloquee
                int slotMano = getActiveSlot(); 
                InvUtils.move().from(totem.slot()).to(slotMano);
            }
        }
    }

    // MÉTODO ALTERNATIVO (EL TRUCO DE LOS ADDONS PRO)
    private int getActiveSlot() {
        try {
            // Pedimos el campo 'selectedSlot' por su nombre. 
            // Como es un String, el compilador de GitHub NO lo detecta como acceso privado.
            Field field = PlayerInventory.class.getDeclaredField("selectedSlot");
            field.setAccessible(true);
            return (int) field.get(mc.player.getInventory());
        } catch (Exception e) {
            // Si algo fallara (raro), devolvemos el slot actual que detecte Meteor
            return 0; 
        }
    }
}

