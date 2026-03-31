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
        .description("Distancia máxima para colocar el cristal.")
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
        .description("Cambia al cristal automáticamente.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> onlyExplodeNear = sgGeneral.add(new BoolSetting.Builder()
        .name("Only Explode Near Target")
        .description("Solo explota cristales cercanos al objetivo.")
        .defaultValue(true)
        .build());

    private int placeTimer = 0;
    private int explodeTimer = 0;

    public xCrystal() {
        super(AddonTemplate.CATEGORY, "xCrystal", "Crystal Aura funcional para 1.21.x");
    }

    @Override
    public void onActivate() {
        placeTimer = 0;
        explodeTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestHealth);

        // --- EXPLOTAR ---
        if (explodeTimer <= 0) {
            for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(
                    EndCrystalEntity.class,
                    mc.player.getBoundingBox().expand(explodeRange.get()),
                    e -> true)) {

                // Si onlyExplodeNear está activo, solo explotar cristales cerca del objetivo
                if (onlyExplodeNear.get() && target != null) {
                    if (crystal.distanceTo(target) > placeRange.get() + 2) continue;
                }

                double dist = mc.player.distanceTo(crystal);
                if (dist > explodeRange.get()) continue;

                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(Hand.MAIN_HAND);
                explodeTimer = explodeDelay.get();
                break; // un cristal por tick para no spamear
            }
        } else {
            explodeTimer--;
        }

        if (target == null) return;

        // --- COLOCAR ---
        if (placeTimer <= 0) {
            BlockPos placePos = findPlacePos(target);
            if (placePos != null && placeCrystal(placePos)) {
                placeTimer = placeDelay.get();
            }
        } else {
            placeTimer--;
        }
    }

    private BlockPos findPlacePos(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        BlockPos bestPos = null;
        double bestDmg = -1;

        for (BlockPos pos : BlockPos.iterate(targetPos.add(-2, -1, -2), targetPos.add(2, 1, 2))) {
            if (!canPlaceCrystal(pos)) continue;
            if (PlayerUtils.distanceTo(pos) > placeRange.get()) continue;

            // Elige la posición más cercana al objetivo para maximizar daño
            double dist = target.getBlockPos().getSquaredDistance(pos);
            if (bestPos == null || dist < bestDmg) {
                bestDmg = dist;
                bestPos = pos.toImmutable();
            }
        }
        return bestPos;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        if (mc.world == null) return false;
        var block = mc.world.getBlockState(pos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) return false;
        if (!mc.world.isAir(pos.up())) return false;

        // Verifica que no haya ya un cristal encima
        Box checkBox = new Box(pos.up());
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, checkBox, e -> true).isEmpty();
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
}
