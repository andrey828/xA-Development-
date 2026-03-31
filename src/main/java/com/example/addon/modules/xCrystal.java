package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

public class xCrystal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- CONFIGURACIÓN ---
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Target Range")
        .description("Distancia para detectar enemigos.")
        .defaultValue(10.0)
        .min(1)
        .sliderMax(15)
        .build());

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Place Range")
        .description("Distancia máxima para colocar el cristal.")
        .defaultValue(4.5)
        .min(1)
        .sliderMax(6)
        .build());

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Place Delay")
        .description("Ticks entre cada colocación (0 = instantáneo).")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Swich")
        .description("Cambia al cristal automáticamente.")
        .defaultValue(true)
        .build());

    public xCrystal() {
        super(AddonTemplate.CATEGORY, "xCrystal", "Crystal Aura funcional para 1.21.x");
    }

    private int timer;

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // --- EXPLOTAR cristales existentes primero ---
        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                mc.player.getBoundingBox().expand(placeRange.get()),
                e -> true)) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        if (timer > 0) {
            timer--;
            return;
        }

        PlayerEntity target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestHealth);
        if (target == null) return;

        BlockPos placePos = findPlacePos(target);

        if (placePos != null) {
            if (placeCrystal(placePos)) {
                timer = placeDelay.get();
            }
        }
    }

    private BlockPos findPlacePos(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();

        for (BlockPos pos : BlockPos.iterate(targetPos.add(-2, -1, -2), targetPos.add(2, 1, 2))) {
            if (canPlaceCrystal(pos)) {
                if (PlayerUtils.distanceTo(pos) <= placeRange.get()) {
                    return pos.toImmutable();
                }
            }
        }
        return null;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN && mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) return false;
        return mc.world.isAir(pos.up());
    }

    private boolean placeCrystal(BlockPos pos) {
        var crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        boolean inOffhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;

        if (!crystal.found() && !inOffhand) return false;

        if (autoSwitch.get() && crystal.found() && !inOffhand && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
            InvUtils.swap(crystal.slot(), false);
        }

        Hand hand = inOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND;

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, pos, false);

        mc.interactionManager.interactBlock(mc.player, hand, result);
        return true;
    }
}
