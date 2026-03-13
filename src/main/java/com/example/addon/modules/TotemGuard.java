package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "Totemguarld", "Anula daño de maza y caida absoluta.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // Resetea la distancia de caída en cada tick para que el cliente no crea que va a morir
        if (mc.player.fallDistance > 0) {
            mc.player.fallDistance = 0;
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        // Si el paquete es de movimiento, forzamos que el servidor crea que estamos tocando el suelo
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            ((IPlayerMoveC2SPacket) packet).meteor$setOnGround(true);
        }
    }
}
