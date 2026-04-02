package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class AntiMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("Movement");

    private final Setting<Double> strafeDistance = sgMovement.add(new DoubleSetting.Builder()
        .name("strafe-distance")
        .description("Distancia de movimiento lateral/diagonal por paquete (bloques).")
        .defaultValue(1.5)
        .min(0.1)
        .sliderMax(10.0)
        .build());

    private final Setting<Double> verticalDistance = sgMovement.add(new DoubleSetting.Builder()
        .name("vertical-distance")
        .description("Distancia vertical del bounce. Mínimo ~10.5 para invalidar el Mace.")
        .defaultValue(11.0)
        .min(10.1)
        .sliderMax(30.0)
        .build());

    private final Setting<Integer> packetsPerTick = sgMovement.add(new IntSetting.Builder()
        .name("packets-per-tick")
        .description("Paquetes de movimiento enviados por tick.")
        .defaultValue(6)
        .min(1)
        .sliderMax(16)
        .build());

    private final Setting<Boolean> relativeToTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("relative-to-target")
        .description("Las direcciones son relativas al jugador más cercano.")
        .defaultValue(true)
        .build());

    private final Setting<Double> activationRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("activation-range")
        .description("Solo activa si hay un jugador dentro de este rango.")
        .defaultValue(50.0)
        .min(5.0)
        .sliderMax(200.0)
        .build());

    private final Setting<Boolean> randomize = sgGeneral.add(new BoolSetting.Builder()
        .name("randomize")
        .description("Aleatoriza el orden y magnitud de las direcciones cada tick.")
        .defaultValue(true)
        .build());

    private static final double[][] DIRECTIONS = {
        { 1,  0,  0},
        {-1,  0,  0},
        { 0,  0,  1},
        { 0,  0, -1},
        { 1,  0,  1},
        { 1,  0, -1},
        {-1,  0,  1},
        {-1,  0, -1},
    };

    private int dirIndex = 0;

    public AntiMace() {
        super(AddonTemplate.CATEGORY, "xAntiMace", "Movimiento evasivo multidireccional + anti-Mace.");
    }

    @Override
    public void onActivate() {
        dirIndex = 0;
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null) return;
        sendPos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity nearest = mc.world.getPlayers().stream()
            .filter(p -> p != mc.player && mc.player.distanceTo(p) <= activationRange.get())
            .min((a, b) -> Double.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)))
            .orElse(null);

        if (nearest == null) return;

        double bx = mc.player.getX();
        double by = mc.player.getY();
        double bz = mc.player.getZ();

        double yaw;
        if (relativeToTarget.get()) {
            Vec3d diff = new Vec3d(nearest.getX() - bx, 0, nearest.getZ() - bz).normalize();
            yaw = Math.atan2(diff.z, diff.x);
        } else {
            yaw = Math.toRadians(mc.player.getYaw());
        }

        double sd = strafeDistance.get();
        double vd = verticalDistance.get();
        if (randomize.get()) {
            sd += (Math.random() - 0.5) * 0.5;
            vd += Math.random() * 2.0;
        }

        int packets = packetsPerTick.get();
        int totalDirs = DIRECTIONS.length;

        for (int i = 0; i < packets; i++) {
            int idx = randomize.get()
                ? (int)(Math.random() * totalDirs)
                : (dirIndex++ % totalDirs);

            double[] dir = DIRECTIONS[idx];
            double rotX = dir[0] * Math.cos(yaw) - dir[2] * Math.sin(yaw);
            double rotZ = dir[0] * Math.sin(yaw) + dir[2] * Math.cos(yaw);

            double ox = rotX * sd;
            double oz = rotZ * sd; //papoi

            sendPos(bx + ox, by, bz + oz, true);
            sendPos(bx + ox, by + vd, bz + oz, false);
            sendPos(bx + ox, by, bz + oz, true);
        }

        sendPos(bx, by, bz, mc.player.isOnGround());
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, onGround, mc.player.horizontalCollision
            )
        );
    }
}
