package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Items;

public class xAnchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .defaultValue(5.0)
        .min(1)
        .sliderMax(15)
        .build());

    public xAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor", "Anchor Aura");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // Usamos los IDs internos para que compile en cualquier version de mappings
        var blockAttr = mc.player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        var entityAttr = mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);

        if (blockAttr != null) blockAttr.setBaseValue(range.get());
        if (entityAttr != null) entityAttr.setBaseValue(range.get());

        handleDoubleHand();
    }

    private void handleDoubleHand() {
        if (InvUtils.find(Items.TOTEM_OF_UNDYING).count() < 2) return;

        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(InvUtils.find(Items.TOTEM_OF_UNDYING).slot()).toOffhand();
        }
        if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.swap(InvUtils.find(Items.TOTEM_OF_UNDYING).slot(), false);
        }
    }
}
