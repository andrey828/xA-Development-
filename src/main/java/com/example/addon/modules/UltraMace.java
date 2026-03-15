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

import java.util.ArrayList;
import java.util.List;

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra Heights (Max Power)");

    private final Setting<Integer> macePower = sgGeneral.add(new IntSetting.Builder().name("Mace Power").defaultValue(100).range(1, 10000).sliderRange(1, 500).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch").defaultValue(true).build());
    private final Setting<Boolean> doTotemFail = sgGeneral.add(new BoolSetting.Builder().name("Do TotemFail").defaultValue(true).build());
    private final Setting<Integer> extraHitsAmount = sgGeneral.add(new IntSetting.Builder().name("Extra Hits Amount").defaultValue(0).range(0, 30).build());
    private final Setting<Integer> noGroundPackets = sgGeneral.add(new IntSetting.Builder().name("TotemFail Packets").defaultValue(15).range(0, 50).build());
    private final Setting<Integer> globalMultiplier = sgGeneral.add(new IntSetting.Builder().name("Global Multiplier").defaultValue(1).range(1, 5).build());

    private final List<Setting<Integer>> extraHeights = new ArrayList<>();
    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "UltraMace", "Máximo daño de maza sin morir por caída.");

        for (int i = 1; i <= 30; i++) {
            int finalI = i;
            extraHeights.add(sgExtra.add(new IntSetting.Builder()
                .name("Extra Hit " + i)
                .defaultValue(150 + (i * 50))
                .range(1, 10000)
                .visible(() -> extraHitsAmount.get() >= finalI)
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
            
            // FIX: Usar String para evitar el error de acceso privado a InteractType
            if (!accessor.meteor$getType().toString().contains("ATTACK")) return;

            Entity entity = accessor.meteor$getEntity();
            if (entity instanceof LivingEntity target) {
                if (target instanceof PlayerEntity player && Friends.get().isFriend(player)) return;

                int maceSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }

                if (maceSlot == -1 && !mc.player.getMainHandStack().isOf(Items.MACE)) return;

                event.cancel();
                isWorking = true;

                // FIX: Usar el método getter para evitar error de acceso privado
                int oldSlot = mc.player.getInventory().selectedSlot;

                if (autoSwitch.get() && maceSlot != -1 && maceSlot != oldSlot) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
                }

                double px = mc.player.getX();
                double py = mc.player.getY();
                double pz = mc.player.getZ();

                for (int a = 0; a < globalMultiplier.get(); a++) {
                    executeMaceHit(target, macePower.get(), px, py, pz);

                    for (int i = 0; i < extraHitsAmount.get(); i++) {
                        executeMaceHit(target, extraHeights.get(i).get(), px, py, pz);
                    }

                    if (doTotemFail.get()) {
                        for (int i = 0; i < noGroundPackets.get(); i++) {
                            sendPositionPacket(px, py + 0.01, pz, false);
                            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                        }
                    }
                }

                if (autoSwitch.get() && maceSlot != -1 && maceSlot != oldSlot) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                }

                isWorking = false;
            }
        }
    }

    private void executeMaceHit(Entity target, int height, double x, double y, double z) {
        sendPositionPacket(x, y + height, z, false);
        sendPositionPacket(x, y, z, false); 
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        sendPositionPacket(x, y, z, false);
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
