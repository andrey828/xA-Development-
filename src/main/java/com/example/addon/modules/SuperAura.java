package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("Range").defaultValue(100.0).range(1, 1000).sliderRange(1, 500).build());
    private final Setting<Integer> steps = sgGeneral.add(new IntSetting.Builder().name("Teleport Steps").defaultValue(5).range(1, 50).build());
    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder().name("Attack Delay").defaultValue(0).range(0, 20).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("Rotate").defaultValue(true).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch Mace").defaultValue(true).build());

    private final Setting<Boolean> targetPlayers = sgTargets.add(new BoolSetting.Builder().name("Players").defaultValue(true).build());
    private final Setting<Boolean> targetMonsters = sgTargets.add(new BoolSetting.Builder().name("Monsters").defaultValue(true).build());
    private final Setting<Boolean> targetAnimals = sgTargets.add(new BoolSetting.Builder().name("Animals").defaultValue(false).build());

    private int delayTimer = 0;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Infinite Reach KillAura para servidores de anarquía.");
    }

    @Override
    public void onActivate() {
        delayTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        for (Entity target : mc.world.getEntities()) {
            if (!isValid(target)) continue;
            if (mc.player.distanceTo(target) > range.get()) continue;

            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 10, () -> attackEntity(target));
            } else {
                attackEntity(target);
            }

            delayTimer = attackDelay.get();
            break; 
        }
    }

    private void attackEntity(Entity target) {
        Vec3d origin = mc.player.getPos();
        Vec3d targetPos = target.getPos();

        // 1. Cambiar a la maza si está disponible
        if (autoSwitch.get()) {
            int maceSlot = findMace();
            if (maceSlot != -1) mc.player.getInventory().selectedSlot = maceSlot;
        }

        // 2. Teletransporte instantáneo por pasos hacia el objetivo
        int stepsCount = steps.get();
        for (int i = 1; i <= stepsCount; i++) {
            double t = (double) i / stepsCount;
            sendPos(origin.x + (targetPos.x - origin.x) * t, origin.y + (targetPos.y - origin.y) * t, origin.z + (targetPos.z - origin.z) * t, false);
        }

        // 3. Ejecutar el ataque
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);

        // 4. Volver a la posición original para que no te quedes flotando
        for (int i = stepsCount - 1; i >= 0; i--) {
            double t = (double) i / stepsCount;
            sendPos(origin.x + (targetPos.x - origin.x) * t, origin.y + (targetPos.y - origin.y) * t, origin.z + (targetPos.z - origin.z) * t, i == 0);
        }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision));
    }

    private int findMace() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) return i;
        }
        return -1;
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == mc.player) return false;
        if (entity instanceof PlayerEntity) {
            if (!targetPlayers.get()) return false;
            if (Friends.get().isFriend((PlayerEntity) entity)) return false;
        }
        if (entity instanceof Monster && !targetMonsters.get()) return false;
        if (entity instanceof AnimalEntity && !targetAnimals.get()) return false;
        return true;
    }
}

