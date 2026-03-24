package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field; 

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("Mace Power")
        .defaultValue(23)
        .min(1)
        .sliderRange(1, 255)
        .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Switch")
        .defaultValue(true)
        .build());

    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "xMace", "Máximo poder del mazo con Infinite Reach.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.getNetworkHandler() == null || isWorking) return;

        if (event.packet instanceof IPlayerMoveC2SPacket move && move.meteor$getTag() == 1337) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            if (!String.valueOf(accessor.meteor$getType()).contains("ATTACK")) return;

            Entity entity = accessor.meteor$getEntity();
            if (!(entity instanceof LivingEntity target)) return;
            if (target instanceof PlayerEntity player && Friends.get().isFriend(player)) return;

            event.cancel();
            isWorking = true;

            
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
                } catch (Exception ex) {
                    oldSlot = 0; 
                }
            }
            // -----------------------------------------------------------

            int maceSlot = -1;
            if (autoSwitch.get()) {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }
            }

            if (maceSlot != -1) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot
                                                                                                  
