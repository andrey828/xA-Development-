package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.accessor.PlayerMoveC2SPacketAccessor;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {

    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "xNoPoP", "Cancels fall damage.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Detectar caída
        if (mc.player.fallDistance > 2 || mc.player.getVelocity().y < -0.1) {

            PlayerMoveC2SPacket.PositionAndOnGround packet =
                new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    true,
                    mc.player.horizontalCollision
                );

            // Usa TU mixin
            ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);

            mc.getNetworkHandler().sendPacket(packet);

            // Reset caída
            mc.player.fallDistance = 0f;
        }
    }
}
