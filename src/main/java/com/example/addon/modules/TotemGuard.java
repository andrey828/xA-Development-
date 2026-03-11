package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "TotemGuard", "Previene el retroceso de daño del SuperAura.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        // Interceptamos cualquier paquete de movimiento (incluidos los del Aura)
        if (event.packet instanceof PlayerMoveC2SPacket) {
            // Forzamos que el paquete diga que estás en el suelo
            // Esto resetea tu distancia de caída en el servidor a 0
            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
        }
    }
}

