package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class MegaAutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("main-hand")
        .description("Coloca totems en la mano principal.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> offHand = sgGeneral.add(new BoolSetting.Builder()
        .name("off-hand")
        .description("Coloca totems en la mano secundaria.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> forceOnHit = sgGeneral.add(new BoolSetting.Builder()
        .name("force-on-hit")
        .description("Al recibir daño, fuerza un totem en ambas manos inmediatamente.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> fillHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("fill-hotbar")
        .description("Rellena slots vacíos del hotbar con totems del inventario.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> enableFilter = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-filter")
        .description("No reemplaza los ítems seleccionados en el filtro.")
        .defaultValue(false)
        .build());

    private final Setting<List<Item>> itemFilter = sgGeneral.add(new ItemListSetting.Builder()
        .name("item-filter")
        .description("Ítems que no serán reemplazados por totems.")
        .build());

    private final Setting<Boolean> disableOnGui = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-gui")
        .description("No reemplaza totems mientras tengas un GUI abierto.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> preferGapple = sgGeneral.add(new BoolSetting.Builder()
        .name("prefer-gapple-main")
        .description("Mantiene la golden apple en mano principal si la tienes.")
        .defaultValue(false)
        .build());

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "xTotem", "AutoTotem mejorado.");
    }

    @EventHandler(priority = 100)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player == null) return;
        if (!forceOnHit.get()) return;

        if (event.packet instanceof EntityDamageS2CPacket packet) {
            if (packet.entityId() == mc.player.getId()) {
                placeOffHand();
                placeMainHand(true);
            }
        }
    }

    @EventHandler(priority = 200)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (totem.count() <= 0) return;

        if (fillHotbar.get()) fillHotbarWithTotems();
        placeOffHand();
        placeMainHand(false);
    }

    private void placeOffHand() {
        if (!offHand.get() || mc.player == null) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
    }

    private void placeMainHand(boolean force) {
        if (!mainHand.get() || mc.player == null) return;

        ItemStack mainStack = mc.player.getMainHandStack();
        Item mainItem = mainStack.getItem();

        if (mainItem == Items.TOTEM_OF_UNDYING) return;
        if (!force && preferGapple.get() && mainItem == Items.ENCHANTED_GOLDEN_APPLE) return;
        if (filterCheck(mainItem, force)) return;

        FindItemResult hotbarTotem = InvUtils.find(stack ->
            stack.isOf(Items.TOTEM_OF_UNDYING) &&
            mc.player.getInventory().getSlotWithStack(stack) < PlayerInventory.getHotbarSize()
        );

        if (hotbarTotem.found()) InvUtils.swap(hotbarTotem.slot(), false);
    }

    private void fillHotbarWithTotems() {
        if (mc.player == null) return;
        if (disableOnGui.get() && mc.currentScreen != null) return;
        if (mc.interactionManager == null) return;

        PlayerInventory inv = mc.player.getInventory();

        for (int hotbarSlot = 0; hotbarSlot < PlayerInventory.getHotbarSize(); hotbarSlot++) {
            if (!inv.getStack(hotbarSlot).isEmpty()) continue;

            for (int slot = PlayerInventory.getHotbarSize(); slot < inv.main.size(); slot++) {
                if (inv.getStack(slot).isOf(Items.TOTEM_OF_UNDYING)) {
                    mc.interactionManager.clickSlot(
                        mc.player.playerScreenHandler.syncId,
                        slot, hotbarSlot,
                        SlotActionType.SWAP,
                        mc.player
                    );
                    return;
                }
            }
            return;
        }
    }

    private boolean filterCheck(Item item, boolean force) {
        if (force) return false;
        if (!enableFilter.get()) return false;
        return itemFilter.get().contains(item);
    }
}
