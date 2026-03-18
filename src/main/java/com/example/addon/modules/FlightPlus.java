package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class FlightPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(
        new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Método de propulsión.")
            .defaultValue(Mode.Velocity)
            .build()
    );

    private final Setting<Double> speed = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("speed")
            .description("Velocidad de traslación horizontal.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> antiKick = sgGeneral.add(
        new BoolSetting.Builder()
            .name("anti-kick")
            .description("Previene el kick por vuelo con micro-descensos.")
            .defaultValue(true)
            .build()
    );

    public FlightPlus() {
        super(AddonTemplate.CATEGORY, "xFlight", "Vuelo avanzado con estabilización y bypass de gravedad.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        mc.player.getAbilities().flying = false;

        // AntiKick
        if (antiKick.get() && mc.player.age % 20 == 0) {
            mc.player.setVelocity(
                mc.player.getVelocity().x,
                -0.04,
                mc.player.getVelocity().z
            );
        }

        switch (mode.get()) {
            case Vanilla:
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().setFlySpeed(speed.get().floatValue() / 10);
                break;

            case Static:
                mc.player.setVelocity(0, 0, 0);
                handleVerticalMovement();
                break;

            case Velocity:
                double x = 0;
                double z = 0;

                Vec3d look = mc.player.getRotationVector().multiply(speed.get());

                if (mc.options.forwardKey.isPressed()) {
                    x += look.x;
                    z += look.z;
                }

                if (mc.options.backKey.isPressed()) {
                    x -= look.x;
                    z -= look.z;
                }

                Vec3d right = look.rotateY((float) Math.toRadians(-90));

                if (mc.options.leftKey.isPressed()) {
                    x -= right.x;
                    z -= right.z;
                }

                if (mc.options.rightKey.isPressed()) {
                    x += right.x;
                    z += right.z;
                }

                mc.player.setVelocity(x, mc.player.getVelocity().y, z);
                handleVerticalMovement();
                break;
        }
    }

    private void handleVerticalMovement() {
        double y = 0;

        if (mc.options.jumpKey.isPressed()) y = speed.get();
        else if (mc.options.sneakKey.isPressed()) y = -speed.get();

        if (mode.get() != Mode.Vanilla) {
            mc.player.setVelocity(
                mc.player.getVelocity().x,
                y,
                mc.player.getVelocity().z
            );
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
        }
    }

    public enum Mode {
        Vanilla,
        Static,
        Velocity
    }
                    }
