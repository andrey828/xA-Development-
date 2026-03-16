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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Filtro de Objetivos");
    private final SettingGroup sgExploit = settings.createGroup("Exploit Config");

    // --- CONFIGURACIÓN GENERAL ---
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Distancia máxima de ataque (hasta 100 bloques).")
        .defaultValue(6.0).min(1).sliderMax(100.0).build()
    );

    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("attack-ms")
        .description("Milisegundos entre cada golpe.")
        .defaultValue(50).min(0).sliderMax(1000).build()
    );

    // --- FILTRO DE OBJETIVOS ---
    private final Setting<Boolean> targetPlayers = sgTargets.add(new BoolSetting.Builder().name("jugadores").defaultValue(true).build());
    private final Setting<Boolean> targetMonsters = sgTargets.add(new BoolSetting.Builder().name("monstruos").defaultValue(false).build());
    private final Setting<Boolean> targetAnimals = sgTargets.add(new BoolSetting.Builder().name("animales").defaultValue(false).build());
    private final Setting<Boolean> ignoreFriends = sgTargets.add(new BoolSetting.Builder().name("ignore-friends").defaultValue(true).build());

    // --- EXPLOIT ---
    private final Setting<Double> dashStep = sgExploit.add(new DoubleSetting.Builder()
        .name("dash-step")
        .description("Tamaño de los saltos de TP (Bypass).")
        .defaultValue(8.5).min(1).sliderMax(10.0).build()
    );

    private long lastAttackTime = 0;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "xAura optimizada: TP-Aura con selección de mobs y corrección de Ghost Hits.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (System.currentTimeMillis() - lastAttackTime < attackDelay.get()) return;

        Entity target = findTarget();
        if (target == null) return;

        // Posiciones
        Vec3d origin = mc.player.getPos();
        Vec3d targetPos = target.getPos();

        // 1. CREAR CAMINO HACIA EL ENEMIGO
        List<Vec3d> path = createPath(origin, targetPos);

        // 2. TELETRANSPORTE DE IDA (Forzar posición en el servidor)
        for (Vec3d step : path) {
            sendImmediatePos(step);
        }

        // 3. ATAQUE (Después de los paquetes de posición para evitar Ghost Hits)
        final Entity finalTarget = target;
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> {
            mc.interactionManager.attackEntity(mc.player, finalTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = System.currentTimeMillis();
        });

        // 4. TELETRANSPORTE DE VUELTA
        for (int i = path.size() - 1; i >= 0; i--) {
            sendImmediatePos(path.get(i));
        }
        
        // 5. VOLVER A LA POSICIÓN REAL
        sendImmediatePos(origin);
    }

    private Entity findTarget() {
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player || !e.isAlive()) continue;

            // Filtrado por tipo de mob
            if (e instanceof PlayerEntity) {
                if (!targetPlayers.get()) continue;
                if (ignoreFriends.get() && Friends.get().isFriend((PlayerEntity) e)) continue;
            } else if (e instanceof Monster) {
                if (!targetMonsters.get()) continue;
            } else if (e instanceof AnimalEntity) {
                if (!targetAnimals.get()) continue;
            } else {
                continue; // Si no es ninguno de los anteriores, no atacar
            }

            double dist = mc.player.distanceTo(e);
            if (dist <= range.get() && dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }
        return closest;
    }

    private List<Vec3d> createPath(Vec3d start, Vec3d end) {
        List<Vec3d> path = new ArrayList<>();
        double distance = start.distanceTo(end);
        double steps = Math.ceil(distance / dashStep.get());

        for (int i = 1; i <= steps; i++) {
            path.add(start.lerp(end, i / steps));
        }
        return path;
    }

    private void sendImmediatePos(Vec3d pos) {
        // Usamos el constructor de 5 argumentos para máxima compatibilidad con servidores de anarquía
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, true));
    }
}
