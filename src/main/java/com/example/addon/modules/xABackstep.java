package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.Vec3d;
import java.util.LinkedList;

public class xABackstep extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Estética");
    private final SettingGroup sgBypass = settings.createGroup("Bypass Maestro");

    private final Setting<Keybind> executeKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("tecla-ejecutar")
        .description("Tecla interna para volver al pasado.")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Integer> seconds = sgGeneral.add(new IntSetting.Builder()
        .name("tiempo-rastro")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("color-rastro")
        .defaultValue(new SettingColor(0, 255, 200, 255))
        .build()
    );

    private final Setting<Integer> packetsPerTick = sgBypass.add(new IntSetting.Builder()
        .name("velocidad-bypass")
        .defaultValue(3)
        .min(1)
        .sliderMax(10)
        .build()
    );

    private final LinkedList<Vec3d> history = new LinkedList<>();

    public xABackstep() {
        super(AddonTemplate.CATEGORY, "xA-Backstep", "Graba tu camino y vuelve atrás con una tecla.");
    }

    @Override
    public void onActivate() {
        history.clear();
        ChatUtils.info("Empezando a grabar rastro temporal...");
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (executeKey.get().isPressed() && mc.currentScreen == null) {
            if (history.isEmpty()) return;
            performBackstep();
        }
    }

    private void performBackstep() {
        Vec3d destination = history.getFirst();

        mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(0));

        for (int i = history.size() - 1; i >= 0; i -= packetsPerTick.get()) {
            sendPos(history.get(i));
        }

        sendPos(destination);
        mc.player.setPosition(destination.getX(), destination.getY(), destination.getZ());
        mc.player.setVelocity(0, 0, 0);

        ChatUtils.info("Rebobinado completado. ¡Has vuelto al pasado!");
        
        history.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        
        Vec3d currentPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        history.addLast(currentPos);
        
        while (history.size() > seconds.get() * 20) history.removeFirst();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (history.size() < 2) return;
        for (int i = 0; i < history.size() - 1; i++) {
            Vec3d p1 = history.get(i);
            Vec3d p2 = history.get(i + 1);
            event.renderer.line(p1.getX(), p1.getY() + 0.1, p1.getZ(),
                                p2.getX(), p2.getY() + 0.1, p2.getZ(), lineColor.get());
        }
    }

    private void sendPos(Vec3d pos) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(pos.getX(), pos.getY(), pos.getZ(), true, false);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
