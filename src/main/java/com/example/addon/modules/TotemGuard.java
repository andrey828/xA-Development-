package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {

    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "TotemGuard", "Nofall pero mejorado.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Validación con los métodos correctos para 1.21 (mc.getNetworkHandler())
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // --- LÓGICA PERMANENTE ---
        // Enviamos el paquete en cada tick del juego, sin importar la altura.
        // OnGround = true, HorizontalCollision = false
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));

        // Opcional: Reseteamos la distancia del cliente constantemente para que el HUD no marque caída
        if (mc.player.fallDistance > 0) {
            mc.player.fallDistance = 0;
        }
    }
}

