package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
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
        .description("Rango de acción.")
        .defaultValue(5.0)
        .min(1)
        .sliderMax(6)
        .build());

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Ticks entre colocaciones.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());

    private final Setting<Integer> chargeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("charge-delay")
        .description("Ticks entre cargas.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());

    private final Setting<Integer> explodeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("explode-delay")
        .description("Ticks entre explosiones.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Cambia automáticamente de ítem.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Coloca el ancla en el aire sin bloque de soporte.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> multiCharge = sgGeneral.add(new IntSetting.Builder()
        .name("multi-charge")
        .description("Cargas por tick (más = más rápido).")
        .defaultValue(4)
        .min(1)
        .sliderMax(4)
        .build());

    private final Setting<Integer> multiExplode = sgGeneral.add(new IntSetting.Builder()
        .name("multi-explode")
        .description("Explosiones por tick.")
        .defaultValue(3)
        .min(1)
        .sliderMax(5)
        .build());

    private static final Color FILL_COLOR    = new Color(0, 100, 160, 60);
    private static final Color OUTLINE_COLOR = new Color(0, 150, 210, 200);

    private int placeTimer = 0;
    private int chargeTimer = 0;
    private int explodeTimer = 0;
    private BlockPos renderPos = null;

    public xAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor", "Anchor Aura: Coloca, carga y explota.");
    }

    @Override
    public void onActivate() {
        placeTimer = 0;
        chargeTimer = 0;
        explodeTimer = 0;
        renderPos = null;
    }

    @Override
    public void onDeactivate() {
        renderPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.world.getRegistryKey().getValue().getPath().contains("the_nether")) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestHealth);
        if (target == null) {
            renderPos = null;
            return;
        }

        BlockPos anchorPos = findAnchor(target.getBlockPos());

        if (anchorPos != null) {
            renderPos = anchorPos;
            int charges = mc.world.getBlockState(anchorPos).get(RespawnAnchorBlock.CHARGES);

            if (charges < RespawnAnchorBlock.MAX_CHARGES) {
                if (chargeTimer <= 0) {
                    for (int i = 0; i < multiCharge.get(); i++) {
                        chargeAnchor(anchorPos);
                    }
                    chargeTimer = chargeDelay.get();
                } else {
                    chargeTimer--;
                }
            } else {
                if (explodeTimer <= 0) {
                    for (int i = 0; i < multiExplode.get(); i++) {
                        explodeAnchor(anchorPos);
                    }
                    explodeTimer = explodeDelay.get();
                } else {
                    explodeTimer--;
                }
            }
        } else {
            if (placeTimer <= 0) {
                BlockPos placePos = findPlacePos(target.getBlockPos());
                if (placePos != null) {
                    renderPos = placePos;
                    placeAnchor(placePos);
                    placeTimer = placeDelay.get();
                } else {
                    renderPos = null;
                }
            } else {
                placeTimer--;
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (renderPos == null) return;
        event.renderer.box(renderPos, FILL_COLOR, OUTLINE_COLOR, ShapeMode.Both, 0);
    }

    private BlockPos findPlacePos(BlockPos targetPos) {
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(targetPos.add(-2, -1, -2), targetPos.add(2, 1, 2))) {
            if (!mc.world.isAir(pos)) continue;
            if (PlayerUtils.distanceTo(pos) > range.get()) continue;

            BlockPos below = pos.down();
            boolean hasSolidBelow = mc.world.getBlockState(below).isSolidBlock(mc.world, below);

            if (!hasSolidBelow && !airPlace.get()) continue;

            double dist = targetPos.getSquaredDistance(pos);
            if (dist < bestDist) {
                bestDist = dist;
                bestPos = pos.toImmutable();
            }
        }
        return bestPos;
    }

    private BlockPos findAnchor(BlockPos targetPos) {
        for (BlockPos pos : BlockPos.iterate(targetPos.add(-2, -1, -2), targetPos.add(2, 2, 2))) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (PlayerUtils.distanceTo(pos) <= range.get()) {
                    return pos.toImmutable();
                }
            }
        }
        return null;
    }

    private void placeAnchor(BlockPos pos) {
        var found = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        if (!found.found()) return;
        if (autoSwitch.get()) InvUtils.swap(found.slot(), false);

        BlockPos support = pos.down();
        boolean hasSolid = mc.world.getBlockState(support).isSolidBlock(mc.world, support);

        BlockPos interactPos = hasSolid ? support : pos;
        Direction face = hasSolid ? Direction.UP : Direction.DOWN;
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, face, interactPos, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void chargeAnchor(BlockPos pos) {
        var found = InvUtils.findInHotbar(Items.GLOWSTONE);
        if (!found.found()) return;
        if (autoSwitch.get()) InvUtils.swap(found.slot(), false);

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void explodeAnchor(BlockPos pos) {
        if (mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() != Items.GLOWSTONE) {
                    InvUtils.swap(i, false);
                    break;
                }
            }
        }

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
