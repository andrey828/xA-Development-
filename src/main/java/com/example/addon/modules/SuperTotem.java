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
        super(AddonTemplate.CATEGORY, "SuperTotem", "Totem que sigue tu mano.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // 1. Siempre asegurar el Offhand
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
        }

        // 2. Reponer en la mano actual (Usando 'Hacker' mode para el Build)
        if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            if (totem.found()) {
                try {
                    // Accedemos a la variable por su nombre oculto
                    // Esto evita el error de 'private access' en GitHub
                    Field field = PlayerInventory.class.getDeclaredField("selectedSlot");
                    field.setAccessible(true);
                    int slotActual = (int) field.get(mc.player.getInventory());
                    
                    InvUtils.move().from(totem.slot()).to(slotActual);
                } catch (Exception e) {
                    // Si algo falla, lo ponemos en el 0 por si acaso
                    InvUtils.move().from(totem.slot()).to(0);
                }
            }
        }
    }
}
