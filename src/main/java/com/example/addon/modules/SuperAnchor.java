package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;

public class SuperAnchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .defaultValue(15.0)
        .min(1)
        .sliderMax(15)
        .build());

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());

    private int delayTimer = 0;

    public SuperAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor", "Ultra Fast 15-Block Anchor");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        
        // Evita explotar en el Nether (donde las anchors no explotan al cargarlas)
        if (mc.world.getDimension().hasCeiling()) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        PlayerEntity target = getTarget();
        if (target == null) return;

        double pX = target.getX() + (target.getVelocity().x * 1.5);
        double pY = target.getY() + (target.getVelocity().y * 1.5);
        double pZ = target.getZ() + (target.getVelocity().z * 1.5);
        
        BlockPos pos = BlockPos.ofFloored(pX, pY, pZ).up();

        if (mc.player.distanceTo(target) > range.get()) return;

        int anchorSlot = findItem(Items.RESPAWN_ANCHOR);
        int glowstoneSlot = findItem(Items.GLOWSTONE);
        if (anchorSlot == -1 || glowstoneSlot == -1) return;

        int oldSlot = 0;
        try {
            Field field = mc.player.getInventory().getClass().getDeclaredField("selectedSlot");
            field.setAccessible(true);
            oldSlot = field.getInt(mc.player.getInventory());
        } catch (Exception e) {
            try {
                Field field = mc.player.getInventory().getClass().getDeclaredField("field_7545");
                field.setAccessible(true);
                oldSlot = field.getInt(mc.player.getInventory());
            } catch (Exception ignored) {}
        }

        BlockHitResult hitResult = new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false);

        if (mc.world.getBlockState(pos).isAir()) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(anchorSlot));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
        }

        if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(glowstoneSlot));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
            delayTimer = speed.get();
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

    private int findItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }
}
