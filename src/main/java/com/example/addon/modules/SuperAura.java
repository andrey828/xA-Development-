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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExploit = settings.createGroup("Exploit Config");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Distancia máxima de ataque (Default 6).")
        .defaultValue(6.0)
        .min(1)
        .sliderMax(100.0)
        .build()
    );

    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("attack-ms")
        .description("Milisegundos entre cada golpe.")
        .defaultValue(50)
        .min(0)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("No toca a tus amigos de Meteor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> dashStep = sgExploit.add(new DoubleSetting.Builder()
        .name("dash-step")
        .description("Tamaño de los saltos de TP (8.5 es el punto dulce).")
        .defaultValue(8.5)
        .min(1)
        .sliderMax(10.0)
        .build()
    );

    private long lastAttackTime = 0;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Infinite Aura nivel Dios con bypass de amigos y delay.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (System.currentTimeMillis() - lastAttackTime < attackDelay.get()) return;

        Entity target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player || !e.isAlive()) continue;
            
            if (ignoreFriends.get() && e instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) e)) continue;

            double dist = mc.player.distanceTo(e);
            if (dist <= range.get() && dist < closestDist) {
                closestDist = dist;
                target = e;
            }
        }

        if (target == null) return;

        // Arreglo de getPos() usando coordenadas directas si falla el símbolo
        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());

        List<Vec3d> path = createPath(origin, targetPos);

        for (Vec3d step : path) {
            sendPos(step);
        }

        final Entity finalTarget = target;
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> {
            mc.interactionManager.attackEntity(mc.player, finalTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = System.currentTimeMillis();
        });

        for (int i = path.size() - 1; i >= 0; i--) {
            sendPos(path.get(i));
        }
        
        sendPos(origin);
    }

    private List<Vec3d> createPath(Vec3d start, Vec3d end) {
        List<Vec3d> path = new ArrayList<>();
        double distance = start.distanceTo(end);
        double steps = Math.ceil(distance / dashStep.get());

        if (steps > 0) {
            for (int i = 1; i <= steps; i++) {
                path.add(start.lerp(end, i / steps));
            }
        }
        return path;
    }

    private void sendPos(Vec3d pos) {
        // Corrección del constructor: requiere x, y, z, onGround (true), y a veces un segundo boolean
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, true));
    }
}
