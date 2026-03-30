package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;

public class AdvancedParticles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- OPCIONES DE ACTIVACIÓN ---
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
        .description("Si se desactiva, oculta las partículas nativas del juego (optimización).")
        .defaultValue(true)
        .build());

    // --- CONFIGURACIÓN VISUAL ---
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color-aura")
        .description("Color del anillo alrededor del jugador.")
        .defaultValue(new SettingColor(0, 255, 255))
        .visible(auraPlayer::get)
        .build());

    public AdvancedParticles() {
        super(Categories.VISUALS, "xParticules", "Control avanzado y visuales de partículas.");
    }

    private double ticks = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // 1. Lógica de RODEARE AL JUGADOR (Efecto de Anillo)
        if (auraPlayer.get()) {
            ticks += 0.2;
            double x = mc.player.getX() + Math.cos(ticks) * 0.8;
            double z = mc.player.getZ() + Math.sin(ticks) * 0.8;
            double y = mc.player.getY() + 0.1;

            Vector3f pColor = new Vector3f(color.get().r / 255f, color.get().g / 255f, color.get().b / 255f);
            mc.world.addParticle(new DustParticleEffect(pColor, 1.0f), x, y, z, 0, 0, 0);
        }

        // 2. Lógica de CRÍTICOS (Simulación manual si el jugador está atacando)
        if (extraCrits.get() && mc.player.isFallFlying() || (mc.player.fallDistance > 0 && !mc.player.isOnGround())) {
            // Genera partículas de crítico extra cuando el jugador está en el aire (condición de crit)
            mc.world.addParticle(ParticleTypes.CRIT, mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0.1, 0.1, 0.1);
        }
    }

    // 3. Lógica para TODAS LAS PARTÍCULAS
    // Este método intercepta cuando el juego intenta spawnear cualquier partícula
    @EventHandler
    private void onParticle(meteordevelopment.meteorclient.events.world.ParticleEvent event) {
        if (!allParticles.get()) {
            event.cancel(); // Cancela la aparición de partículas si la opción está desactivada
        }
    }
}
