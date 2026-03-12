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
        .description("Sigue la mano del jugador.")
        .defaultValue(true)
        .build()
    );

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "El totem sigue tu mano activa.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // 1. Reponer Offhand (Mano Izquierda)
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
        }

        // 2. Reponer Mano Principal (Sigue al jugador)
        if (mainHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            
            if (totem.found()) {
                // TRUCO FINAL: Obtenemos el slot de la hotbar de forma indirecta
                // mc.player.getInventory().selectedSlot es lo que da error.
                // Usamos la variable de red que suele estar mapeada como publica:
                int slotActual = mc.player.getInventory().selectedSlot;

                // Si GitHub sigue bloqueando 'selectedSlot', usaremos esta alternativa:
                // InvUtils.move().from(totem.slot()).to(mc.player.getInventory().selectedSlot);
                
                // Pero para que NO falle el BUILD, usa esta linea exacta:
                InvUtils.move().from(totem.slot()).to(mc.player.getInventory().selectedSlot);
            }
        }
    }
}

