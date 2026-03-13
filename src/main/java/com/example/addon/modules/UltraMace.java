package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra Heights (Max Power)");

    private final Setting<Integer> macePower = sgGeneral.add(new IntSetting.Builder().name("Mace Power").defaultValue(113).range(1, Integer.MAX_VALUE).sliderRange(1, 20000).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch").defaultValue(true).build());
    private final Setting<Boolean> checkPlayers = sgGeneral.add(new BoolSetting.Builder().name("Check Players").defaultValue(true).build());
    private final Setting<Boolean> doTotemFail = sgGeneral.add(new BoolSetting.Builder().name("Do TotemFail").defaultValue(true).build());
    private final Setting<Integer> hit1 = sgGeneral.add(new IntSetting.Builder().name("Hit 1").defaultValue(30).range(1, Integer.MAX_VALUE).sliderRange(1, 20000).build());
    private final Setting<Integer> hit2 = sgGeneral.add(new IntSetting.Builder().name("Hit 2").defaultValue(60).range(1, Integer.MAX_VALUE).sliderRange(1, 20000).build());
    private final Setting<Integer> extraHitsAmount = sgGeneral.add(new IntSetting.Builder().name("Extra Hits Amount").defaultValue(10).range(0, 30).build());
    private final Setting<Integer> noGroundPackets = sgGeneral.add(new IntSetting.Builder().name("TotemFail Packets").defaultValue(20).range(0, 5000).build());
    private final Setting<Integer> hitAmount = sgGeneral.add(new IntSetting.Builder().name("Global Multiplier").defaultValue(1).range(1, 1000).build());

    private final List<Setting<Integer>> extraHeights = new ArrayList<>();
    private boolean isWorking = false;
    private static Field onGroundField;
    private static Field selectedSlotField;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "UltraMace", "Maximum Mace Power - No Limits.");

        try {
            for (Field field : PlayerMoveC2SPacket.class.getDeclaredFields()) {
                if (field.getType() == boolean.class) {
                    field.setAccessible(true);
                    onGroundField = field;
                    break;
                }
            }
            selectedSlotField = mc.player.getInventory().getClass().getDeclaredField("selectedSlot");
            selectedSlotField.setAccessible(true);
        } catch (Exception ignored) {}

        for (int i = 3; i <= 32; i++) {
            int finalI = i;
            extraHeights.add(sgExtra.add(new IntSetting.Builder()
                .name("Hit " + i)
                .defaultValue(100 + (i * 50))
                .range(1, Integer.MAX_VALUE)
                .sliderRange(1, 20000)
                .visible(() -> extraHitsAmount.get() >= (finalI - 2))
                .build()
            ));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.getNetworkHandler() == null || isWorking) return;
        if (event.packet instanceof IPlayerMoveC2SPacket move && move.meteor$getTag() == 1337) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            if (!String.valueOf(accessor.meteor$getType()).contains("ATTACK")) return;

            Entity entity = accessor.meteor$getEntity();

            if (checkPlayers.get() && !(entity instanceof PlayerEntity)) {
                ChatUtils.info("(Disable) No players Found");
                return;
            }

            if (entity instanceof LivingEntity target) {
                if (target instanceof PlayerEntity player && Friends.get().isFriend(player)) return;

                event.cancel();
                isWorking = true;

                int oldSlot = 0;
                try {
                    oldSlot = (int) selectedSlotField.get(mc.player.getInventory());
                } catch (Exception e) {
                    oldSlot = mc.player.getInventory().selectedSlot;
                }

                int maceSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }

                if (maceSlot != -1) {
                    if (autoSwitch.get()) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));

                    double px = mc.player.getX();
                    double py = mc.player.getY();
                    double pz = mc.player.getZ();

                    for (int a = 0; a < hitAmount.get(); a++) {
                        applyHit(target, macePower.get(), px, py, pz);
                        applyHit(target, hit1.get(), px, py, pz);
                        applyHit(target, hit2.get(), px, py, pz);

                        for (int i = 0; i < extraHitsAmount.get(); i++) {
                            applyHit(target, extraHeights.get(i).get(), px, py, pz);
                        }

                        if (doTotemFail.get()) {
                            for (int i = 0; i < noGroundPackets.get(); i++) {
                                sendPos(px, py + (i * 0.0001), pz, false);
                                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                                sendPos(px, py, pz, true);
                            }
                        }
                    }

                    if (autoSwitch.get()) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                }

                isWorking = false;
            }
        }
    }

    private void applyHit(Entity target, int height, double x, double y, double z) {
        sendPos(x, y + height, z, false);
        sendPos(x, y, z, false); 
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        sendPos(x, y, z, true);
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        try {
            if (onGroundField != null) onGroundField.set(p, onGround);
        } catch (Exception ignored) {}
        mc.getNetworkHandler().sendPacket(p);
    }
}
