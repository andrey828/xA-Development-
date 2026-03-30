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
import org.joml.Vector3f;

public class AdvancedParticles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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
        super(AddonTemplate.VISUALS, "xPartucules", "Control avanzado y visuales de partículas.");
    }

    private double ticks = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.particleManager == null) return;

        // 1. AURA DEL JUGADOR
        if (auraPlayer.get()) {
            ticks += 0.2;
            double x = mc.player.getX() + Math.cos(ticks) * 0.8;
            double z = mc.player.getZ() + Math.sin(ticks) * 0.8;
            double y = mc.player.getY() + 0.1;

            // Convertimos el color a Vector3f para el constructor de Dust
            Vector3f pColor = new Vector3f(color.get().r / 255f, color.get().g / 255f, color.get().b / 255f);
            DustParticleEffect effect = new DustParticleEffect(pColor, 1.0f);
            
            // Usamos el ParticleManager directamente, que es público y siempre funciona
            mc.particleManager.addParticle(effect, x, y, z, 0, 0, 0);
        }

        // 2. CRÍTICOS EXTRA
        if (extraCrits.get() && !mc.player.isOnGround() && mc.player.fallDistance > 0) {
            mc.particleManager.addParticle(ParticleTypes.CRIT, mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0, 0.1, 0);
        }
    }

    @EventHandler
    private void onParticle(ParticleEvent event) {
        if (!allParticles.get()) {
            event.cancel();
        }
    }
}
