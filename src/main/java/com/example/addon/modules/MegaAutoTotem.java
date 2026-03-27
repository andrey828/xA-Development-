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
import net.minecraft.block.Blocks;

public class MegaAutoTotem extends Module {

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> strict = sg.add(new BoolSetting.Builder()
        .name("Strict")
        .defaultValue(true)
        .build());

    private final Setting<Double> hp = sg.add(new DoubleSetting.Builder()
        .name("HP")
        .defaultValue(10)
        .min(0)
        .sliderMax(36)
        .visible(() -> !strict.get())
        .build());

    private long last = 0;

    public MegaAutoTotem() {
        super(AddonTemplate.CATEGORY, "xTotem", "AutoTotem");
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null) return;
        if (System.currentTimeMillis() - last < 10) return;

        if (should()) {
            equip();
            last = System.currentTimeMillis();
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive e) {
        if (e.packet instanceof ExplosionS2CPacket) force();
        if (e.packet instanceof EntityStatusS2CPacket p) {
            if (p.getEntity(mc.world) == mc.player && p.getStatus() == 2) force();
        }
    }

    private boolean should() {
        float h = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return strict.get() ? (h <= 12 || danger()) : (h <= hp.get() || danger());
    }

    private void force() {
        equip();
    }

    private void equip() {
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        FindItemResult t = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!t.found()) return;

        InvUtils.move().from(t.slot()).toOffhand();
    }

    private boolean danger() {
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity && mc.player.distanceTo(e) <= 8) return true;
        }

        BlockPos p = mc.player.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (mc.world.getBlockState(p.add(x, 0, z)).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(InvUtils.find(Items.TOTEM_OF_UNDYING).count());
    }
}
