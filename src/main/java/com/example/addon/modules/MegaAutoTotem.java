package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
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

    private final Setting<Boolean> strictMode = sgGeneral.add(new BoolSetting.Builder()
        .name("STRICT-MODE")
        .description("Prioridad máxima: Tótems en ambas manos siempre, sin delay.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder().name("health-limit").defaultValue(14).min(0).sliderMax(36).visible(() -> !strictMode.get()).build());
    private final Setting<Boolean> hoverEquip = sgGeneral.add(new BoolSetting.Builder().name("hover-refill").defaultValue(true).build());

    private final Setting<Boolean> damagePrediction = sgSmart.add(new BoolSetting.Builder().name("damage-prediction").defaultValue(true).build());
    private final Setting<Boolean> holeModifier = sgSmart.add(new BoolSetting.Builder().name("hole-modifier").defaultValue(true).visible(() -> !strictMode.get()).build());

    private final Setting<Boolean> doubleHand = sgDouble.add(new BoolSetting.Builder().name("double-totem").defaultValue(false).visible(() -> !strictMode.get()).build());
    private final Setting<Double> criticalHealth = sgDouble.add(new DoubleSetting.Builder().name("critical-health").defaultValue(6).visible(() -> doubleHand.get() && !strictMode.get()).build());

    private double lastHealth = 20;

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "MegaAutoTotem", "Protección de tótems avanzada para xA.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        double healthDiff = lastHealth - currentHealth;
        lastHealth = currentHealth;

        FindItemResult totems = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totems.found()) return;

        // --- LÓGICA STRICT MODE (Prioridad Absoluta) ---
        if (strictMode.get()) {
            // Equipar Offhand instantáneo
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(totems.slot()).toOffhand();
            }
            // Equipar Mainhand instantáneo (Slot actual de la hotbar)
            if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(totems.slot()).to(mc.player.getInventory().selectedSlot);
            }
            return; // En modo strict, no procesamos el resto para ahorrar ticks
        }

        // --- LÓGICA NORMAL / SMART ---
        double currentThreshold = healthThreshold.get();
        if (holeModifier.get()) {
            if (PlayerUtils.isInHole(true)) currentThreshold = 6; // Bedrock
            else if (PlayerUtils.isInHole(false)) currentThreshold = 10; // Obsidian
        }

        if (damagePrediction.get() && healthDiff > 8) currentThreshold = 36;

        // Equipar Offhand si baja la vida
        if (currentHealth <= currentThreshold) {
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(totems.slot()).toOffhand();
            }
        }
        
        // Doble Tótem convencional
        if (doubleHand.get() && currentHealth <= criticalHealth.get()) {
            if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(totems.slot()).to(mc.player.getInventory().selectedSlot);
            }
        }

        if (hoverEquip.get() && mc.currentScreen instanceof InventoryScreen inv) {
            handleHover(inv);
        }
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
