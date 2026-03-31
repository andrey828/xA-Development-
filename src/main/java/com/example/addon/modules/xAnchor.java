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

    private static final Color FILL_COLOR    = new Color(0, 100, 160, 60);
    private static final Color OUTLINE_COLOR = new Color(0, 150, 210, 200);

    public xAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor", "Anchor Aura: Coloca, carga y explota.");
    }

    private int timer;
    private BlockPos renderPos = null;

    @Override
    public void onActivate() {
        timer = 0;
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

        if (timer > 0) {
            timer--;
            return;
        }

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
               
                chargeAnchor(anchorPos);
            } else {
       
                explodeAnchor(anchorPos);
            }
            timer = delay.get();

        } else {

            BlockPos placePos = findPlacePos(target.getBlockPos());
            if (placePos != null) {
                renderPos = placePos;
                placeAnchor(placePos);
                timer = delay.get();
            } else {
                renderPos = null;
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (renderPos == null) return;
        event.renderer.box(
            renderPos,
            FILL_COLOR,
            OUTLINE_COLOR,
            ShapeMode.Both,
            0
        );
    }

    private BlockPos findPlacePos(BlockPos targetPos) {
        for (BlockPos pos : BlockPos.iterate(targetPos.add(-2, -1, -2), targetPos.add(2, 1, 2))) {
            BlockState state = mc.world.getBlockState(pos);
            BlockPos above = pos.up();

            if (!state.isSolid()) continue;                           
            if (!mc.world.isAir(above)) continue;                   
            if (PlayerUtils.distanceTo(above) > range.get()) continue;

            return above.toImmutable(); 
        }
        return null;
    }

    // Busca un ancla ya colocada cerca del objetivo
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

    // Coloca el ancla interactuando con la cara superior del bloque de soporte
    private void placeAnchor(BlockPos pos) {
        var found = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        if (!found.found()) return;
        if (autoSwitch.get()) InvUtils.swap(found.slot(), false);

        // Interactuamos con la cara superior del bloque debajo de pos
        BlockPos support = pos.down();
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, support, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // Carga el ancla con glowstone
    private void chargeAnchor(BlockPos pos) {
        var found = InvUtils.findInHotbar(Items.GLOWSTONE);
        if (!found.found()) return;
        if (autoSwitch.get()) InvUtils.swap(found.slot(), false);

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // Explota el ancla — el jugador NO debe tener glowstone en mano
    private void explodeAnchor(BlockPos pos) {
        // Si tiene glowstone en mano, cambiar a cualquier otro slot
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
