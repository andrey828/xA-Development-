package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Opción para limpiar efectos de estado negativos
    private final Setting<Boolean> antiBadEffects = sgGeneral.add(new BoolSetting.Builder()
        .name("Anti Bad Effects")
        .description("Quita automáticamente la lentitud y debilidad al caer.")
        .defaultValue(true)
        .build()
    );

    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "TotemGuard", "Protección total contra caídas y teletransporte.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // 1. PROTECCIÓN DE CAÍDA Y TELETRANSPORTE
        // Obtenemos la instancia del SuperAura de forma segura
        SuperAura aura = Modules.get().get(SuperAura.class);
        boolean auraActiva = aura != null && aura.isActive();
        
        // Detectamos caída o velocidad negativa
        // fallDistance > 0.5f detecta caídas normales
        // getVelocity().y < -0.1 detecta descensos rápidos (como bajar con maza)
        if (auraActiva || mc.player.fallDistance > 0.5f || mc.player.getVelocity().y < -0.1) {
            
            // Enviamos el paquete OnGroundOnly(true)
            // Esto le dice al servidor: "No importa lo que veas, mis pies tocan el suelo"
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
        }

        // 2. LIMPIEZA DE EFECTOS (Opcional)
        if (antiBadEffects.get()) {
            // Eliminamos efectos que suelen poner los servidores para castigar caídas
            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                mc.player.removeStatusEffect(StatusEffects.SLOWNESS);
            }
            if (mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
                mc.player.removeStatusEffect(StatusEffects.WEAKNESS);
            }
        }
    }
}

