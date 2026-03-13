package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.lang.reflect.Field;

public class TotemGuard extends Module {
    private static Field onGroundField;

    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "Totemguarld", "Anula daño de maza y caida absoluta.");
        
        try {
            for (Field field : PlayerMoveC2SPacket.class.getDeclaredFields()) {
                if (field.getType() == boolean.class) {
                    field.setAccessible(true);
                    onGroundField = field;
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (mc.player.fallDistance > 0) {
            mc.player.fallDistance = 0;
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);
            
            try {
                if (onGroundField != null) {
                    onGroundField.set(packet, true);
                }
            } catch (Exception ignored) {}
        }
    }
}
