package com.example.addon.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    // Esto permite cambiar el estado de onGround que Minecraft tiene bloqueado
    @Accessor("onGround")
    void setOnGround(boolean onGround);
}

