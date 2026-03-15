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
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class SuperSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Configuració de la tecla per fer el canvi
    private final Setting<Keybind> swapKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("tecla-canvi")
        .description("La tecla que farà l'intercanvi entre Èlitres i Pitrera.")
        .defaultValue(Keybind.none())
        .build()
    );

    private boolean wasPressed = false;

    public SuperSwap() {
        super(AddonTemplate.CATEGORY, "SuperSwap", "Canvia ràpidament entre Èlitres i Pitrera amb una tecla.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // Comprovem si s'ha premut la tecla sense fer "spam"
        boolean isPressed = swapKey.get().isPressed();
        if (isPressed && !wasPressed) {
            swapGear();
        }
        wasPressed = isPressed;
    }

    private void swapGear() {
        // Obtenim el que portem posat actualment al tors (índex 2 de l'armadura)
        ItemStack currentChest = mc.player.getInventory().getArmorStack(2);

        if (currentChest.getItem() == Items.ELYTRA) {
            // Si portem Èlitres, busquem una pitrera a l'inventari
            FindItemResult chestplate = InvUtils.find(itemStack -> 
                itemStack.getItem() instanceof ArmorItem && 
                ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.CHEST
            );
            
            if (chestplate.found()) {
                // El slot 6 de l'inventari correspon al pit
                InvUtils.move().from(chestplate.slot()).to(6);
            }
        } else {
            // Si portem pitrera (o res), busquem les Èlitres
            FindItemResult elytra = InvUtils.find(Items.ELYTRA);
            
            if (elytra.found()) {
                InvUtils.move().from(elytra.slot()).to(6);
            }
        }
    }
}
