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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra Heights (Max Power)");

    private final Setting<Integer> macePower = sgGeneral.add(new IntSetting.Builder().name("Mace Power").defaultValue(113).range(1, Integer.MAX_VALUE).sliderRange(1, 20000).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch").defaultValue(true).build());
    private final Setting<Boolean> doTotemFail = sgGeneral.add(new BoolSetting.Builder().name("Do TotemFail").defaultValue(true).build());
    private final Setting<Integer> hit1 = sgGeneral.add(new IntSetting.Builder().name("Hit 1").defaultValue(30).range(1, Integer.MAX_VALUE).sliderRange(1, 20000).build());
    private final Setting<Integer> hit2 = sgGeneral.add(new IntSetting.Builder().name("Hit 2").defaultValue(60).range(1, Integer.MAX_VALUE).sliderRange(1, 20000).build());
    private final Setting<Integer> extraHitsAmount = sgGeneral.add(new IntSetting.Builder().name("Extra Hits Amount").defaultValue(10).range(0, 30).build());
    private final Setting<Integer> noGroundPackets = sgGeneral.add(new IntSetting.Builder().name("TotemFail Packets").defaultValue(20).range(0, 5000).build());
    private final Setting<Integer> hitAmount = sgGeneral.add(new IntSetting.Builder().name("Global Multiplier").defaultValue(1).range(1, 1000).build());

    private final List<Setting<Integer>> extraHeights = new ArrayList<>();
    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "xMace", "Maximum Mace Power - No Limits Anarchy.");

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
            if (entity instanceof LivingEntity target) {
                if (target instanceof PlayerEntity player && Friends.get().isFriend(player)) return;


                Vec3d pPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                Vec3d tPos = new Vec3d(target.getX(), target.getY(), target.getZ());

                if (pPos.distanceTo(tPos) > attackRange.get()) return;

                event.cancel();
                isWorking = true;

                // Buscar el Mazo
                int maceSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }

                if (maceSlot != -1 || mc.player.getMainHandStack().isOf(Items.MACE)) {
                    // Reflexión para leer selectedSlot (Solución al error Private Access)
                    int oldSlot = getSelectedSlot();

                    if (autoSwitch.get() && maceSlot != -1) {
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
                    }

                    // IDA (Teleport)
                    teleport(pPos, tPos);

                    // EJECUCIÓN DE GOLPES
                    for (int a = 0; a < hitAmount.get(); a++) {
                        applyHit(target, macePower.get(), tPos.x, tPos.y, tPos.z);
                        applyHit(target, hit1.get(), tPos.x, tPos.y, tPos.z);
                        applyHit(target, hit2.get(), tPos.x, tPos.y, tPos.z);

                        for (int i = 0; i < extraHitsAmount.get(); i++) {
                            applyHit(target, extraHeights.get(i).get(), tPos.x, tPos.y, tPos.z);
                        }

                        if (doTotemFail.get()) {
                            for (int i = 0; i < noGroundPackets.get(); i++) {
                                sendPos(tPos.x, tPos.y + (i * 0.0001), tPos.z, false);
                                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                                sendPos(tPos.x, tPos.y, tPos.z, false);
                            }
                        }
                    }

                    // VUELTA (Regreso instantáneo)
                    teleport(tPos, pPos);

                    if (autoSwitch.get() && maceSlot != -1) {
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                    }
                }

                isWorking = false;
            }
        }
    }

    private int getSelectedSlot() {
        try {
            Field field = PlayerInventory.class.getDeclaredField("selectedSlot");
            field.setAccessible(true);
            return (int) field.get(mc.player.getInventory());
        } catch (Exception e) {
            return 0; // Fallback
        }
    }

    private void teleport(Vec3d from, Vec3d to) {
        double dist = from.distanceTo(to);
        double steps = Math.ceil(dist / tpStep.get());
        for (int i = 1; i <= (int) steps; i++) {
            Vec3d step = from.lerp(to, (double) i / steps);
            sendPos(step.x, step.y, step.z, true);
        }
    }

    private void applyHit(Entity target, int height, double x, double y, double z) {
        sendPos(x, y + height, z, false);
        sendPos(x, y, z, false);
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        sendPos(x, y, z, false);
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision);

        ((IPlayerMoveC2SPacket) (Object) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
