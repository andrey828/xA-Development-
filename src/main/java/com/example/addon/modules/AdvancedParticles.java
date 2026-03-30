package com.example.addon.modules;

import com.example.addon.AddonTemplate; // ¡Importación corregida!
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

public class AdvancedParticles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- OPCIONES ---
    private final Setting<Boolean> auraPlayer = sgGeneral.add(new BoolSetting.Builder()
        .name("aura-jugador")
        .description("Crea un anillo de partículas alrededor de ti.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> extraCrits = sgGeneral.add(new BoolSetting.Builder()
        .name("criticos-plus")
        .description("Multiplica las partículas de golpes críticos.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> allParticles = sgGeneral.add(new BoolSetting.Builder()
        .name("todas-las-particulas")
        .description("Si se desactiva, oculta las partículas nativas del juego.")
        .defaultValue(true)
        .build());

    // --- CONFIGURACIÓN DE COLOR ---
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color-aura")
        .description("Color del anillo alrededor del jugador.")
        .defaultValue(new SettingColor(0, 255, 255))
        .visible(auraPlayer::get)
        .build());

    public AdvancedParticles() {
        // Usamos AddonTemplate.VISUALS tal como lo tienes en tu proyecto
        super(AddonTemplate.VISUALS, "xPartucules", "Control avanzado y visuales de partículas.");
    }

    private double ticks = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // 1. Lógica de RODEAR AL JUGADOR
        if (auraPlayer.get()) {
            ticks += 0.2;
            double x = mc.player.getX() + Math.cos(ticks) * 0.8;
            double z = mc.player.getZ() + Math.sin(ticks) * 0.8;
            double y = mc.player.getY() + 0.1;

            // Empaquetamos el SettingColor en un Integer (RGB) para la API de Fabric actual
            int colorInt = ((color.get().r & 0xFF) << 16) | ((color.get().g & 0xFF) << 8) | (color.get().b & 0xFF);

            // Método con los 9 argumentos requeridos
            mc.world.addParticle(new DustParticleEffect(colorInt, 1.0f), true, true, x, y, z, 0.0, 0.0, 0.0);
        }

        // 2. Lógica de CRÍTICOS EXTRA
        if (extraCrits.get() && !mc.player.isOnGround() && mc.player.fallDistance > 0) {
            mc.world.addParticle(ParticleTypes.CRIT, true, true, mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0.1, 0.1, 0.1);
        }
    }

    // 3. CONTROL GLOBAL DE PARTÍCULAS
    @EventHandler
    private void onParticle(ParticleEvent event) {
        if (!allParticles.get()) {
            event.cancel();
        }
    }
}
