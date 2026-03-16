package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class MegaAutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSmart = settings.createGroup("Smart Logic");
    private final SettingGroup sgDouble = settings.createGroup("Double Hand");

    // --- GENERAL ---
    private final Setting<Boolean> strictMode = sgGeneral.add(new BoolSetting.Builder()
        .name("strict-mode")
        .description("Velocidad máxima sin delay. Ignora configuración Smart.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("health-limit")
        .defaultValue(14)
        .min(0)
        .sliderMax(36)
        .visible(() -> !strictMode.get()) 
        .build()
    );

    // --- SMART LOGIC ---
    private final Setting<Integer> totemDelay = sgSmart.add(new IntSetting.Builder()
        .name("totem-delay")
        .description("Ticks de espera (Solo modo normal) para evitar kicks del servidor.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .visible(() -> !strictMode.get())
        .build()
    );

    private final Setting<Boolean> damagePrediction = sgSmart.add(new BoolSetting.Builder()
        .name("damage-prediction")
        .defaultValue(true)
        .visible(() -> !strictMode.get())
        .build()
    );

    // --- DOUBLE HAND ---
    private final Setting<Boolean> doubleHand = sgDouble.add(new BoolSetting.Builder()
        .name("double-totem")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> criticalHealth = sgDouble.add(new DoubleSetting.Builder()
        .name("critical-health")
        .defaultValue(6)
        .visible(() -> !strictMode.get())
        .build()
    );

    private double lastHealth = 20;
    private int timer = 0;

    public MegaAutoTotem() {
        // Nombre en la GUI: xTotem. Nombre del archivo: MegaAutoTotem.java
        super(AddonTemplate.CATEGORY, "xTotem", "Protección de tótems avanzada para xA.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        FindItemResult totems = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totems.found()) return;

        // --- MODO STRICT (MAX SPEED) ---
        if (strictMode.get()) {
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(totems.slot()).toOffhand();
            }
            if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(totems.slot()).to(getHandSlot());
            }
            return; 
        }

        // --- MODO SMART (CON DELAY) ---
        if (timer > 0) {
            timer--;
            return;
        }

        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        double healthDiff = lastHealth - currentHealth;
        lastHealth = currentHealth;

        double currentThreshold = healthThreshold.get();
        if (damagePrediction.get() && healthDiff > 8) currentThreshold = 36;

        boolean acted = false;

        if (currentHealth <= currentThreshold && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(totems.slot()).toOffhand();
            acted = true;
        }
        
        if (doubleHand.get() && currentHealth <= criticalHealth.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(totems.slot()).to(getHandSlot());
            acted = true;
        }

        if (acted) timer = totemDelay.get();

        // Refill al pasar el ratón por encima en el inventario
        if (mc.currentScreen instanceof InventoryScreen inv) {
            handleHover(inv);
        }
    }

    // Método para obtener el slot de la mano sin dar error de "private access"
    private int getHandSlot() {
        ItemStack mainHand = mc.player.getMainHandStack();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i) == mainHand) {
                return i;
            }
        }
        return 0; 
    }

    private void handleHover(InventoryScreen screen) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.getStack().getItem() == Items.TOTEM_OF_UNDYING && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 40, SlotActionType.SWAP, mc.player);
            }
        }
    }

    @Override
    public String getInfoString() {
        return String.valueOf(InvUtils.find(Items.TOTEM_OF_UNDYING).count());
    }
}
