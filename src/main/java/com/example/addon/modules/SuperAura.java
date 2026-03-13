package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("rango-infinito")
        .description("Distancia masiva de ataque (Teleport Aura).")
        .defaultValue(400.0)
        .min(1)
        .sliderMax(1000)
        .build()
    );

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "super-aura", "Aura con alcance masivo mediante exploit de paquetes.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        // Buscamos al enemigo aunque esté lejísimos
        Entity target = getTarget();
        if (target == null) return;

        // Guardamos nuestra posición real para volver después del golpe
        double oldX = mc.player.getX();
        double oldY = mc.player.getY();
        double oldZ = mc.player.getZ();

        // 1. "Teletransportamos" al servidor hacia el enemigo enviando paquetes de posición
        // Esto engaña al servidor haciéndole creer que estamos al lado del enemigo
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(target.getX(), target.getY(), target.getZ(), true, true));

        // 2. Rotamos y Atacamos instantáneamente
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            mc.player.swingHand(Hand.MAIN_HAND);
        });

        // 3. Volvemos a nuestra posición real inmediatamente en el mismo tick
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(oldX, oldY, oldZ, true, true));
    }

    private Entity getTarget() {
        Entity closest = null;
        double closestDist = range.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity) || entity == mc.player || !entity.isAlive()) continue;
            if (!Friends.get().shouldAttack((PlayerEntity) entity)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist <= closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }
        return closest;
    }
}

