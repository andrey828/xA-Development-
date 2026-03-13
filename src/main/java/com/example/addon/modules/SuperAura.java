package com.example.addon.modules;

import com.example.addon.AddonTemplate; // Volvemos a AddonTemplate
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("Range").defaultValue(150.0).range(1, 1000).sliderRange(1, 400).build());
    private final Setting<Integer> steps = sgGeneral.add(new IntSetting.Builder().name("Steps").defaultValue(8).range(1, 100).build());
    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder().name("Attack Delay").defaultValue(0).range(0, 20).build());
    private final Setting<Boolean> criticals = sgGeneral.add(new BoolSetting.Builder().name("Criticals").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("Rotate").defaultValue(true).build());
    private final Setting<ServerType> serverType = sgGeneral.add(new EnumSetting.Builder<ServerType>().name("Server Type").defaultValue(ServerType.Spigot).build());
    private final Setting<HandMode> handMode = sgGeneral.add(new EnumSetting.Builder<HandMode>().name("Hand").defaultValue(HandMode.Main).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch").defaultValue(false).build());

    private final Setting<Boolean> targetPlayers = sgTargets.add(new BoolSetting.Builder().name("Players").defaultValue(true).build());
    private final Setting<Boolean> targetMonsters = sgTargets.add(new BoolSetting.Builder().name("Monsters").defaultValue(true).build());
    private final Setting<Boolean> targetAnimals = sgTargets.add(new BoolSetting.Builder().name("Animals").defaultValue(false).build());
    private final Setting<Boolean> targetAmbients = sgTargets.add(new BoolSetting.Builder().name("Ambients").defaultValue(false).build());
    private final Setting<Boolean> targetArmorStands = sgTargets.add(new BoolSetting.Builder().name("Armor Stands").defaultValue(false).build());

    private int delayTimer = 0;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Advanced aura with infinite reach simulation and step-based teleport bypass.");
    }

    @Override
    public void onActivate() {
        delayTimer = attackDelay.get();
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
        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());

        if (criticals.get() && mc.player.isOnGround()) {
            sendPos(origin.x, origin.y + 0.0625101, origin.z, false); 
            sendPos(origin.x, origin.y + 0.0000001, origin.z, false); 
        }

        int stepsCount = steps.get();
        for (int i = 1; i <= stepsCount; i++) {
            double t = (double) i / stepsCount;
            double yPos = origin.y + (targetPos.y - origin.y) * t;
            if (serverType.get() == ServerType.Paper) yPos = targetPos.y + (i % 2 == 0 ? 0.01 : -0.01);
            
            sendPos(origin.x + (targetPos.x - origin.x) * t, yPos, origin.z + (targetPos.z - origin.z) * t, false);
        }

        Hand hand = handMode.get() == HandMode.Main ? Hand.MAIN_HAND : Hand.OFF_HAND;
        int oldSlot = getSelectedSlot();

        if (autoSwitch.get()) {
            int bestSlot = getBestWeaponSlot();
            if (bestSlot != -1 && bestSlot != oldSlot) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
            }
        }

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.swingHand(hand);

        if (autoSwitch.get()) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
        }

        for (int i = stepsCount - 1; i >= 0; i--) {
            double t = (double) i / stepsCount;
            double yPos = origin.y + (targetPos.y - origin.y) * t;
            if (serverType.get() == ServerType.Paper) yPos = origin.y + (i % 2 == 0 ? 0.01 : -0.01);
            
            sendPos(origin.x + (targetPos.x - origin.x) * t, yPos, origin.z + (targetPos.z - origin.z) * t, i == 0);
        }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision);
        ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(packet);
    }

    private int getSelectedSlot() {
        try {
            Field field = PlayerInventory.class.getDeclaredField("selectedSlot");
            field.setAccessible(true);
            return field.getInt(mc.player.getInventory());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == mc.player) return false;
        if (entity instanceof PlayerEntity) {
            if (!targetPlayers.get()) return false;
            if (Friends.get().isFriend((PlayerEntity) entity)) return false;
        }
        if (entity instanceof Monster && !targetMonsters.get()) return false;
        if (entity instanceof AnimalEntity && !targetAnimals.get()) return false;
        if (entity instanceof AmbientEntity && !targetAmbients.get()) return false;
        if (entity instanceof ArmorStandEntity && !targetArmorStands.get()) return false;
        return true;
    }

    private int getBestWeaponSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem().toString().contains("mace")) return i;
        }
        return -1;
    }

    public enum ServerType { Vanilla, Spigot, Paper, Spartan }
    public enum HandMode { Main, Off }
}
