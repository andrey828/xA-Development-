package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(400.0).min(1.0).sliderMax(1000.0).build());
    private final Setting<Boolean> tpBypass = sgGeneral.add(new BoolSetting.Builder().name("tp-bypass").defaultValue(true).build());
    private final Setting<Boolean> onlyPlayers = sgGeneral.add(new BoolSetting.Builder().name("only-players").defaultValue(true).build());
    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder().name("ignore-friends").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());

    private Entity target;

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Killaura con TP-Bypass para distancias extremas.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || !mc.player.isAlive()) {
            target = null;
            return;
        }

        target = findTarget();
        if (target == null) return;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> attackTarget(target));
        } else {
            attackTarget(target);
        }
    }

    private Entity findTarget() {
        List<Entity> entities = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
            .filter(e -> e instanceof LivingEntity && e.isAlive() && e != mc.player)
            .filter(e -> mc.player.distanceTo(e) <= range.get())
            .filter(e -> {
                if (onlyPlayers.get() && !(e instanceof PlayerEntity)) return false;
                if (ignoreFriends.get() && e instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) e)) return false;
                return true;
            })
            .sorted(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
            .collect(Collectors.toList());

        return entities.isEmpty() ? null : entities.get(0);
    }

    private void attackTarget(Entity entity) {
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.9f) return;

        if (tpBypass.get() && mc.player.distanceTo(entity) > 5) {
            Vec3d origin = mc.player.getPos();
            
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(entity.getX(), entity.getY(), entity.getZ(), false, mc.player.horizontalCollision));
            
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(origin.x, origin.y, origin.z, true, mc.player.horizontalCollision));
        } else {
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getName().getString();
        return null;
    }
}
