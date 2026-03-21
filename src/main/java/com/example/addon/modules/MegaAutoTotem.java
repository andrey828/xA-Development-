package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.List;

public class MegaAutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSmart   = settings.createGroup("Smart Logic");
    private final SettingGroup sgDouble  = settings.createGroup("Double Hand");
    private final SettingGroup sgHotbar  = settings.createGroup("Hotbar Fill");
    private final SettingGroup sgFilter  = settings.createGroup("Item Filter");

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
    // Pone tótem tanto en offhand como en mainhand simultáneamente
    private final Setting<Boolean> doubleHand = sgDouble.add(new BoolSetting.Builder()
        .name("double-totem")
        .description("Pone tótem en AMBAS manos (offhand y mainhand).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> criticalHealth = sgDouble.add(new DoubleSetting.Builder()
        .name("critical-health")
        .description("Vida a la que se activa el doble tótem.")
        .defaultValue(6)
        .min(0)
        .sliderMax(36)
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

    // --- ITEM FILTER ---
    private final Setting<List<Item>> ignoredItems = sgFilter.add(new ItemListSetting.Builder()
        .name("ignored-items")
        .description("Items que NO serán reemplazados por tótems en la hotbar.")
        .defaultValue()
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
            if (doubleHand.get()) equipMainHand();
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

        // Offhand — siempre que la vida baje del umbral
        if (currentHealth <= threshold && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult fresh = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (fresh.found()) { InvUtils.move().from(fresh.slot()).toOffhand(); acted = true; }
        }

        // Mainhand — solo cuando la vida baja del umbral crítico
        if (doubleHand.get() && currentHealth <= criticalHealth.get()) {
            acted |= equipMainHand();
        }

        if (hotbarFill.get()) fillHotbar();

        if (acted) timer = totemDelay.get();

        if (mc.currentScreen instanceof InventoryScreen inv) handleHover(inv);
    }

    // ---------------------------------------------------------------
    // Offhand: mueve tótem al slot 45 (offhand slot de Minecraft)
    // ---------------------------------------------------------------
    private void equipOffhand() {
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        FindItemResult fresh = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (fresh.found()) InvUtils.move().from(fresh.slot()).toOffhand();
    }

    // ---------------------------------------------------------------
    // Mainhand: usa InvUtils.swap() para poner tótem en la mano
    // activa directamente, sin tocar selectedSlot.
    // Busca fuente en inventario principal (9-35) para no ciclar.
    // ---------------------------------------------------------------
    private boolean equipMainHand() {
        if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) return false;

        // Prioridad: buscar tótem en inventario principal
        FindItemResult source = InvUtils.find(
            stack -> stack.getItem() == Items.TOTEM_OF_UNDYING,
            9, 35
        );

        // Si no hay en el inventario, buscar en la hotbar (excluyendo slot activo)
        if (!source.found()) {
            source = InvUtils.find(Items.TOTEM_OF_UNDYING, 0, 8);
            // Verificar que no es el mismo stack que la mainhand para no hacer nada inútil
            if (!source.found()) return false;
            if (mc.player.getInventory().getStack(source.slot()) == mc.player.getMainHandStack()) return false;
        }

        // swap() mueve el item del slot al hand indicado (Hand.MAIN_HAND = mano activa)
        InvUtils.swap(source.slot(), Hand.MAIN_HAND);
        return true;
    }

    // ---------------------------------------------------------------
    // Rellena los primeros N slots de la hotbar con tótems,
    // respetando los items ignorados.
    // ---------------------------------------------------------------
    private void fillHotbar() {
        int count = hotbarSlots.get();
        for (int i = 0; i < count; i++) {
            Item slotItem = mc.player.getInventory().getStack(i).getItem();
            if (slotItem == Items.TOTEM_OF_UNDYING) continue;
            if (isIgnored(slotItem)) continue;

            FindItemResult source = InvUtils.find(
                stack -> stack.getItem() == Items.TOTEM_OF_UNDYING,
                9, 35
            );
            if (!source.found()) break;
            InvUtils.move().from(source.slot()).to(i);
        }
    }

    // ---------------------------------------------------------------
    // Comprueba si un item está en la lista de ignorados
    // ---------------------------------------------------------------
    private boolean isIgnored(Item item) {
        if (item == Items.AIR) return false;
        return ignoredItems.get().contains(item);
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
