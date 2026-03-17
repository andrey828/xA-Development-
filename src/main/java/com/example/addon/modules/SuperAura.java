package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

public class SuperAura extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgMulti = settings.createGroup("Multi-Target");

    private final Setting<Double> range = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("range")
            .defaultValue(250.0)
            .min(1.0)
            .sliderMax(500.0)
            .build()
    );

    private final Setting<Integer> hitDelay = sgGeneral.add(
        new IntSetting.Builder()
            .name("hit-delay")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(
        new BoolSetting.Builder()
            .name("rotate")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> tpBypass = sgGeneral.add(
        new BoolSetting.Builder()
            .name("tp-bypass")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgTargeting.add(
        new BoolSetting.Builder()
            .name("ignore-friends")
            .defaultValue(true)
            .build()
    );

    private final Setting<Set<EntityType<?>>> mobFilter =
        sgTargeting.add(new EntityTypeListSetting.Builder()
            .name("mob-filter")
            .description("Selecciona mobs específicos para atacar.")
            .onlyAttackable()
            .build()
        );

    private final Setting<Boolean> multiTarget = sgMulti.add(
        new BoolSetting.Builder()
            .name("multi-target")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> multiRange = sgMulti.add(
        new DoubleSetting.Builder()
            .name("aoe-range")
            .defaultValue(6.0)
            .min(1.0)
            .sliderMax(10.0)
            .visible(multiTarget::get)
            .build()
    );

    private Entity primaryTarget;
    private int timer;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "xAura", "Optimized Multi Aura.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (mc.player == null || mc.world == null || !mc.player.isAlive()) return;

        if (timer > 0) timer--;

        primaryTarget = findPrimaryTarget();

        if (primaryTarget == null) return;

        if (rotate.get()) {
            Rotations.rotate(
                Rotations.getYaw(primaryTarget),
                Rotations.getPitch(primaryTarget),
                this::executeAura
            );
        } else {
            executeAura();
        }
    }

    private void executeAura() {

        if (timer > 0) return;

        double startX = mc.player.getX();
        double startY = mc.player.getY();
        double startZ = mc.player.getZ();

        double distance = mc.player.distanceTo(primaryTarget);

        if (tpBypass.get() && distance > 4) {

            int steps = Math.max(2, (int) (distance / 6));

            for (int i = 1; i <= steps; i++) {

                double t = i / (double) steps;

                double x = startX + (primaryTarget.getX() - startX) * t;
                double y = startY + (primaryTarget.getY() - startY) * t;
                double z = startZ + (primaryTarget.getZ() - startZ) * t;

                mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false)
                );

                if (i == steps) {
                    hitEntitiesAt(x, y, z);
                }
            }

            mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(startX, startY, startZ, true, false)
            );

        } else {

            mc.interactionManager.attackEntity(mc.player, primaryTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        timer = hitDelay.get();
    }

    private void hitEntitiesAt(double x, double y, double z) {

        List<Entity> entities = mc.world.getOtherEntities(
            mc.player,
            mc.player.getBoundingBox().expand(multiRange.get())
        );

        double rangeSq = multiRange.get() * multiRange.get();

        for (Entity e : entities) {

            if (!(e instanceof LivingEntity) || !e.isAlive()) continue;

            if (!mobFilter.get().contains(e.getType())) continue;

            if (ignoreFriends.get()
                && e instanceof PlayerEntity
                && Friends.get().isFriend((PlayerEntity) e))
                continue;

            if (e.squaredDistanceTo(x, y, z) <= rangeSq) {

                mc.interactionManager.attackEntity(mc.player, e);
                mc.player.swingHand(Hand.MAIN_HAND);

                if (!multiTarget.get()) break;
            }
        }
    }

    private Entity findPrimaryTarget() {

        double rangeSq = range.get() * range.get();

        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)

            .filter(e -> e instanceof LivingEntity && e.isAlive() && e != mc.player)

            .filter(e -> mc.player.squaredDistanceTo(e) <= rangeSq)

            .filter(e -> {

                if (!mobFilter.get().contains(e.getType())) return false;

                if (ignoreFriends.get()
                    && e instanceof PlayerEntity
                    && Friends.get().isFriend((PlayerEntity) e))
                    return false;

                return true;
            })

            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))

            .orElse(null);
    }

    @Override
    public String getInfoString() {
        return primaryTarget != null
            ? "xAura: " + primaryTarget.getName().getString()
            : null;
    }
}
