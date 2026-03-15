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

    private final Setting<Integer> macePower = sgGeneral.add(new IntSetting.Builder().name("Mace Power").description("Altura virtual para el primer golpe.").defaultValue(100).range(1, 10000).sliderRange(1, 500).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch").description("Cambia a la maza automáticamente al atacar.").defaultValue(true).build());
    private final Setting<Boolean> doTotemFail = sgGeneral.add(new BoolSetting.Builder().name("Do TotemFail").description("Intenta romper tótems enviando ataques rápidos.").defaultValue(true).build());
    private final Setting<Integer> extraHitsAmount = sgGeneral.add(new IntSetting.Builder().name("Extra Hits Amount").description("Cuántos golpes extra de altura añadir.").defaultValue(0).range(0, 30).build());
    private final Setting<Integer> noGroundPackets = sgGeneral.add(new IntSetting.Builder().name("TotemFail Packets").description("Cantidad de paquetes para el bypass de tótem.").defaultValue(15).range(0, 50).build());
    private final Setting<Integer> globalMultiplier = sgGeneral.add(new IntSetting.Builder().name("Global Multiplier").description("Repite todo el proceso N veces.").defaultValue(1).range(1, 5).build());

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

        // Evitar procesar nuestros propios paquetes marcados
        if (event.packet instanceof IPlayerMoveC2SPacket move && move.meteor$getTag() == 1337) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            
            // Solo si es un ataque
            if (accessor.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

            Entity entity = accessor.meteor$getEntity();
            if (entity instanceof LivingEntity target) {
                if (target instanceof PlayerEntity player && Friends.get().isFriend(player)) return;

                // Buscar maza en la hotbar
                int maceSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }

                // Si no tenemos la maza y no la tenemos en mano, ignorar
                if (maceSlot == -1 && !mc.player.getMainHandStack().isOf(Items.MACE)) return;

                event.cancel();
                isWorking = true;

                int oldSlot = mc.player.getInventory().selectedSlot;

                // Auto-Switch
                if (autoSwitch.get() && maceSlot != -1 && maceSlot != oldSlot) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
                }

                double px = mc.player.getX();
                double py = mc.player.getY();
                double pz = mc.player.getZ();

                for (int a = 0; a < globalMultiplier.get(); a++) {
                    // Golpe principal
                    executeMaceHit(target, macePower.get(), px, py, pz);

                    // Golpes extra
                    for (int i = 0; i < extraHitsAmount.get(); i++) {
                        executeMaceHit(target, extraHeights.get(i).get(), px, py, pz);
                    }

                    // Totem Fail (Envía múltiples ataques sin suelo para intentar saltar el cooldown de daño)
                    if (doTotemFail.get()) {
                        for (int i = 0; i < noGroundPackets.get(); i++) {
                            sendPositionPacket(px, py + 0.01, pz, false);
                            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
                        }
                    }
                }

                // Volver al slot anterior
                if (autoSwitch.get() && maceSlot != -1 && maceSlot != oldSlot) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                }

                isWorking = false;
            }
        }
    }

    private void executeMaceHit(Entity target, int height, double x, double y, double z) {
        // 1. Subir al servidor (onGround false)
        sendPositionPacket(x, y + height, z, false);
        
        // 2. Bajar (Seguimos en false para que el server no nos mate)
        sendPositionPacket(x, y, z, false); 
        
        // 3. Atacar (La maza detectará la caída acumulada desde 'height')
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        
        // 4. Paquete de seguridad para resetear caída sin morir
        sendPositionPacket(x, y, z, false);
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
