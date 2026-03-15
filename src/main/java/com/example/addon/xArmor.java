package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.List;

public class xArmor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-delay")
        .description("Retraso entre equipamiento (0 = instantáneo).")
        .defaultValue(0)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Quita la armadura si está a punto de romperse.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("Durabilidad mínima para quitarse la pieza.")
        .defaultValue(15)
        .min(2)
        .sliderMax(50)
        .visible(antiBreak::get)
        .build()
    );

    private int timer;

    public xArmor() {
        super(AddonTemplate.CATEGORY, "xArmor", "Equipa la mejor armadura automáticamente (Optimizado).");
    }

    @Override
    public void onActivate() {
        timer = 0;
        ChatUtils.info("xArmor" + Formatting.GRAY + ": Sistema de defensa activado.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timer > 0) {
            timer--;
            return;
        }

        // Revisamos los 4 slots de armadura
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (processSlot(slot)) {
                timer = delay.get();
                if (timer > 0) return; // Si hay delay, procesamos una pieza por tick
            }
        }
    }

    private boolean processSlot(EquipmentSlot slot) {
        ItemStack current = mc.player.getEquippedStack(slot);

        // Si tenemos activado anti-break y la pieza actual está muriendo, la quitamos
        if (antiBreak.get() && isLowDurability(current)) {
            return moveToEmpty(slot);
        }

        int bestSlot = -1;
        float bestScore = getScore(current);

        // Buscamos en el inventario (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) continue;
            if (armor.getSlotType() != slot) continue;
            if (antiBreak.get() && isLowDurability(stack)) continue;

            float score = getScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        // Si encontramos algo mejor, lo equipamos
        if (bestSlot != -1) {
            InvUtils.move().from(bestSlot).toArmor(slot.getEntitySlotId());
            return true;
        }

        return false;
    }

    private float getScore(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() == Items.ELYTRA) return 0;
        if (!(stack.getItem() instanceof ArmorItem armor)) return 0;

        float score = armor.getProtection();
        score += armor.getToughness() * 0.5f;

        // Lectura de componentes optimizada para Meteor 1.21+
        score += InvUtils.getEnchantmentLevel(stack, Enchantments.PROTECTION) * 1.5f;
        score += InvUtils.getEnchantmentLevel(stack, Enchantments.MENDING) * 1.0f;
        score += InvUtils.getEnchantmentLevel(stack, Enchantments.UNBREAKING) * 0.2f;

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

