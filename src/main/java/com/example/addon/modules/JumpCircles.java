package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class JumpCircles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> circleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("El color de la onda de choque.")
        .defaultValue(new SettingColor(0, 255, 255, 255))
        .build()
    );

    private final Setting<Integer> duration = sgGeneral.add(new IntSetting.Builder()
        .name("duración")
        .description("Ticks que tarda en desaparecer (20 ticks = 1 segundo).")
        .defaultValue(30)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private final Setting<Double> maxRadius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radio-máximo")
        .description("Qué tanto se expande la onda.")
        .defaultValue(3.5)
        .min(0.1)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<Double> glowWidth = sgGeneral.add(new DoubleSetting.Builder()
        .name("ancho-del-glow")
        .description("Qué tan grueso se ve el resplandor.")
        .defaultValue(0.15)
        .min(0.01)
        .sliderMax(0.5)
        .build()
    );

    private final List<Ripple> ripples = new ArrayList<>();
    private boolean wasOnGround;

    public JumpCircles() {
        super(AddonTemplate.VISUALS, "xJumpCircles", "Ondas de choque estilo gota de agua con glow neón.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (mc.player.isOnGround() && !wasOnGround) {
            spawnRipple();
        }
        wasOnGround = mc.player.isOnGround();

        if (mc.player.getMainHandStack().getItem() == Items.MACE && mc.options.attackKey.isPressed()) {
            spawnRipple();
        }

        ripples.removeIf(r -> r.age > duration.get());
        for (Ripple r : ripples) r.age++;
    }

    private void spawnRipple() {
        ripples.add(new Ripple(new Vec3d(mc.player.getX(), mc.player.getY() + 0.03, mc.player.getZ())));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (Ripple ripple : ripples) {
            double progress = (double) ripple.age / duration.get();
            double radius = progress * maxRadius.get();
            
            int alpha = (int) ((1 - progress) * circleColor.get().a);
            Color baseCol = new Color(circleColor.get().r, circleColor.get().g, circleColor.get().b, alpha);

            for (double i = 0; i < glowWidth.get(); i += 0.02) {
                int layerAlpha = (int) (alpha * (1 - (i / glowWidth.get())));
                Color layerCol = new Color(baseCol.r, baseCol.g, baseCol.b, layerAlpha);

                drawRippleCircle(event, ripple.pos, radius + i, layerCol);
                drawRippleCircle(event, ripple.pos, radius - i, layerCol);
            }
            
            drawRippleCircle(event, ripple.pos, radius, baseCol);
        }
    }

    private void drawRippleCircle(Render3DEvent event, Vec3d pos, double r, Color c) {
        if (r < 0) return;
        int points = 60; 
        for (int i = 0; i < points; i++) {
            double angle = (i * 2 * Math.PI) / points;
            double nextAngle = ((i + 1) * 2 * Math.PI) / points;

            double x1 = pos.x + Math.cos(angle) * r;
            double z1 = pos.z + Math.sin(angle) * r;
            double x2 = pos.x + Math.cos(nextAngle) * r;
            double z2 = pos.z + Math.sin(nextAngle) * r;

            event.renderer.line(x1, pos.y, z1, x2, pos.y, z2, c);
        }
    }

    private static class Ripple {
        public final Vec3d pos;
        public int age = 0;
        public Ripple(Vec3d pos) { this.pos = pos; }
    }
}
