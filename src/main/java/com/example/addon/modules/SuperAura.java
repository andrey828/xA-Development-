package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    // --- Settings General (Igual a la foto) ---
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("Range").description("Rango de ataque.").defaultValue(5.0).min(1).sliderMax(20).build());
    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder().name("Attack Delay").description("Ticks entre ataques.").defaultValue(2).min(0).sliderMax(20).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("Rotate").description("Mirar al objetivo.").defaultValue(true).build());
    private final Setting<Boolean> autoSwitchMace = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch Mace").description("Cambia a la maza automáticamente.").defaultValue(true).build());

    // --- Settings Targets ---
    private final Setting<Boolean> players = sgTargets.add(new BoolSetting.Builder().name("Players").defaultValue(true).build());
    private final Setting<Boolean> monsters = sgTargets.add(new BoolSetting.Builder().name("Monsters").defaultValue(false).build());
    private final Setting<Boolean> animals = sgTargets.add(new BoolSetting.Builder().name("Animals").defaultValue(false).build());

    private int ticks;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Infinite Reach KillAura .");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;

        ticks++;
        if (ticks < attackDelay.get()) return;

        Entity target = findTarget();
        if (target == null) return;

        // Rotación
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> attack(target));
        else attack(target);

        ticks = 0;
    }

    private void attack(Entity target) {
        // Lógica de cambio a maza (Mace)
        if (autoSwitchMace.get()) {
            FindItemResult mace = InvUtils.findInHotbar(Items.MACE);
            if (mace.found()) InvUtils.swap(mace.slot(), false);
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private Entity findTarget() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive() || mc.player.distanceTo(entity) > range.get()) continue;
            if (players.get() && entity instanceof PlayerEntity) return entity;
            if (monsters.get() && entity instanceof Monster) return entity;
            if (animals.get() && entity instanceof AnimalEntity) return entity;
        }
        return null;
    }
}
