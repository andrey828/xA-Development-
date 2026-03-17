package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.PlayerMoveC2SPacketAccessor;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class xNoPoP extends Module {

    public xNoPoP() {
        super(AddonTemplate.CATEGORY, "xNoPoP", "Cancels all fall damage.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // si está cayendo
        if (mc.player.fallDistance > 2) {

            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                true,
                mc.player.horizontalCollision
            );

            // spoof onGround
            ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);

            mc.getNetworkHandler().sendPacket(packet);

            // reset fall distance
            mc.player.fallDistance = 0f;
        }
    }
}
