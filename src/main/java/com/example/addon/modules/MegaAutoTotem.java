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

    // --- NUEVO MODO STRICT ---
    private final Setting<Boolean> strictMode = sgGeneral.add(new BoolSetting.Builder()
        .name("strict-mode")
        .description("Fuerza tótems en ambas manos siempre, ignorando vida y delay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder().name("health-limit").defaultValue(14).min(0).sliderMax(36).build());
    private final Setting<Boolean> hoverEquip = sgGeneral.add(new BoolSetting.Builder().name("hover-refill").defaultValue(true).build());

    private final Setting<Boolean> damagePrediction = sgSmart.add(new BoolSetting.Builder().name("damage-prediction").defaultValue(true).build());
    private final Setting<Boolean> holeModifier = sgSmart.add(new BoolSetting.Builder().name("hole-modifier").defaultValue(true).build());
    private final Setting<Double> obsidianHoleHealth = sgSmart.add(new DoubleSetting.Builder().name("obsidian-hole-health").defaultValue(10).visible(holeModifier::get).build());
    private final Setting<Double> bedrockHoleHealth = sgSmart.add(new DoubleSetting.Builder().name("bedrock-hole-health").defaultValue(6).visible(holeModifier::get).build());

    private final Setting<Boolean> doubleHand = sgDouble.add(new BoolSetting.Builder().name("double-totem").defaultValue(false).build());
    private final Setting<Double> criticalHealth = sgDouble.add(new DoubleSetting.Builder().name("critical-health").defaultValue(6).visible(doubleHand::get).build());

    private double lastHealth = 20;

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "MegaAutoTotem", "Protección de tótems avanzada para xA.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        FindItemResult totems = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totems.found()) return;

        // LÓGICA STRICT: Salto de todas las condiciones
        if (strictMode.get()) {
            ensureTotem(45, totems); // Mano izquierda
            ensureTotem(getHandSlot(), totems); // Mano derecha
            return; // Salimos para no procesar la lógica normal
        }

        // LÓGICA NORMAL (Si Strict está desactivado)
        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        double healthDiff = lastHealth - currentHealth;
        lastHealth = currentHealth;

        double currentThreshold = healthThreshold.get();
        if (holeModifier.get()) {
            if (PlayerUtils.isInHole(true)) currentThreshold = bedrockHoleHealth.get();
            else if (PlayerUtils.isInHole(false)) currentThreshold = obsidianHoleHealth.get();
        }

        if (damagePrediction.get() && healthDiff > 8) currentThreshold = 36;

        // Ejecución normal por salud
        if (currentHealth <= currentThreshold) {
            ensureTotem(45, totems);
        }
        
        if (doubleHand.get() && currentHealth <= criticalHealth.get()) {
            ensureTotem(getHandSlot(), totems);
        }

        if (hoverEquip.get() && mc.currentScreen instanceof InventoryScreen inv) {
            handleHover(inv);
        }
    }

    private int getHandSlot() {
        ItemStack mainHand = mc.player.getMainHandStack();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i) == mainHand) {
                return i;
            }
        }
        return mc.player.getInventory().selectedSlot; 
    }

    private void ensureTotem(int targetSlot, FindItemResult totems) {
        boolean isOffhand = (targetSlot == 45);
        
        // Verificamos si ya hay un tótem para no spamear paquetes innecesarios
        if (isOffhand && mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        if (!isOffhand && mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        if (isOffhand) InvUtils.move().from(totems.slot()).toOffhand();
        else InvUtils.move().from(totems.slot()).to(targetSlot);
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
