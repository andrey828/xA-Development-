package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra Heights (Max Power)");

    private final Setting<Integer> macePower = sgGeneral.add(new IntSetting.Builder().name("Mace Power").defaultValue(113).range(1, 30000).sliderRange(1, 20000).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch").defaultValue(true).build());
    private final Setting<Boolean> totemFail = sgGeneral.add(new BoolSetting.Builder().name("Do TotemFail").defaultValue(true).build());
    private final Setting<Integer> extraHitsAmount = sgGeneral.add(new IntSetting.Builder().name("Extra Hits Amount").defaultValue(10).range(0, 30).build());
    private final Setting<Integer> noGroundPackets = sgGeneral.add(new IntSetting.Builder().name("NoGround Packets").defaultValue(20).range(0, 500).build());
    private final Setting<Integer> globalMultiplier = sgGeneral.add(new IntSetting.Builder().name("Global Multiplier").defaultValue(1).range(1, 100).build());

    private final List<Setting<Integer>> extraHeights = new ArrayList<>();
    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "UltraMace", "Exploit de daño masivo para la maza (Bypass NoGround).");

        for (int i = 1; i <= 30; i++) {
            extraHeights.add(sgExtra.add(new IntSetting.Builder()
                .name("Hit " + i)
                .defaultValue(100 + (i * 50))
                .range(1, 30000)
                .build()
            ));
        }
    }

    @Override
    public void onActivate() {
        ChatUtils.info("UltraMace" + Formatting.GRAY + ": Sistema cargado.");
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

                // Buscamos el mazo en la hotbar
                int maceSlot = InvUtils.findInHotbar(Items.MACE).slot();

                if (maceSlot != -1) {
                    event.cancel();
                    isWorking = true;

                    // Guardamos el slot actual usando el método de Meteor para evitar accesos privados
                    int oldSlot = mc.player.getInventory().selectedSlot; 
                    // Si la línea de arriba falla, el compilador está roto. Usaremos InvUtils.
                    
                    if (autoSwitch.get()) {
                        InvUtils.swap(maceSlot, false);
                    }

                    double px = mc.player.getX();
                    double py = mc.player.getY();
                    double pz = mc.player.getZ();

                    for (int a = 0; a < globalMultiplier.get(); a++) {
                        applyHit(target, macePower.get(), px, py, pz);

                        for (int i = 0; i < extraHitsAmount.get(); i++) {
                            applyHit(target, extraHeights.get(i).get(), px, py, pz);
                        }

                        if (totemFail.get()) {
                            for (int i = 0; i < noGroundPackets.get(); i++) {
                                sendPos(px, py + 0.05, pz, false);
                                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                            }
                        }
                    }

                    if (autoSwitch.get()) {
                        // Volvemos al slot que teníamos antes
                        InvUtils.swapBack();
                    }
                    
                    ChatUtils.info(Formatting.GOLD + "UltraMace" + Formatting.GRAY + ": Ataque ejecutado.");
                    isWorking = false;
                }
            }
        }
    }

    private void applyHit(Entity target, int height, double x, double y, double z) {
        sendPos(x, y + height, z, false);
        sendPos(x, y + 0.05, z, false); 
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
