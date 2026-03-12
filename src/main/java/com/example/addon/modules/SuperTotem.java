package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .defaultValue(4.5)
        .min(1)
        .sliderMax(6)
        .build()
    );

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Ataca al jugador mas cercano.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = null;
        double closestDist = range.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity) || entity == mc.player || !entity.isAlive()) continue;
            if (!Friends.get().shouldAttack((PlayerEntity) entity)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist < closestDist) {
                closestDist = dist;
                target = entity;
            }
        }

        if (target != null && mc.player.getAttackCooldownProgress(0.5f) >= 0.9f) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}

