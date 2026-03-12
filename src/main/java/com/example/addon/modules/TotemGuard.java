package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-distance")
        .description("Distancia de caída para activar el NoFall.")
        .defaultValue(2.5)
        .min(0.1)
        .sliderMax(10)
        .build()
    );

    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "TotemGuard", "No fall pero mejorado ");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.networkHandler == null) return;

        if (mc.player.fallDistance > fallDistance.get()) {
            // Cancelamos el daño de caída enviando el paquete 'OnGround'
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
            mc.player.fallDistance = 0;
        }
    }
}

