package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class MegaAutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSmart   = settings.createGroup("Smart Logic");
    private final SettingGroup sgDouble  = settings.createGroup("Double Hand");
    private final SettingGroup sgHotbar  = settings.createGroup("Hotbar Fill");

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
        .visible(() -> doubleHand.get() && !strictMode.get())
        .build()
    );

    // --- HOTBAR FILL ---
    private final Setting<Boolean> hotbarFill = sgHotbar.add(new BoolSetting.Builder()
        .name("hotbar-fill")
        .description("Rellena slots vacíos de la hotbar con tótems.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> hotbarSlots = sgHotbar.add(new IntSetting.Builder()
        .name("hotbar-slots")
        .description("Cuántos slots de la hotbar rellenar con tótems (desde slot 1).")
        .defaultValue(1)
        .min(1)
        .sliderMax(8)
        .visible(() -> hotbarFill.get())
        .build()
    );

    private double lastHealth = 20;
    private int timer = 0;

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "xTotem", "Protección de tótems avanzada para xA.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totem.found()) return;

        // --- MODO STRICT ---
        if (strictMode.get()) {
            equipOffhand();
            if (doubleHand.get()) equipMainhand();
            if (hotbarFill.get()) fillHotbar();
            return;
        }

        // --- MODO SMART ---
        if (timer > 0) { timer--; return; }

        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        double healthDiff    = lastHealth - currentHealth;
        lastHealth = currentHealth;

        double threshold = healthThreshold.get();
        if (damagePrediction.get() && healthDiff > 8) threshold = 36;

        boolean acted = false;

        if (currentHealth <= threshold && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult fresh = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (fresh.found()) { InvUtils.move().from(fresh.slot()).toOffhand(); acted = true; }
        }

        if (doubleHand.get() && currentHealth <= criticalHealth.get()) {
            acted |= equipMainhand();
        }

        if (hotbarFill.get()) fillHotbar();

        if (acted) timer = totemDelay.get();

        if (mc.currentScreen instanceof InventoryScreen inv) handleHover(inv);
    }

    // ---------------------------------------------------------------
    // Equipa offhand si no tiene ya un tótem
    // ---------------------------------------------------------------
    private void equipOffhand() {
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        FindItemResult fresh = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (fresh.found()) InvUtils.move().from(fresh.slot()).toOffhand();
    }

    // ---------------------------------------------------------------
    // Mueve un tótem al primer slot libre de la hotbar.
    // Fuente: inventario principal (slots 9-35) para no ciclar.
    // ---------------------------------------------------------------
    private boolean equipMainhand() {
        FindItemResult source = InvUtils.find(
            stack -> stack.getItem() == Items.TOTEM_OF_UNDYING,
            9, 35
        );
        if (!source.found()) return false;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(source.slot()).to(i);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Rellena los primeros N slots de la hotbar con tótems
    // ---------------------------------------------------------------
    private void fillHotbar() {
        int count = hotbarSlots.get();
        for (int i = 0; i < count; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) continue;

            FindItemResult source = InvUtils.find(
                stack -> stack.getItem() == Items.TOTEM_OF_UNDYING,
                9, 35
            );
            if (!source.found()) break;
            InvUtils.move().from(source.slot()).to(i);
        }
    }

    // ---------------------------------------------------------------
    // Refill desde pantalla de inventario al pasar el ratón
    // ---------------------------------------------------------------
    private void handleHover(InventoryScreen screen) {
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                mc.interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    slot.id, 40, SlotActionType.SWAP, mc.player
                );
                return;
            }
        }
    }

    @Override
    public String getInfoString() {
        return String.valueOf(InvUtils.find(Items.TOTEM_OF_UNDYING).count());
    }
}
