package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {

    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "xNoPoP", "Nofall but better");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (mc.player.fallDistance > 0 || mc.player.getVelocity().y < -0.1) {

            PlayerMoveC2SPacket.PositionAndOnGround packet =
                new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    true,
                    mc.player.horizontalCollision
                );

            ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);

            mc.getNetworkHandler().sendPacket(packet);

            mc.player.fallDistance = 0f;
        }
    }
}
