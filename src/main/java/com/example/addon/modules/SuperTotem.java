package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class SuperTotem extends Module {

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "Mantiene siempre un tótem en la mano secundaria.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Si ya hay tótem en offhand no hacer nada
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        int totemSlot = findTotem();
        if (totemSlot == -1) return;

        int syncId = mc.player.currentScreenHandler.syncId;

        // coger tótem
        mc.interactionManager.clickSlot(syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);

        // poner en offhand (slot 45)
        mc.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, mc.player);

        // devolver item restante si quedó algo en cursor
        mc.interactionManager.clickSlot(syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private int findTotem() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                return i < 9 ? i + 36 : i; // conversión correcta de hotbar
            }
        }
        return -1;
    }
}
