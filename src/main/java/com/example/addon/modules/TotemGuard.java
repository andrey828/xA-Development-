package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "totem-guard", "NoFall clasico por paquetes para anular daño.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        // Si el paquete es de movimiento, forzamos el estado 'onGround' a true
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            // Usamos un 'accessor' (IPlayerMoveC2SPacket) para cambiar el valor privado
            // Esto es lo que hace que el servidor crea que siempre estas pisando bloque
            ((IPlayerMoveC2SPacket) packet).setOnGround(true);
        }
    }
}

