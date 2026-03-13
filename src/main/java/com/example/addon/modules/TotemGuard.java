package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "totem-guard", "NoFall directo que anula el daño de caída y mazas.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (event.packet instanceof PlayerMoveC2SPacket packet && !event.isCancelled()) {
            if (packet.isOnGround()) return;

            PlayerMoveC2SPacket nuevo = null;

            if (packet instanceof PlayerMoveC2SPacket.Full p) {
                nuevo = new PlayerMoveC2SPacket.Full(p.getX(0), p.getY(0), p.getZ(0), p.getYaw(0), p.getPitch(0), true, false);
            } 
            else if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround p) {
                nuevo = new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(0), p.getY(0), p.getZ(0), true, false);
            } 
            else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround p) {
                nuevo = new PlayerMoveC2SPacket.LookAndOnGround(p.getYaw(0), p.getPitch(0), true, false);
            } 
            else if (packet instanceof PlayerMoveC2SPacket.OnGroundOnly) {
                nuevo = new PlayerMoveC2SPacket.OnGroundOnly(true, false);
            }

            if (nuevo != null) {
                event.cancel();
                mc.getNetworkHandler().sendPacket(nuevo);
            }
        }
    }
}

