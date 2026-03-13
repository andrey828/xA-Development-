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
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("rango-infinito").defaultValue(400.0).min(1).sliderMax(1000).build());

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Aura con alcance de 400 bloques.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        Entity target = getTarget();
        if (target == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(target.getX(), target.getY(), target.getZ(), true, false));

        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            mc.player.swingHand(Hand.MAIN_HAND);
        });

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, false));
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

