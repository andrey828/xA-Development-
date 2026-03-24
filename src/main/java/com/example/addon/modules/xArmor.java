package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
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
        super(AddonTemplate.CATEGORY, "xArmor", "Defensa automática optimizada sin dependencias conflictivas.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        ChatUtils.info("xArmor" + Formatting.GRAY + ": Sistema iniciado.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timer > 0) {
            timer--;
            return;
        }

        // Procesamos los slots de armadura
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
        float bestScore = getScore(current);

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !isCorrectSlot(stack, slot)) continue;
            if (antiBreak.get() && isLowDurability(stack)) continue;

            float score = getScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            InvUtils.move().from(bestSlot).toArmor(slot.getEntitySlotId());
            return true;
        }

        return false;
    }

    private boolean isCorrectSlot(ItemStack stack, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) return stack.isIn(ItemTags.HEAD_ARMOR);
        if (slot == EquipmentSlot.CHEST) return stack.isIn(ItemTags.CHEST_ARMOR) || stack.getItem() == Items.ELYTRA;
        if (slot == EquipmentSlot.LEGS) return stack.isIn(ItemTags.LEG_ARMOR);
        if (slot == EquipmentSlot.FEET) return stack.isIn(ItemTags.FOOT_ARMOR);
        return false;
    }

    private float getScore(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        
        float score = 0;
        String name = stack.getItem().toString().toLowerCase();
        
        // Material Score
        if (name.contains("netherite")) score += 20;
        else if (name.contains("diamond")) score += 15;
        else if (name.contains("iron")) score += 10;
        else if (name.contains("chainmail")) score += 5;
        else if (name.contains("gold")) score += 2;
        
        // Elytra tiene prioridad si no hay pechera mejor
        if (stack.getItem() == Items.ELYTRA) score = 10;

        score += Utils.getEnchantmentLevel(stack, Enchantments.PROTECTION) * 1.5f;
        score += Utils.getEnchantmentLevel(stack, Enchantments.MENDING) * 1.0f;
        score += Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING) * 0.2f;

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
