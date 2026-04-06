package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;

public class AdvancedParticles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- CONFIGURACIÓN ---
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

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color-aura")
        .description("Color del anillo.")
        .defaultValue(new SettingColor(0, 255, 255))
        .visible(auraPlayer::get)
        .build());

    public AdvancedParticles() {
        super(AddonTemplate.VISUALS, "xParticles ", "Control avanzado y visuales de partículas.");
    }

    private double ticks = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.particleManager == null) return;

        // 1. AURA (Anillo)
        if (auraPlayer.get()) {
            ticks += 0.2;
            double x = mc.player.getX() + Math.cos(ticks) * 0.8;
            double z = mc.player.getZ() + Math.sin(ticks) * 0.8;
            double y = mc.player.getY() + 0.1;

            // FIX: Convertir SettingColor a un int (0xRRGGBB) para evitar error de Vector3f
            int r = color.get().r & 0xFF;
            int g = color.get().g & 0xFF;
            int b = color.get().b & 0xFF;
            int colorInt = (r << 16) | (g << 8) | b;

            DustParticleEffect effect = new DustParticleEffect(colorInt, 1.0f);
            
            // Usar mc.particleManager evita los métodos privados de ClientWorld
            mc.particleManager.addParticle(effect, x, y, z, 0.0, 0.0, 0.0);
        }

        // 2. CRÍTICOS
        if (extraCrits.get() && !mc.player.isOnGround() && mc.player.fallDistance > 0.1) {
            mc.particleManager.addParticle(ParticleTypes.CRIT, mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0.0, 0.1, 0.0);
        }
    }

    @EventHandler
    private void onParticle(ParticleEvent event) {
        if (!allParticles.get()) {
            event.cancel();
        }
    }
}
