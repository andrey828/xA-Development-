package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.accessor.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TotemGuard extends Module {

    public TotemGuard() {
        super(AddonTemplate.CATEGORY, "xTotem", "Cancels all fall damage, even with fly.");
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (event.packet instanceof PlayerMoveC2SPacket movePacket) {
            ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(false);
            mc.player.fallDistance = 0f;
        }
    }
}
