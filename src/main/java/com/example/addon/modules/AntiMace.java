package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

/**
 * AntiMace — Envía paquetes de movimiento vertical oscilante para
 * romper la mecánica de caída del xMace y el TP reach del xAura.
 *
 * El servidor calcula el daño del Mace según la distancia de caída acumulada.
 * Si tu posición reportada cambia continuamente arriba/abajo, esa distancia
 * se resetea o se vuelve inválida, anulando el golpe.
 * También interrumpe el hitbox predictivo del xAura.
 */
public class AntiMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> bounceHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("bounce-height")
        .description("Cuántos bloques sube/baja por paquete. Bajo = más seguro vs kicks.")
        .defaultValue(0.25)
        .min(0.05)
        .sliderMax(1.0)
        .build());

    private final Setting<Integer> packetsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("packets-per-tick")
        .description("Cuántos paquetes de oscilación enviar por tick.")
        .defaultValue(2)
        .min(1)
        .sliderMax(8)
        .build());

    private final Setting<Boolean> onlyWhenAttacked = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-attacked")
        .description("Solo activa la oscilación cuando hay un jugador cerca.")
        .defaultValue(false)
        .build());

    private final Setting<Double> activationRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("activation-range")
        .description("Rango para detectar jugadores cercanos (solo-when-attacked).")
        .defaultValue(20.0)
        .min(1.0)
        .sliderMax(100.0)
        .build());

    private final Setting<Boolean> randomize = sgGeneral.add(new BoolSetting.Builder()
        .name("randomize")
        .description("Añade variación aleatoria al bounce para evitar detección por patrón.")
        .defaultValue(true)
        .build());

    private boolean goingUp = true;
    private double offset = 0.0;

    public AntiMace() {
        super(AddonTemplate.CATEGORY, "AntiMace", "Oscila tu posición para anular xMace y xAura.");
    }

    @Override
    public void onActivate() {
        goingUp = true;
        offset = 0.0;
    }

    @Override
    public void onDeactivate() {
        // Enviar un paquete final en la posición real para re-sincronizar
        if (mc.player != null) {
            sendPos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Si only-when-attacked está activo, verificar si hay alguien cerca
        if (onlyWhenAttacked.get()) {
            boolean playerNearby = mc.world.getPlayers().stream()
                .anyMatch(p -> p != mc.player && mc.player.distanceTo(p) <= activationRange.get());
            if (!playerNearby) return;
        }

        double baseX = mc.player.getX();
        double baseY = mc.player.getY();
        double baseZ = mc.player.getZ();
        double step = bounceHeight.get();

        for (int i = 0; i < packetsPerTick.get(); i++) {
            // Variación aleatoria opcional
            double variation = randomize.get() ? (Math.random() * 0.05) : 0.0;
            double currentStep = step + variation;

            if (goingUp) {
                offset += currentStep;
                if (offset >= step * 2) goingUp = false;
            } else {
                offset -= currentStep;
                if (offset <= 0) {
                    offset = 0;
                    goingUp = true;
                }
            }

            // onGround = false → el servidor no acumula distancia de caída real
            sendPos(baseX, baseY + offset, baseZ, false);
        }

        // Paquete final con la posición real para no desincronizarse visualmente
        sendPos(baseX, baseY, baseZ, mc.player.isOnGround());
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, onGround, mc.player.horizontalCollision
            )
        );
    }
}
