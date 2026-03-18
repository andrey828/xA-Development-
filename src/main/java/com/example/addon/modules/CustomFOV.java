package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class CustomFOV extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> FOV = sgGeneral.add(
        new IntSetting.Builder()
            .name("fov")
            .defaultValue(120)
            .range(0, 358)
            .sliderRange(0, 358)
            .build()
    );

    private final Setting<Boolean> staticFov = sgGeneral.add(
        new BoolSetting.Builder()
            .name("static-fov")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> swingSpeed = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("swing-speed")
            .description("0.1 = efecto cámara lenta.")
            .defaultValue(0.15)
            .min(0.01)
            .sliderMax(1.0)
            .build()
    );

    public CustomFOV() {
        super(AddonTemplate.VISUALS, "xCustomFOV", "Control avanzado del FOV y animaciones.");
    }

    @EventHandler
    private void onGetFov(GetFovEvent event) {
        if (staticFov.get()) event.fov = 1.0f;
        event.fov *= (FOV.get() / 70f);
    }

    @EventHandler
    private void onRenderItem(HeldItemRendererEvent event) {
        if (event.hand == net.minecraft.util.Hand.MAIN_HAND) {
            event.matrix.translate(0.15f, -0.2f, 0.0f);
            event.matrix.scale(0.7f, 0.7f, 0.7f);
        } else {
            event.matrix.translate(-0.15f, -0.2f, 0.0f);
            event.matrix.scale(0.7f, 0.7f, 0.7f);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player != null && mc.player.handSwingProgress > 0) {
            mc.player.handSwingProgress *= swingSpeed.get();
        }
    }
}
