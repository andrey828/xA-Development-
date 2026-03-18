package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
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
import java.util.Comparator;
import java.util.stream.StreamSupport;

public class SuperAura extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(100.0).min(1.0).sliderMax(250.0).build());
    private final Setting<Integer> hitDelay = sgGeneral.add(new IntSetting.Builder().name("hit-delay").defaultValue(5).min(0).sliderMax(40).build());
    private final Setting<Double> blinkStep = sgGeneral.add(new DoubleSetting.Builder().name("blink-step").defaultValue(8.0).min(1.0).sliderMax(20.0).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder().name("entities").onlyAttackable().defaultValue(EntityType.PLAYER).build());

    private Entity primaryTarget;
    private int timer;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "xAura", "Infinite Reach KillAura.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;
        if (timer > 0) { timer--; return; }

        // BUSQUEDA MANUAL (Esto ignora el límite de 6 bloques de TargetUtils)
        primaryTarget = findTarget();

        if (primaryTarget == null) return;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(primaryTarget), Rotations.getPitch(primaryTarget), this::executeAura);
        } else {
            executeAura();
        }
    }

    private void executeAura() {
        Vec3d startPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(primaryTarget.getX(), primaryTarget.getY(), primaryTarget.getZ());
        double distance = startPos.distanceTo(targetPos);

        // Lógica de Teleport Bypass
        double step = blinkStep.get();
        int steps = (int) Math.ceil(distance / step);

        // Ida
        for (int i = 1; i <= steps; i++) {
            Vec3d nextStep = startPos.lerp(targetPos, (double) i / steps);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(nextStep.x, nextStep.y, nextStep.z, true, false));
        }

        // Ataque
        mc.interactionManager.attackEntity(mc.player, primaryTarget);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Vuelta
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(startPos.x, startPos.y, startPos.z, true, false));

        timer = hitDelay.get();
    }

    private Entity findTarget() {
        double rSq = range.get() * range.get();
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
            .filter(e -> e instanceof LivingEntity && e.isAlive() && e != mc.player)
            .filter(e -> entities.get().contains(e.getType()))
            .filter(e -> mc.player.squaredDistanceTo(e) <= rSq)
            .filter(e -> !(e instanceof PlayerEntity) || !Friends.get().isFriend((PlayerEntity) e))
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
            .orElse(null);
    }
}
