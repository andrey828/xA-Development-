package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class SuperTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- NUEVOS SETTINGS ---
    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("main-hand")
        .description("Equipa tótems también en la mano derecha.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> offHand = sgGeneral.add(new BoolSetting.Builder()
        .name("off-hand")
        .description("Equipa tótems en la mano izquierda (Recomendado).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smartActivation = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-activation")
        .description("Solo activa el NoFall si hay enemigos cerca.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .defaultValue(10.0)
        .visible(smartActivation::get)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .defaultValue(true)
        .visible(smartActivation::get)
        .build()
    );

    public SuperTotem() {
        super(AddonTemplate.CATEGORY, "SuperTotem", "AutoTotem ultra rápido con control de manos.");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;
        
        if (p.getEntity(mc.world) == mc.player) {
            reponerTotems();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // NoFall Permanente Inteligente
        if (!smartActivation.get() || entityCheck()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            if (mc.player.fallDistance > 0) mc.player.fallDistance = 0;
        }

        reponerTotems();
    }

    private void reponerTotems() {
        if (mc.player == null) return;

        // 1. Reponer Mano Izquierda
        if (offHand.get() && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
        }

        // 2. Reponer Mano Principal (Solo si está activado)
        if (mainHand.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            // Buscamos un tótem que no sea el de la mano izquierda
            FindItemResult totem = InvUtils.find(itemStack -> 
                itemStack.getItem() == Items.TOTEM_OF_UNDYING && itemStack != mc.player.getOffHandStack()
            );
            if (totem.found()) InvUtils.move().from(totem.slot()).toMainHand();
        }
    }

    private boolean entityCheck() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (ignoreFriends.get() && !Friends.get().shouldAttack(player)) continue;
            if (mc.player.distanceTo(player) <= range.get() && player.isAlive()) return true;
        }
        return false;
    }
             }
