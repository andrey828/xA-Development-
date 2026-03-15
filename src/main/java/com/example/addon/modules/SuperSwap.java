package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class SuperSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> swapKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("tecla-cambio")
        .description("La tecla para cambiar entre Elytras y Pechera.")
        .defaultValue(Keybind.none())
        .build()
    );

    private boolean wasPressed = false;

    public SuperSwap() {
        super(AddonTemplate.CATEGORY, "SuperSwap", "Cambia rápidamente entre Elytras y Pechera con una tecla.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        boolean isPressed = swapKey.get().isPressed();
        if (isPressed && !wasPressed) {
            swapGear();
        }
        wasPressed = isPressed;
    }

    private void swapGear() {
        // Usamos getEquippedStack que es un método universal y no da errores de mapeo
        ItemStack currentChest = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        if (currentChest.getItem() == Items.ELYTRA) {
            // Buscamos las pecheras manualmente para evitar la clase conflictiva ArmorItem
            FindItemResult chestplate = InvUtils.find(itemStack -> {
                Item i = itemStack.getItem();
                return i == Items.NETHERITE_CHESTPLATE || 
                       i == Items.DIAMOND_CHESTPLATE || 
                       i == Items.IRON_CHESTPLATE || 
                       i == Items.GOLDEN_CHESTPLATE || 
                       i == Items.CHAINMAIL_CHESTPLATE || 
                       i == Items.LEATHER_CHESTPLATE;
            });
            
            if (chestplate.found()) {
                // El slot 6 corresponde a la pechera en el inventario del jugador
                InvUtils.move().from(chestplate.slot()).to(6);
            }
        } else {
            // Si llevamos pechera o no llevamos nada, buscamos las Elytras
            FindItemResult elytra = InvUtils.find(Items.ELYTRA);
            
            if (elytra.found()) {
                InvUtils.move().from(elytra.slot()).to(6);
            }
        }
    }
}
