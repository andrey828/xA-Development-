package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class xAnchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .defaultValue(5.0)
        .min(1)
        .sliderMax(15)
        .build());

    private final Setting<Boolean> forceDouble = sgGeneral.add(new BoolSetting.Builder()
        .name("force-double-hand")
        .defaultValue(true)
        .build());

    public xAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor", "Anchor Aura ");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        updateReach();
        handleDoubleHand();
        
        BlockPos targetPos = findTargetBlock();
        if (targetPos != null) {
            executeAnchorCycle(targetPos);
        }
    }

    private void updateReach() {
        mc.player.getAttributeInstance(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(range.get());
        mc.player.getAttributeInstance(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(range.get());
    }

    private void handleDoubleHand() {
        if (!forceDouble.get()) return;
        if (InvUtils.find(Items.TOTEM_OF_UNDYING).count() < 2) return;

        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(InvUtils.find(Items.TOTEM_OF_UNDYING).slot()).toOffhand();
        }
        if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.swap(InvUtils.find(Items.TOTEM_OF_UNDYING).slot(), false);
        }
    }

    private void executeAnchorCycle(BlockPos pos) {
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);

        if (!anchor.found() || !glowstone.found()) return;

        if (mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) {
            InvUtils.swap(anchor.slot(), false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
        } else {
            InvUtils.swap(glowstone.slot(), false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
        }
    }

    private BlockPos findTargetBlock() {
        return null; 
    }
}
