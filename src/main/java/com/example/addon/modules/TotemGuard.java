package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "TotemGuard", "Inmunidad total a UltraMace (Versión 1.20.x).");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            // Si ya dice que esta en el suelo, no lo tocamos para evitar lag
            if (packet.isOnGround()) return;

            PlayerMoveC2SPacket nuevo = null;
            
            // En 1.20+, los constructores piden: (..., onGround, horizontalCollision)
            // Ponemos true en ambos para anular el daño de la maza
            if (packet instanceof PlayerMoveC2SPacket.Full p) {
                nuevo = new PlayerMoveC2SPacket.Full(p.getX(0), p.getY(0), p.getZ(0), p.getYaw(0), p.getPitch(0), true, true);
            } else if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround p) {
                nuevo = new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(0), p.getY(0), p.getZ(0), true, true);
            } else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround p) {
                nuevo = new PlayerMoveC2SPacket.LookAndOnGround(p.getYaw(0), p.getPitch(0), true, true);
            } else if (packet instanceof PlayerMoveC2SPacket.OnGroundOnly p) {
                nuevo = new PlayerMoveC2SPacket.OnGroundOnly(true, true);
            }

            if (nuevo != null) {
                event.setCancelled(true);
                mc.player.networkHandler.sendPacket(nuevo);
            }
        }
    }
}

