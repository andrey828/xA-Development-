package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SimpleCrystal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Place Delay")
        .defaultValue(1)
        .min(0)
        .sliderMax(20)
        .build());

    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Break Delay")
        .defaultValue(1)
        .min(0)
        .sliderMax(20)
        .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .defaultValue(15.0)
        .min(1)
        .sliderMax(15)
        .build());

    private int placeTimer = 0;
    private int breakTimer = 0;

    public SimpleCrystal() {
        super(AddonTemplate.CATEGORY, "xCrystal", "Automatic Crystal PvP");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (breakTimer > 0) breakTimer--;
        if (breakTimer == 0) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity crystal && mc.player.distanceTo(crystal) <= range.get()) {
                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    breakTimer = breakDelay.get();
                    return;
                }
            }
        }

        if (placeTimer > 0) placeTimer--;
        if (placeTimer == 0) {
            PlayerEntity target = getTarget();
            if (target != null) {
                Vec3d targetVelocity = target.getVelocity();
                double pX = target.getX() + (targetVelocity.x * 2);
                double pY = target.getY() + (targetVelocity.y * 2);
                double pZ = target.getZ() + (targetVelocity.z * 2);
                
                BlockPos targetBlock = BlockPos.ofFloored(pX, pY, pZ).down();

                if (mc.world.getBlockState(targetBlock).getBlock() == net.minecraft.block.Blocks.OBSIDIAN || mc.world.getBlockState(targetBlock).getBlock() == net.minecraft.block.Blocks.BEDROCK) {
                    Hand hand = mc.player.getMainHandStack().isOf(Items.END_CRYSTAL) ? Hand.MAIN_HAND : Hand.OFF_HAND;
                    BlockHitResult hitResult = new BlockHitResult(new Vec3d(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()), Direction.UP, targetBlock, false);
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
                    placeTimer = placeDelay.get();
                }
            }
        }
    }

    private PlayerEntity getTarget() {
        PlayerEntity closest = null;
        double closestDist = range.get();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || Friends.get().isFriend(player) || player.isDead()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        return closest;
    }
}
