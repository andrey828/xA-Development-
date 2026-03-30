package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("mace-power")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 1000)
        .build());

    private final Setting<Integer> packetsPerHit = sgGeneral.add(new IntSetting.Builder()
        .name("packets-per-hit")
        .defaultValue(30)
        .min(5)
        .sliderMax(100)
        .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .defaultValue(true)
        .build());

    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "xMace", "Maximum Mace Power");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || isWorking) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            if (!String.valueOf(accessor.meteor$getType()).contains("ATTACK")) return;

            Entity entity = accessor.meteor$getEntity();
            if (!(entity instanceof LivingEntity target)) return;
            if (target instanceof PlayerEntity player && Friends.get().isFriend(player)) return;

            event.cancel();
            isWorking = true;

            int oldSlot = mc.player.getInventory().selectedSlot;
            int maceSlot = -1;

            if (autoSwitch.get()) {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }
            }

            if (maceSlot != -1) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));

            executePowerHit(target);

            if (maceSlot != -1) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));

            isWorking = false;
        }
    }

    private void executePowerHit(Entity target) {
        Vec3d pos = mc.player.getPos();
        double h = fallHeight.get();
        int steps = packetsPerHit.get();

        for (int i = 1; i <= steps; i++) {
            sendPos(pos.x, pos.y + (h * i / steps), pos.z, false);
        }

        sendPos(pos.x, pos.y, pos.z, false);

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);

        sendPos(pos.x, pos.y, pos.z, true);
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
