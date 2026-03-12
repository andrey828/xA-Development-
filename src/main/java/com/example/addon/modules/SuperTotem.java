package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class SuperTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("main-hand")
        .defaultValue(true)
        .build()
    );

    private int delay;

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "AutoTotem estable sin parpadeos.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.currentScreen != null) return;

        if (delay > 0) {
            delay--;
            return;
        }

        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) {
                InvUtils.move().from(totem.slot()).toOffhand();
                delay = 2; 
                return;
            }
        }

        if (mainHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(i -> i.getItem() == Items.TOTEM_OF_UNDYING && i != mc.player.getOffHandStack());
            
            if (totem.found()) {
                int handSlot = -1;
                ItemStack handStack = mc.player.getMainHandStack();
                
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i) == handStack) {
                        handSlot = i;
                        break;
                    }
                }

                if (handSlot != -1) {
                    InvUtils.move().from(totem.slot()).to(handSlot);
                    delay = 2;
                }
            }
        }
    }
}

