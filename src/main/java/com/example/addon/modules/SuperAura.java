package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
//Q miras aki ? nadie te llamo 

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAnarchy = settings.createGroup("Anarchy Config");

    public static boolean isSendingAttack = false;

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range").description("Alcance máximo (TP Reach).")
        .defaultValue(100.0).min(1.0).sliderMax(250.0).build());

    private final Setting<Integer> hitDelay = sgGeneral.add(new IntSetting.Builder()
        .name("hit-delay-ms").description("Delay entre ataques en milisegundos (0 = sin límite).")
        .defaultValue(100).min(0).sliderMax(2000).build());

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities").onlyAttackable().defaultValue(EntityType.PLAYER).build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate").description("Rotación forzada al objetivo.")
        .defaultValue(true).build());

    private final Setting<Double> tpStep = sgAnarchy.add(new DoubleSetting.Builder()
        .name("tp-step").description("Tamaño del salto de paquetes.")
        .defaultValue(10.0).min(1.0).sliderMax(30.0).build());

    private final Setting<Integer> packetsPerHit = sgAnarchy.add(new IntSetting.Builder()
        .name("packets-per-hit").description("Cuántos paquetes de ataque enviar por tick.")
        .defaultValue(1).min(1).sliderMax(5).build());

    private final Setting<Boolean> multiTarget = sgAnarchy.add(new BoolSetting.Builder()
        .name("multi-target").description("Ataca a varios objetivos a la vez si están en rango.")
        .defaultValue(false).build());

    private final Setting<Boolean> forceOnGround = sgAnarchy.add(new BoolSetting.Builder()
        .name("force-on-ground").description("Mantiene el estado 'en el suelo' para evitar kicks.")
        .defaultValue(true).build());

    private final Setting<Boolean> teleportBack = sgAnarchy.add(new BoolSetting.Builder()
        .name("instant-return").description("Regresa instantáneamente.")
        .defaultValue(true).build());

    private final Setting<Boolean> attackInvisibles = sgAnarchy.add(new BoolSetting.Builder()
        .name("attack-invisibles").defaultValue(true).build());

    private final Setting<Boolean> ignoreWalls = sgAnarchy.add(new BoolSetting.Builder()
        .name("ignore-walls").description("Ataca a través de cualquier bloque.")
        .defaultValue(true).build());

    private final Setting<Boolean> tpsSync = sgAnarchy.add(new BoolSetting.Builder()
        .name("tps-sync").description("Sincroniza el delay con los TPS del servidor.")
        .defaultValue(true).build());

    private final Setting<Boolean> critBypass = sgAnarchy.add(new BoolSetting.Builder()
        .name("packet-crits").description("Intenta hacer críticos mediante paquetes.")
        .defaultValue(false).build());

    private final Setting<Boolean> swing = sgAnarchy.add(new BoolSetting.Builder()
        .name("show-swing").defaultValue(true).build());

    private final Setting<Boolean> antiFriend = sgAnarchy.add(new BoolSetting.Builder()
        .name("anti-friend").defaultValue(true).build());

    private final Setting<Double> minHealth = sgAnarchy.add(new DoubleSetting.Builder()
        .name("safety-health").description("Se apaga si tu vida es menor a esto.")
        .defaultValue(0.0).min(0.0).sliderMax(20.0).build());

    private long lastAttackTime = 0;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "xAura", "Infinite Reach KillAura");
    }

    @Override
    public void onActivate() {
        lastAttackTime = 0;
        isSendingAttack = false;
    }

    @Override
    public void onDeactivate() {
        isSendingAttack = false;
    }

    /**
     * Interfaz funcional para que xMace pase su propio sendPos taggeado con 1337.
     * Así todos los paquetes de movimiento usan el mismo tag y el filtro de
     * onSendPacket no los cancela.
     */
    public interface TaggedPosSender {
        void send(Vec3d pos, boolean onGround);
    }

    /**
     * Llamado por UltraMace. Hace el TP hacia el objetivo usando el sendPos
     * taggeado de xMace, ejecuta la acción (hits del mace) y regresa al origen.
     *
     * IMPORTANTE: action.run() ya setea isSendingAttack internamente en sendAttack()
     * de UltraMace, así que aquí NO lo envolvemos para no pisarlo.
     */
    public void teleportToAndBack(Entity target, Runnable action, TaggedPosSender taggedSendPos) {
        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d destination = new Vec3d(target.getX(), target.getY(), target.getZ());

        double distance = origin.distanceTo(destination);
        double step = Math.min(tpStep.get(), distance);
        int steps = (step <= 0) ? 1 : (int) Math.ceil(distance / step);

        // TP hacia el objetivo con paquetes taggeados de xMace
        for (int i = 1; i <= steps; i++) {
            Vec3d next = origin.lerp(destination, (double) i / steps);
            taggedSendPos.send(next, true);
        }

        // Hits del mace — executeHits usará destination como origen
        action.run();

        // Regreso al origen
        if (teleportBack.get()) {
            taggedSendPos.send(origin, true);
        } else {
            for (int i = steps - 1; i >= 0; i--) {
                Vec3d next = origin.lerp(destination, (double) i / steps);
                taggedSendPos.send(next, true);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;
        if (mc.player.getHealth() <= minHealth.get()) { toggle(); return; }

        // Si xMace está activo, xAura no ataca por su cuenta — xMace lo invoca directamente
        if (Modules.get().isActive(UltraMace.class)) return;

        long now = System.currentTimeMillis();
        if (now - lastAttackTime < hitDelay.get()) return;

        if (multiTarget.get()) {
            List<Entity> targets = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(this::isValidTarget)
                .toList();

            if (targets.isEmpty()) return;
            targets.forEach(this::attackProcess);
        } else {
            Entity target = findTarget();
            if (target == null) return;
            attackProcess(target);
        }

        lastAttackTime = System.currentTimeMillis();
    }

    private void attackProcess(Entity target) {
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target),
                () -> doInfiniteAttack(target));
        } else {
            doInfiniteAttack(target);
        }
    }

    private void doInfiniteAttack(Entity target) {
        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d destination = new Vec3d(target.getX(), target.getY(), target.getZ());

        double distance = origin.distanceTo(destination);
        double step = Math.min(tpStep.get(), distance);
        int steps = (step <= 0) ? 1 : (int) Math.ceil(distance / step);

        for (int i = 1; i <= steps; i++) {
            Vec3d next = origin.lerp(destination, (double) i / steps);
            sendPos(next);
        }

        if (critBypass.get()) {
            sendPos(new Vec3d(destination.x, destination.y + 0.11, destination.z));
            sendPos(new Vec3d(destination.x, destination.y + 0.1001, destination.z));
        }

        for (int i = 0; i < packetsPerHit.get(); i++) {
            try {
                isSendingAttack = true;
                mc.player.networkHandler.sendPacket(
                    PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking())
                );
            } finally {
                isSendingAttack = false;
            }
        }

        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);

        if (teleportBack.get()) {
            sendPos(origin);
        } else {
            for (int i = steps - 1; i >= 0; i--) {
                Vec3d next = origin.lerp(destination, (double) i / steps);
                sendPos(next);
            }
        }
    }

    /** sendPos propio de xAura — sin tag 1337, solo para uso independiente */
    public void sendPos(Vec3d pos) {
        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x, pos.y, pos.z,
                forceOnGround.get(),
                false
            )
        );
    }

    private boolean isValidTarget(Entity e) {
        if (!(e instanceof LivingEntity) || !e.isAlive() || e == mc.player) return false;
        if (!entities.get().contains(e.getType())) return false;

        double distSq = mc.player.squaredDistanceTo(e);
        double rangeSq = range.get() * range.get();
        if (distSq > rangeSq) return false;

        if (!attackInvisibles.get() && e.isInvisible()) return false;
        if (antiFriend.get() && e instanceof PlayerEntity pe && Friends.get().isFriend(pe)) return false;

        if (!ignoreWalls.get() && mc.world != null) {
            Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
            Vec3d targetEye = new Vec3d(e.getX(), e.getEyeY(), e.getZ());

            BlockHitResult result = mc.world.raycast(new RaycastContext(
                eyePos,
                targetEye,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
            ));

            if (result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) return false;
        }

        return true;
    }

    private Entity findTarget() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
            .filter(this::isValidTarget)
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
            .orElse(null);
    }
                    }
                 
