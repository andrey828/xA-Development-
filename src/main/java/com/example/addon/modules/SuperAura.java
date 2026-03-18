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

    // --- General Settings ---
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range").description("Rango máximo de detección y ataque.").defaultValue(50.0).min(1.0).sliderMax(200.0).build()
    );

    private final Setting<Integer> hitDelay = sgGeneral.add(new IntSetting.Builder()
        .name("hit-delay").description("Ticks de espera entre ataques.").defaultValue(10).min(0).sliderMax(40).build()
    );

    private final Setting<Double> blinkStep = sgGeneral.add(new DoubleSetting.Builder()
        .name("blink-step").description("Distancia máxima por paquete de movimiento (Bypass).").defaultValue(8.5).min(1.0).sliderMax(20.0).build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate").description("Mira hacia el objetivo antes de atacar.").defaultValue(true).build()
    );

    private final Setting<Boolean> tpBypass = sgGeneral.add(new BoolSetting.Builder()
        .name("tp-bypass").description("Usa teletransporte por paquetes para pegar desde lejos.").defaultValue(true).build()
    );

    // --- Targeting Settings ---
    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities").description("Tipos de entidades a atacar.").onlyAttackable().defaultValue(EntityType.PLAYER).build()
    );

    // --- Multi-Target Settings ---
    private final Setting<Boolean> multiTarget = sgMulti.add(new BoolSetting.Builder()
        .name("multi-target").description("Ataca a múltiples entidades alrededor del destino.").defaultValue(true).build()
    );

    private final Setting<Double> multiRange = sgMulti.add(new DoubleSetting.Builder()
        .name("aoe-range").description("Radio de daño en área al teletransportarse.").defaultValue(6.0).min(1.0).sliderMax(10.0).visible(multiTarget::get).build()
    );

    private final List<Entity> targets = new ArrayList<>();
    private Entity primaryTarget;
    private int timer;

    public SuperAura() {
        // Usa tu categoría de AddonTemplate
        super(AddonTemplate.CATEGORY, "xAura", "Optimized Infinite Multi-Aura para xA-Addon.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        primaryTarget = null;
        targets.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;

        if (timer > 0) timer--;

        // Usamos la utilidad de Meteor para encontrar el mejor objetivo
        targets.clear();
        TargetUtils.getList(targets, this::entityCheck, SortPriority.LowestDistance, 1);

        primaryTarget = targets.isEmpty() ? null : targets.get(0);

        if (primaryTarget == null) return;

        if (rotate.get()) {
            Rotations.rotate(
                Rotations.getYaw(primaryTarget),
                Rotations.getPitch(primaryTarget),
                this::executeAura
            );
        } else {
            executeAura();
        }
    }

    private void executeAura() {
        if (timer > 0 || primaryTarget == null) return;

        Vec3d startPos = mc.player.getPos();
        Vec3d targetPos = primaryTarget.getPos();
        double distance = startPos.distanceTo(targetPos);

        // Si tpBypass está activo y el enemigo está lejos (Infinite Aura)
        if (tpBypass.get() && distance > 4.0) {
            double step = blinkStep.get();
            int steps = (int) Math.ceil(distance / step);

            // Fase de Ida (Fragmentada)
            for (int i = 1; i <= steps; i++) {
                double ratio = (double) i / steps;
                Vec3d intermediatePos = startPos.lerp(targetPos, ratio);

                mc.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(
                        intermediatePos.x, intermediatePos.y, intermediatePos.z, true, false
                    )
                );

                // Pegar cuando llegamos al final del recorrido simulado
                if (i == steps) {
                    hitEntitiesAt(intermediatePos);
                }
            }

            // Vuelta inmediata al punto de origen en el mismo tick
            mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(
                    startPos.x, startPos.y, startPos.z, true, false
                )
            );

        } else {
            // Golpe Vanilla/Normal si está cerca
            mc.interactionManager.attackEntity(mc.player, primaryTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            if (multiTarget.get()) {
                 hitEntitiesAt(startPos); // Aplica el daño en área desde donde estamos
            }
        }

        timer = hitDelay.get();
    }

    private void hitEntitiesAt(Vec3d impactPos) {
        double rangeSq = multiRange.get() * multiRange.get();

        // Buscamos objetivos en el área de impacto simulada
        List<Entity> aoeTargets = new ArrayList<>();
        TargetUtils.getList(aoeTargets, e -> entityCheck(e) && e.squaredDistanceTo(impactPos) <= rangeSq, SortPriority.LowestDistance, 5);

        for (Entity e : aoeTargets) {
            if (e.equals(mc.player)) continue; // Evitar pegarse a sí mismo

            mc.interactionManager.attackEntity(mc.player, e);
            mc.player.swingHand(Hand.MAIN_HAND);

            if (!multiTarget.get()) break;
        }
    }

    private boolean entityCheck(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == mc.player) return false;
        if (!entities.get().contains(entity.getType())) return false;
        
        // Integración del Friend System oficial de Meteor
        if (entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) return false;
        
        return mc.player.distanceTo(entity) <= range.get();
    }

    @Override
    public String getInfoString() {
        return primaryTarget != null ? primaryTarget.getName().getString() : null;
    }
}
