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
    private final SettingGroup sgVertical = settings.createGroup("Vertical");

    public enum Mode {
        Directional,
        Orbital,
        Predict
    }

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Directional: 8 direcciones fijas. Orbital: círculo alrededor del objetivo. Predict: se mueve hacia donde va el objetivo.")
        .defaultValue(Mode.Orbital)
        .build());

    private final Setting<Double> activationRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("activation-range")
        .description("Solo activa si hay un jugador dentro de este rango.")
        .defaultValue(60.0)
        .min(5.0)
        .sliderMax(200.0)
        .build());

    private final Setting<Boolean> randomize = sgGeneral.add(new BoolSetting.Builder()
        .name("randomize")
        .description("Añade variación aleatoria para evitar detección por patrón.")
        .defaultValue(true)
        .build());

    private final Setting<Double> strafeDistance = sgMovement.add(new DoubleSetting.Builder()
        .name("strafe-distance")
        .description("Distancia de desplazamiento horizontal por paquete.")
        .defaultValue(2.0)
        .min(0.1)
        .sliderMax(15.0)
        .build());

    private final Setting<Integer> packetsPerTick = sgMovement.add(new IntSetting.Builder()
        .name("packets-per-tick")
        .description("Paquetes enviados por tick.")
        .defaultValue(8)
        .min(1)
        .sliderMax(20)
        .build());

    private final Setting<Double> orbitalRadius = sgMovement.add(new DoubleSetting.Builder()
        .name("orbital-radius")
        .description("Radio del círculo orbital alrededor del objetivo (modo Orbital).")
        .defaultValue(3.0)
        .min(0.5)
        .sliderMax(20.0)
        .build());

    private final Setting<Double> orbitalSpeed = sgMovement.add(new DoubleSetting.Builder()
        .name("orbital-speed")
        .description("Velocidad de rotación orbital en grados por tick.")
        .defaultValue(45.0)
        .min(1.0)
        .sliderMax(180.0)
        .build());

    private final Setting<Double> verticalDistance = sgVertical.add(new DoubleSetting.Builder()
        .name("vertical-distance")
        .description("Altura del bounce. Mínimo 10.1 para invalidar el Mace.")
        .defaultValue(11.5)
        .min(10.1)
        .sliderMax(40.0)
        .build());

    private final Setting<Integer> verticalSteps = sgVertical.add(new IntSetting.Builder()
        .name("vertical-steps")
        .description("En cuántos paquetes dividir la subida vertical.")
        .defaultValue(3)
        .min(1)
        .sliderMax(8)
        .build());

    private final Setting<Boolean> doubleReset = sgVertical.add(new BoolSetting.Builder()
        .name("double-reset")
        .description("Envía onGround=true dos veces por ciclo para mayor seguridad contra el Mace.")
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
    private double orbitalAngle = 0.0;
    private Vec3d lastTargetPos = null;

    public AntiMace() {
        super(AddonTemplate.CATEGORY, "AntiMace", "Movimiento evasivo multidireccional + anti-Mace.");
    }

    @Override
    public void onActivate() {
        dirIndex = 0;
        orbitalAngle = 0.0;
        lastTargetPos = null;
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

        if (nearest == null) {
            lastTargetPos = null;
            return;
        }

        double bx = mc.player.getX();
        double by = mc.player.getY();
        double bz = mc.player.getZ();

        double vd = verticalDistance.get();
        if (randomize.get()) vd += Math.random() * 1.5;

        int packets = packetsPerTick.get();

        switch (mode.get()) {
            case Directional -> runDirectional(bx, by, bz, nearest, vd, packets);
            case Orbital     -> runOrbital(bx, by, bz, nearest, vd, packets);
            case Predict     -> runPredict(bx, by, bz, nearest, vd, packets);
        }

        lastTargetPos = nearest.getPos();

        sendPos(bx, by, bz, mc.player.isOnGround());
    }

    private void runDirectional(double bx, double by, double bz, PlayerEntity target, double vd, int packets) {
        Vec3d diff = new Vec3d(target.getX() - bx, 0, target.getZ() - bz).normalize();
        double yaw = Math.atan2(diff.z, diff.x);

        double sd = strafeDistance.get();
        if (randomize.get()) sd += (Math.random() - 0.5) * 0.8;

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

            sendBounce(bx + ox, by, bz + oz, vd);
        }
    }

    private void runOrbital(double bx, double by, double bz, PlayerEntity target, double vd, int packets) {
        double radius = orbitalRadius.get();
        double speed = orbitalSpeed.get();
        double angleStep = speed / packets;

        for (int i = 0; i < packets; i++) {
            orbitalAngle += angleStep;
            if (orbitalAngle >= 360.0) orbitalAngle -= 360.0;

            double rad = Math.toRadians(orbitalAngle);
            double ox = Math.cos(rad) * radius;
            double oz = Math.sin(rad) * radius;

            if (randomize.get()) {
                ox += (Math.random() - 0.5) * 0.3;
                oz += (Math.random() - 0.5) * 0.3;
            }

            sendBounce(target.getX() + ox, by, target.getZ() + oz, vd);
        }
    }

    private void runPredict(double bx, double by, double bz, PlayerEntity target, double vd, int packets) {
        Vec3d targetVel;
        if (lastTargetPos != null) {
            targetVel = target.getPos().subtract(lastTargetPos);
        } else {
            targetVel = Vec3d.ZERO;
        }

        double sd = strafeDistance.get();
        if (randomize.get()) sd += (Math.random() - 0.5) * 0.5;

        for (int i = 0; i < packets; i++) {
            double predict = (i + 1) * 0.5;
            double px = target.getX() + targetVel.x * predict;
            double pz = target.getZ() + targetVel.z * predict;

            Vec3d diff = new Vec3d(px - bx, 0, pz - bz).normalize();
            double yaw = Math.atan2(diff.z, diff.x);

            double perpYaw = yaw + Math.PI / 2.0;
            double side = randomize.get() ? (Math.random() > 0.5 ? 1 : -1) : (i % 2 == 0 ? 1 : -1);

            double ox = Math.cos(perpYaw) * sd * side;
            double oz = Math.sin(perpYaw) * sd * side;

            sendBounce(bx + ox, by, bz + oz, vd);
        }
    }

    private void sendBounce(double x, double by, double z, double vd) {
        if (doubleReset.get()) sendPos(x, by, z, true);
        sendPos(x, by, z, true);

        int steps = verticalSteps.get();
        for (int s = 1; s <= steps; s++) {
            sendPos(x, by + (vd * s / steps), z, false);
        }

        sendPos(x, by, z, true);
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, onGround, mc.player.horizontalCollision
            )
        );
    }
}
