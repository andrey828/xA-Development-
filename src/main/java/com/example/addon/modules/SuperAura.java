package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SuperAura extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgMulti = settings.createGroup("Multi-Target");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(50.0).min(1.0).sliderMax(200.0).build());
    private final Setting<Integer> hitDelay = sgGeneral.add(new IntSetting.Builder().name("hit-delay").defaultValue(10).min(0).sliderMax(40).build());
    private final Setting<Double> blinkStep = sgGeneral.add(new DoubleSetting.Builder().name("blink-step").defaultValue(8.5).min(1.0).sliderMax(20.0).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> tpBypass = sgGeneral.add(new BoolSetting.Builder().name("tp-bypass").defaultValue(true).build());

    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder().name("entities").onlyAttackable().defaultValue(EntityType.PLAYER).build());
    private final Setting<Boolean> multiTarget = sgMulti.add(new BoolSetting.Builder().name("multi-target").defaultValue(true).build());
    private final Setting<Double> multiRange = sgMulti.add(new DoubleSetting.Builder().name("aoe-range").defaultValue(6.0).min(1.0).sliderMax(10.0).visible(multiTarget::get).build());

    private final List<Entity> targets = new ArrayList<>();
    private Entity primaryTarget;
    private int timer;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "xAura", " KillAura con teletransporte que golpea a distancias extremas. ");
    }

    @Override
    public void onActivate() {
        timer = 0;
        primaryTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;
        if (timer > 0) timer--;

        targets.clear();
        TargetUtils.getList(targets, this::entityCheck, SortPriority.LowestDistance, 1);
        primaryTarget = targets.isEmpty() ? null : targets.get(0);

        if (primaryTarget == null) return;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(primaryTarget), Rotations.getPitch(primaryTarget), this::executeAura);
        } else {
            executeAura();
        }
    }

    private void executeAura() {
        if (timer > 0 || primaryTarget == null) return;

        // --- FIX: Reemplazamos getPos() por creación manual de Vec3d ---
        Vec3d startPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(primaryTarget.getX(), primaryTarget.getY(), primaryTarget.getZ());
        
        double distance = startPos.distanceTo(targetPos);

        if (tpBypass.get() && distance > 4.0) {
            double step = blinkStep.get();
            int steps = (int) Math.ceil(distance / step);

            for (int i = 1; i <= steps; i++) {
                double ratio = (double) i / steps;
                Vec3d intermediatePos = startPos.lerp(targetPos, ratio);

                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        intermediatePos.x, intermediatePos.y, intermediatePos.z, true, false
                ));

                if (i == steps) hitEntitiesAt(intermediatePos);
            }

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    startPos.x, startPos.y, startPos.z, true, false
            ));
        } else {
            mc.interactionManager.attackEntity(mc.player, primaryTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (multiTarget.get()) hitEntitiesAt(startPos);
        }

        timer = hitDelay.get();
    }

    private void hitEntitiesAt(Vec3d impactPos) {
        double rangeSq = multiRange.get() * multiRange.get();
        List<Entity> aoeTargets = new ArrayList<>();
        TargetUtils.getList(aoeTargets, e -> entityCheck(e) && e.squaredDistanceTo(impactPos.x, impactPos.y, impactPos.z) <= rangeSq, SortPriority.LowestDistance, 5);

        for (Entity e : aoeTargets) {
            if (e.equals(mc.player)) continue;
            mc.interactionManager.attackEntity(mc.player, e);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (!multiTarget.get()) break;
        }
    }

    private boolean entityCheck(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == mc.player) return false;
        if (!entities.get().contains(entity.getType())) return false;
        if (entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) return false;
        return mc.player.distanceTo(entity) <= range.get();
    }
}
