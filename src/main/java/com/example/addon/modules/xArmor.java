package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.List;

public class xArmor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("swap-delay").defaultValue(0).min(0).sliderMax(5).build());
    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder().name("anti-break").defaultValue(false).build());
    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder().name("threshold").defaultValue(15).min(2).sliderMax(50).visible(antiBreak::get).build());

    private int timer;

    public xArmor() {
        super(AddonTemplate.CATEGORY, "xArmor", "Equipa armadura automáticamente (Optimizado).");
    }

    @Override
    public void onActivate() {
        timer = 0;
        ChatUtils.info("xArmor" + Formatting.GRAY + ": Defensa optimizada lista.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timer > 0) {
            timer--;
            return;
        }

        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (processSlot(slot)) {
                timer = delay.get();
                if (timer > 0) return;
            }
        }
    }

    private boolean processSlot(EquipmentSlot slot) {
        ItemStack current = mc.player.getEquippedStack(slot);

        if (antiBreak.get() && isLowDurability(current)) {
            return moveToEmpty(slot);
        }

        int bestSlot = -1;
        float bestScore = getScore(current, slot);

        for (int i = 0; i < 9; i++) { // Escaneamos Hotbar
            if (checkInventorySlot(i, slot, bestScore)) {
                bestScore = getScore(mc.player.getInventory().getStack(i), slot);
                bestSlot = i;
            }
        }
        
        for (int i = 9; i < 36; i++) { // Escaneamos Inventario
            if (checkInventorySlot(i, slot, bestScore)) {
                bestScore = getScore(mc.player.getInventory().getStack(i), slot);
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            InvUtils.move().from(bestSlot).toArmor(slot.getEntitySlotId());
            return true;
        }

        return false;
    }

    private boolean checkInventorySlot(int i, EquipmentSlot slot, float bestScore) {
        ItemStack stack = mc.player.getInventory().getStack(i);
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) return false;
        ArmorItem item = (ArmorItem) stack.getItem();
        if (item.getSlotType() != slot) return false;
        if (antiBreak.get() && isLowDurability(stack)) return false;
        
        return getScore(stack, slot) > bestScore;
    }

    private float getScore(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || stack.getItem() == Items.ELYTRA) return 0;
        if (!(stack.getItem() instanceof ArmorItem)) return 0;

        ArmorItem item = (ArmorItem) stack.getItem();
        float score = item.getProtection();
        score += item.getToughness() * 0.5f;

        // Usamos EnchantmentHelper para máxima compatibilidad entre versiones de Meteor/Minecraft
        score += EnchantmentHelper.getLevel(mc.world.getRegistryManager().getWrapperOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), stack) * 1.5f;
        score += EnchantmentHelper.getLevel(mc.world.getRegistryManager().getWrapperOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MENDING), stack) * 1.0f;
        score += EnchantmentHelper.getLevel(mc.world.getRegistryManager().getWrapperOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), stack) * 0.2f;

        return score;
    }

    private boolean isLowDurability(ItemStack stack) {
        if (!stack.isDamageable()) return false;
        return (stack.getMaxDamage() - stack.getDamage()) <= threshold.get();
    }

    private boolean moveToEmpty(EquipmentSlot slot) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                InvUtils.move().fromArmor(slot.getEntitySlotId()).to(i);
                return true;
            }
        }
        return false;
    }
}
