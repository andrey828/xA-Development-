package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "TotemGuard", "Inmunidad total a UltraMace y caida.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            PlayerMoveC2SPacket nuevo = null;

            if (packet instanceof PlayerMoveC2SPacket.Full p) {
                nuevo = new PlayerMoveC2SPacket.Full(p.getX(mc.player.getX()), p.getY(mc.player.getY()), p.getZ(mc.player.getZ()), p.getYaw(mc.player.getYaw()), p.getPitch(mc.player.getPitch()), true);
            } else if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround p) {
                nuevo = new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(mc.player.getX()), p.getY(mc.player.getY()), p.getZ(mc.player.getZ()), true);
            } else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround p) {
                nuevo = new PlayerMoveC2SPacket.LookAndOnGround(p.getYaw(mc.player.getYaw()), p.getPitch(mc.player.getPitch()), true);
            } else if (packet instanceof PlayerMoveC2SPacket.OnGroundOnly p) {
                nuevo = new PlayerMoveC2SPacket.OnGroundOnly(true);
            }

            if (nuevo != null) {
                event.setCancelled(true);
                mc.player.networkHandler.sendInternalPacket(nuevo);
            }
        }
    }
}

