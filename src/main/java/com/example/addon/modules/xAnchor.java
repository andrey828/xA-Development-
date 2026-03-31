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

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks entre acciones.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Cambia automáticamente de ítem.")
        .defaultValue(true)
        .build());

    public xAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor", "Anchor Aura: Coloca, carga y explota.");
    }

    private int timer;

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // FIX: Comprobamos la dimensión de forma segura. 
        // Si estamos en el Nether, el ancla NO explota, así que paramos el módulo.
        if (mc.world.getRegistryKey().getValue().getPath().contains("the_nether")) return;

        if (timer > 0) {
            timer--;
            return;
        }

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestHealth);
        if (target == null) return;

        BlockPos targetPos = target.getBlockPos();
        BlockPos anchorPos = findAnchor(targetPos);

        if (anchorPos == null) {
            // Intentar colocar ancla en la cabeza o pies del objetivo
            BlockPos placePos = targetPos.up();
            if (canPlaceAnchor(placePos)) {
                if (placeBlock(placePos, Items.RESPAWN_ANCHOR)) {
                    timer = delay.get();
                }
            }
        } else {
            int charges = mc.world.getBlockState(anchorPos).get(RespawnAnchorBlock.CHARGES);

            if (charges == 0) {
                // Cargar
                if (interactBlock(anchorPos, Items.GLOWSTONE)) {
                    timer = delay.get();
                }
            } else {
                // Explotar (usando la mano o cualquier item que no sea Glowstone)
                if (interactBlock(anchorPos, null)) {
                    timer = delay.get();
                }
            }
        }
    }

    private BlockPos findAnchor(BlockPos pos) {
        for (BlockPos checkPos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 2, 1))) {
            if (mc.world.getBlockState(checkPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                return checkPos.toImmutable();
            }
        }
        return null;
    }

    private boolean canPlaceAnchor(BlockPos pos) {
        return (mc.world.getBlockState(pos).isReplaceable() || mc.world.isAir(pos)) && PlayerUtils.distanceTo(pos) <= range.get();
    }

    private boolean placeBlock(BlockPos pos, net.minecraft.item.Item item) {
        var findItem = InvUtils.findInHotbar(item);
        if (!findItem.found()) return false;
        if (autoSwitch.get()) InvUtils.swap(findItem.slot(), false);

        BlockHitResult hit = new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        return true;
    }

    private boolean interactBlock(BlockPos pos, net.minecraft.item.Item item) {
        if (item != null) {
            var findItem = InvUtils.findInHotbar(item);
            if (!findItem.found()) return false;
            if (autoSwitch.get()) InvUtils.swap(findItem.slot(), false);
        }

        BlockHitResult hit = new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        return true;
    }
}
