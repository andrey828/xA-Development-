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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

public class xCrystal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Target Range")
        .description("Distancia para detectar enemigos.")
        .defaultValue(10.0)
        .min(1)
        .sliderMax(15)
        .build());

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Place Range")
        .description("Distancia máxima para colocar cristal/obsidiana.")
        .defaultValue(4.5)
        .min(1)
        .sliderMax(6)
        .build());

    private final Setting<Double> explodeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Explode Range")
        .description("Distancia máxima para explotar cristales.")
        .defaultValue(5.0)
        .min(1)
        .sliderMax(8)
        .build());

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Place Delay")
        .description("Ticks entre cada colocación.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build());

    private final Setting<Integer> explodeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Explode Delay")
        .description("Ticks entre cada explosión.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Switch")
        .description("Cambia al cristal/obsidiana automáticamente.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> placeObsidian = sgGeneral.add(new BoolSetting.Builder()
        .name("Place Obsidian")
        .description("Coloca obsidiana automáticamente donde se necesite.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> onlyExplodeNear = sgGeneral.add(new BoolSetting.Builder()
        .name("Only Explode Near Target")
        .description("Solo explota cristales cercanos al objetivo.")
        .defaultValue(true)
        .build());

    private int placeTimer = 0;
    private int explodeTimer = 0;
    private int prevSlot = -1;

    public xCrystal() {
        super(AddonTemplate.CATEGORY, "xCrystal", "Crystal Aura con Auto Obsidian para 1.21.x");
    }

    @Override
    public void onActivate() {
        placeTimer = 0;
        explodeTimer = 0;
        prevSlot = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestHealth);

        if (explodeTimer <= 0) {
            for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(
                    EndCrystalEntity.class,
                    mc.player.getBoundingBox().expand(explodeRange.get()),
                    e -> true)) {

                if (onlyExplodeNear.get() && target != null) {
                    if (crystal.distanceTo(target) > placeRange.get() + 2) continue;
                }

                if (mc.player.distanceTo(crystal) > explodeRange.get()) continue;

                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(Hand.MAIN_HAND);
                explodeTimer = explodeDelay.get();
                break;
            }
        } else {
            explodeTimer--;
        }

        if (target == null) return;

        if (placeTimer <= 0) {
            BlockPos placePos = findPlacePos(target);

            if (placePos != null) {
                var baseBlock = mc.world.getBlockState(placePos).getBlock();
                boolean needsObsidian = baseBlock != Blocks.OBSIDIAN && baseBlock != Blocks.BEDROCK;

                if (needsObsidian && placeObsidian.get()) {
                    if (placeObsidianAt(placePos)) {
                        placeTimer = placeDelay.get();
                    }
                } else if (!needsObsidian) {
                    if (placeCrystal(placePos)) {
                        placeTimer = placeDelay.get();
                    }
                }
            }
        } else {
            placeTimer--;
        }
    }

    private BlockPos findPlacePos(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        BlockPos bestCrystalPos = null;
        BlockPos bestObsidianPos = null;
        double bestCrystalDist = Double.MAX_VALUE;
        double bestObsidianDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(targetPos.add(-2, -1, -2), targetPos.add(2, 1, 2))) {
            if (PlayerUtils.distanceTo(pos) > placeRange.get()) continue;

            var block = mc.world.getBlockState(pos).getBlock();

            if ((block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) && mc.world.isAir(pos.up())) {
                Box checkBox = new Box(pos.up());
                if (mc.world.getEntitiesByClass(EndCrystalEntity.class, checkBox, e -> true).isEmpty()) {
                    double dist = target.getBlockPos().getSquaredDistance(pos);
                    if (dist < bestCrystalDist) {
                        bestCrystalDist = dist;
                        bestCrystalPos = pos.toImmutable();
                    }
                }
            }

            if (placeObsidian.get() && block == Blocks.AIR) {
                BlockPos below = pos.down();
                if (mc.world.getBlockState(below).isSolidBlock(mc.world, below) && mc.world.isAir(pos.up())) {
                    double dist = target.getBlockPos().getSquaredDistance(pos);
                    if (dist < bestObsidianDist) {
                        bestObsidianDist = dist;
                        bestObsidianPos = pos.toImmutable();
                    }
                }
            }
        }

        return bestCrystalPos != null ? bestCrystalPos : bestObsidianPos;
    }

    private boolean placeObsidianAt(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return false;

        var obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found()) {
            var obsidianInv = InvUtils.find(Items.OBSIDIAN);
            if (!obsidianInv.found()) return false;
            int empty = getEmptyHotbarSlot();
            if (empty == -1) empty = mc.player.getInventory().getSelectedSlot();
            InvUtils.move().from(obsidianInv.slot()).toHotbar(empty);
            obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
            if (!obsidian.found()) return false;
        }

        if (autoSwitch.get()) {
            prevSlot = mc.player.getInventory().getSelectedSlot();
            InvUtils.swap(obsidian.slot(), false);
        }

        Hand hand = Hand.MAIN_HAND;
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, pos.down(), false);

        mc.interactionManager.interactBlock(mc.player, hand, result);
        mc.player.swingHand(hand);

        if (autoSwitch.get() && prevSlot != -1) {
            InvUtils.swap(prevSlot, false);
            prevSlot = -1;
        }

        return true;
    }

    private boolean placeCrystal(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return false;

        boolean inOffhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        var crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);

        if (!crystal.found() && !inOffhand) return false;

        if (autoSwitch.get() && crystal.found() && !inOffhand
                && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
            InvUtils.swap(crystal.slot(), false);
        }

        Hand hand = inOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, pos, false);

        mc.interactionManager.interactBlock(mc.player, hand, result);
        mc.player.swingHand(hand);
        return true;
    }

    private int getEmptyHotbarSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }
}
