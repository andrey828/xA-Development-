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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgMulti = settings.createGroup("Multi-Target");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(250.0).min(1.0).sliderMax(500.0).build());
    private final Setting<Integer> hitDelay = sgGeneral.add(new IntSetting.Builder().name("hit-delay").defaultValue(2).min(0).sliderMax(20).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    
    private final Setting<Boolean> tpBypass = sgGeneral.add(new BoolSetting.Builder().name("tp-bypass").defaultValue(true).build());
    private final Setting<Integer> steps = sgGeneral.add(new IntSetting.Builder().name("steps").defaultValue(30).min(1).sliderMax(100).visible(tpBypass::get).build());

    private final Setting<Boolean> targetPlayers = sgTargeting.add(new BoolSetting.Builder().name("players").defaultValue(true).build());
    private final Setting<Boolean> targetMonsters = sgTargeting.add(new BoolSetting.Builder().name("monsters").defaultValue(false).build());
    private final Setting<Boolean> targetAnimals = sgTargeting.add(new BoolSetting.Builder().name("animals").defaultValue(false).build());
    private final Setting<Boolean> ignoreFriends = sgTargeting.add(new BoolSetting.Builder().name("ignore-friends").defaultValue(true).build());

    private final Setting<Boolean> multiTarget = sgMulti.add(new BoolSetting.Builder().name("multi-target").defaultValue(true).build());
    private final Setting<Double> multiRange = sgMulti.add(new DoubleSetting.Builder().name("aoe-range").defaultValue(6.0).min(1.0).sliderMax(10.0).visible(multiTarget::get).build());

    private Entity primaryTarget;
    private int timer = 0;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "xAura", "Multi-Aura con exploit de trayectoria.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;
        if (timer > 0) timer--;

        primaryTarget = findPrimaryTarget();
        if (primaryTarget == null) return;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(primaryTarget), Rotations.getPitch(primaryTarget), this::executeAura);
        } else {
            executeAura();
        }
    }

    private void executeAura() {
        if (timer > 0) return;

        double startX = mc.player.getX();
        double startY = mc.player.getY();
        double startZ = mc.player.getZ();

        if (tpBypass.get() && mc.player.distanceTo(primaryTarget) > 5) {
            int stepCount = steps.get();
            
            for (int i = 1; i <= stepCount; i++) {
                double t = i / (double) stepCount;
                double x = startX + (primaryTarget.getX() - startX) * t;
                double y = startY + (primaryTarget.getY() - startY) * t;
                double z = startZ + (primaryTarget.getZ() - startZ) * t;

                // CORRECCIÓN: Añadido el 5to parámetro (false para horizontalCollision)
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));

                if (i == stepCount || multiTarget.get()) {
                    hitEntitiesAt(x, y, z);
                }
            }

            for (int i = stepCount - 1; i >= 0; i--) {
                double t = i / (double) stepCount;
                double x = startX + (primaryTarget.getX() - startX) * t;
                double y = startY + (primaryTarget.getY() - startY) * t;
                double z = startZ + (primaryTarget.getZ() - startZ) * t;
                // CORRECCIÓN: Añadido el 5to parámetro
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
            }

            // Regreso a la posición real
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(startX, startY, startZ, true, false));
        } else {
            mc.interactionManager.attackEntity(mc.player, primaryTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        timer = hitDelay.get();
    }

    private void hitEntitiesAt(double x, double y, double z) {
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || !e.isAlive() || e == mc.player) continue;
            
            if (e instanceof PlayerEntity) {
                if (!targetPlayers.get() || (ignoreFriends.get() && Friends.get().isFriend((PlayerEntity) e))) continue;
            } else if (e instanceof Monster) {
                if (!targetMonsters.get()) continue;
            } else if (e instanceof AnimalEntity) {
                if (!targetAnimals.get()) continue;
            } else continue;

            if (e.squaredDistanceTo(x, y, z) <= (multiRange.get() * multiRange.get())) {
                mc.interactionManager.attackEntity(mc.player, e);
                mc.player.swingHand(Hand.MAIN_HAND);
                if (!multiTarget.get()) break;
            }
        }
    }

    private Entity findPrimaryTarget() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
            .filter(e -> e instanceof LivingEntity && e.isAlive() && e != mc.player)
            .filter(e -> mc.player.distanceTo(e) <= range.get())
            .filter(e -> (e instanceof PlayerEntity && targetPlayers.get()) || (e instanceof Monster && targetMonsters.get()) || (e instanceof AnimalEntity && targetAnimals.get()))
            .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
            .orElse(null);
    }

    @Override
    public String getInfoString() {
        return primaryTarget != null ? "xAura: " + primaryTarget.getName().getString() : null;
    }
}
