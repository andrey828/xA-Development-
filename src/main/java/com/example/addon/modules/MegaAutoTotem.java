package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.BlockPos;

public class MegaAutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDouble = settings.createGroup("Double Hand");

    private final Setting<Double> health = sgGeneral.add(new DoubleSetting.Builder()
        .name("Health Threshold")
        .defaultValue(6)
        .min(0)
        .sliderMax(36)
        .build());

    private final Setting<Boolean> predictExplosions = sgGeneral.add(new BoolSetting.Builder()
        .name("Predict Explosions")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> forceDouble = sgDouble.add(new BoolSetting.Builder()
        .name("Force Double Hand")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> doubleOnlyOnLowHealth = sgDouble.add(new BoolSetting.Builder()
        .name("Only on Low Health")
        .defaultValue(false)
        .visible(forceDouble::get)
        .build());

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "xTotem", "Ultra Fast AutoTotem");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (shouldHaveTotemOffhand()) {
            equipTotem(true);
        }

        if (shouldHaveTotemMainhand()) {
            equipTotem(false);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (predictExplosions.get() && event.packet instanceof ExplosionS2CPacket) {
            forceImmediate();
        }

        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getEntity(mc.world) == mc.player && packet.getStatus() == 2) {
                if (mc.player.getHealth() <= health.get() + 2) forceImmediate();
            }
        }
    }

    private boolean shouldHaveTotemOffhand() {
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return false;
        return mc.player.getHealth() + mc.player.getAbsorptionAmount() <= health.get() || isNearExplosive() || forceDouble.get();
    }

    private boolean shouldHaveTotemMainhand() {
        if (!forceDouble.get()) return false;
        if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) return false;
        if (doubleOnlyOnLowHealth.get() && (mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.get())) return false;
        return InvUtils.find(Items.TOTEM_OF_UNDYING).count() > 1;
    }

    private void equipTotem(boolean offhand) {
        FindItemResult totem = InvUtils.find(stack -> stack.getItem() == Items.TOTEM_OF_UNDYING, 9, 35);
        if (!totem.found()) totem = InvUtils.find(Items.TOTEM_OF_UNDYING);

        if (totem.found()) {
            if (offhand) {
                InvUtils.move().from(totem.slot()).toOffhand();
            } else {
                InvUtils.swap(totem.slot(), false);
            }
        }
    }

    private void forceImmediate() {
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) equipTotem(true);
        if (forceDouble.get() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) equipTotem(false);
    }

    private boolean isNearExplosive() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= 10) return true;
        }
        BlockPos p = mc.player.getBlockPos();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (mc.world.getBlockState(p.add(x, 0, z)).getBlock() == net.minecraft.block.Blocks.RESPAWN_ANCHOR) return true;
            }
        }
        return false;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(InvUtils.find(Items.TOTEM_OF_UNDYING).count());
    }
}
