package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class xAnchor extends Module {

    private static final Color SIDE_COLOR    = new Color(160, 80, 0, 55);
    private static final Color OUTLINE_COLOR = new Color(230, 140, 0, 220);
    private static final double ANCHOR_POWER = 5.0;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHotbar  = settings.createGroup("Hotbar Hub");
    private final SettingGroup sgDamage  = settings.createGroup("Damage");
    private final SettingGroup sgPredict = settings.createGroup("Prediction");
    private final SettingGroup sgRender  = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range").description("Rango de acción.")
        .defaultValue(5.0).min(1).sliderMax(6).build());

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Place Delay").defaultValue(0).min(0).sliderMax(10).build());

    private final Setting<Integer> chargeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Charge Delay").defaultValue(0).min(0).sliderMax(10).build());

    private final Setting<Integer> explodeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Explode Delay").defaultValue(0).min(0).sliderMax(10).build());

    private final Setting<Integer> multiCharge = sgGeneral.add(new IntSetting.Builder()
        .name("Multi Charge").defaultValue(4).min(1).sliderMax(4).build());

    private final Setting<Integer> multiExplode = sgGeneral.add(new IntSetting.Builder()
        .name("Multi Explode").defaultValue(3).min(1).sliderMax(5).build());

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("Air Place").description("Coloca el ancla sin bloque de soporte.")
        .defaultValue(true).build());

    private final Setting<Boolean> enableHub = sgHotbar.add(new BoolSetting.Builder()
        .name("Enable Hub").defaultValue(true).build());

    private final Setting<Integer> anchorSlot = sgHotbar.add(new IntSetting.Builder()
        .name("Anchor Slot").defaultValue(1).min(1).sliderMax(9).build());

    private final Setting<Integer> glowstoneSlot = sgHotbar.add(new IntSetting.Builder()
        .name("Glowstone Slot").defaultValue(2).min(1).sliderMax(9).build());

    private final Setting<Boolean> autoSwitch = sgHotbar.add(new BoolSetting.Builder()
        .name("Auto Switch").defaultValue(true).build());

    private final Setting<Double> minTargetDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("Min Target Damage").defaultValue(4.0).min(0).sliderMax(36).build());

    private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("Max Self Damage").defaultValue(8.0).min(0).sliderMax(36).build());

    private final Setting<Boolean> antiSuicide = sgDamage.add(new BoolSetting.Builder()
        .name("Anti Suicide").defaultValue(true).build());

    private final Setting<Double> minSelfHealth = sgDamage.add(new DoubleSetting.Builder()
        .name("Min Self Health").defaultValue(6.0).min(0).sliderMax(20).build());

    private final Setting<Boolean> smartPosition = sgDamage.add(new BoolSetting.Builder()
        .name("Smart Position").defaultValue(true).build());

    private final Setting<Boolean> enablePrediction = sgPredict.add(new BoolSetting.Builder()
        .name("Enable Prediction").defaultValue(true).build());

    private final Setting<Integer> predictTicks = sgPredict.add(new IntSetting.Builder()
        .name("Predict Ticks").defaultValue(3).min(1).sliderMax(10).build());

    private final Setting<Boolean> predictFallback = sgPredict.add(new BoolSetting.Builder()
        .name("Fallback to Current Pos").defaultValue(true).build());

    private final Setting<Boolean> renderAnchor = sgRender.add(new BoolSetting.Builder()
        .name("Render Anchor Pos").defaultValue(true).build());

    // ─── Estado interno ───────────────────────────────────────────────────────
    private int placeTimer   = 0;
    private int chargeTimer  = 0;
    private int explodeTimer = 0;
    private BlockPos renderPos = null;

    // Posiciones ya usadas (colocadas+explotadas o abandonadas) — fuerza rotación
    private final Set<BlockPos> usedPositions = new HashSet<>();
    // Ticks que llevamos con el ancla actual sin poder explotarla
    private int stuckTimer = 0;
    // Si llevamos STUCK_THRESHOLD ticks sin explotar, descartamos esa posición
    private static final int STUCK_THRESHOLD = 10;

    public xAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor",
            "Anchor Aura con hub, damage calc, predicción y rotación de posición ");
    }

    @Override public void onActivate() {
        placeTimer = 0; chargeTimer = 0; explodeTimer = 0;
        renderPos = null; usedPositions.clear(); stuckTimer = 0;
    }
    @Override public void onDeactivate() { renderPos = null; usedPositions.clear(); }

    // ─── TICK ─────────────────────────────────────────────────────────────────
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        String dim = mc.world.getRegistryKey().getValue().getPath();
        if (dim.contains("the_nether")) return;

        if (enableHub.get()) manageHotbar();

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestHealth);
        if (target == null) { renderPos = null; return; }

        BlockPos predictedPos = getPredictedBlockPos(target);

        // Buscar ancla existente que NO esté marcada como usada
        BlockPos anchorPos = findAnchor(predictedPos);
        if (anchorPos == null && predictFallback.get() && enablePrediction.get())
            anchorPos = findAnchor(target.getBlockPos());

        if (anchorPos != null) {
            renderPos = anchorPos;
            int charges = mc.world.getBlockState(anchorPos).get(RespawnAnchorBlock.CHARGES);

            if (charges < RespawnAnchorBlock.MAX_CHARGES) {
                stuckTimer++;
                // Demasiado tiempo cargando sin explotar → descartar y buscar nueva pos
                if (stuckTimer >= STUCK_THRESHOLD) {
                    usedPositions.add(anchorPos);
                    stuckTimer = 0;
                    return;
                }
                if (chargeTimer <= 0) {
                    for (int i = 0; i < multiCharge.get(); i++) chargeAnchor(anchorPos);
                    chargeTimer = chargeDelay.get();
                } else {
                    chargeTimer--;
                }
            } else {
                // Cargada — comprobar si merece explotar
                Vec3d anchorCenter = Vec3d.ofCenter(anchorPos);
                float tDmg = calcDamage(target,    anchorCenter);
                float sDmg = calcDamage(mc.player, anchorCenter);

                boolean safe = tDmg >= minTargetDamage.get() && sDmg <= maxSelfDamage.get();
                if (antiSuicide.get() && safe) {
                    float hpAfter = mc.player.getHealth() + mc.player.getAbsorptionAmount() - sDmg;
                    safe = hpAfter >= minSelfHealth.get();
                }

                if (safe) {
                    if (explodeTimer <= 0) {
                        for (int i = 0; i < multiExplode.get(); i++) explodeAnchor(anchorPos);
                        // Marcar como usada TRAS explotar → siguiente tick busca nueva pos
                        usedPositions.add(anchorPos);
                        stuckTimer = 0;
                        explodeTimer = explodeDelay.get();
                    } else {
                        explodeTimer--;
                    }
                } else {
                    // No es seguro explotar → descartar esta posición
                    usedPositions.add(anchorPos);
                    stuckTimer = 0;
                }
            }
        } else {
            // ── Sin ancla válida → colocar nueva ──────────────────────────────
            stuckTimer = 0;
            if (placeTimer <= 0) {
                PlaceData best = findBestPlacePos(target, predictedPos);
                if (best != null) {
                    renderPos = best.pos;
                    placeAnchor(best.pos);
                    placeTimer = placeDelay.get();
                } else {
                    // Sin candidatos posibles: resetear usedPositions para no bloquearse
                    usedPositions.clear();
                    renderPos = null;
                }
            } else {
                placeTimer--;
            }
        }
    }

    // ─── RENDER 3D ────────────────────────────────────────────────────────────
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderAnchor.get() || renderPos == null) return;
        event.renderer.box(renderPos, SIDE_COLOR, OUTLINE_COLOR, ShapeMode.Both, 0);
    }

    // ─── BUSCAR MEJOR POSICIÓN CON FALLBACKS ─────────────────────────────────
    private PlaceData findBestPlacePos(PlayerEntity target, BlockPos predictedPos) {
        List<PlaceData> c;

        c = findPlaceCandidates(target, predictedPos, 2);
        if (!c.isEmpty()) return best(c);

        if (predictFallback.get() && enablePrediction.get()) {
            c = findPlaceCandidates(target, target.getBlockPos(), 2);
            if (!c.isEmpty()) return best(c);
        }

        c = findPlaceCandidates(target, target.getBlockPos(), 3);
        if (!c.isEmpty()) return best(c);

        c = findPlaceCandidates(target, mc.player.getBlockPos(), 2);
        if (!c.isEmpty()) return best(c);

        return null;
    }

    private PlaceData best(List<PlaceData> list) {
        if (!smartPosition.get()) return list.get(0);
        return list.stream().max(Comparator.comparingDouble(PlaceData::score)).orElse(null);
    }

    // ─── CÁLCULO DE DAÑO ──────────────────────────────────────────────────────
    private float calcDamage(LivingEntity entity, Vec3d explosionPos) {
        Vec3d entityPos = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() / 2.0, entity.getZ());
        double dist = entityPos.distanceTo(explosionPos);
        double radius = ANCHOR_POWER * 2.0;
        if (dist > radius) return 0f;
        double exposure = 1.0 - (dist / radius);
        float damage = (float) ((exposure * exposure + exposure) / 2.0 * 7.0 * radius + 1.0);
        damage *= (1.0f - Math.min(20.0f, entity.getArmor()) / 25.0f);
        return Math.max(0f, damage);
    }

    // ─── CANDIDATOS — excluye posiciones ya usadas ────────────────────────────
    private record PlaceData(BlockPos pos, float targetDmg, float selfDmg) {
        double score() { return targetDmg - selfDmg * 0.5; }
    }

    private List<PlaceData> findPlaceCandidates(PlayerEntity target, BlockPos origin, int radius) {
        List<PlaceData> list = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(
                origin.add(-radius, -1, -radius),
                origin.add( radius,  1,  radius))) {

            if (!mc.world.isAir(pos)) continue;
            if (PlayerUtils.distanceTo(pos) > range.get()) continue;
            if (usedPositions.contains(pos.toImmutable())) continue;

            BlockPos below = pos.down();
            boolean hasSolid = mc.world.getBlockState(below).isSolidBlock(mc.world, below);
            if (!hasSolid && !airPlace.get()) continue;

            Vec3d center = Vec3d.ofCenter(pos);
            float tDmg = calcDamage(target,    center);
            float sDmg = calcDamage(mc.player, center);

            if (tDmg < minTargetDamage.get()) continue;
            if (sDmg > maxSelfDamage.get())   continue;
            if (antiSuicide.get()) {
                float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount() - sDmg;
                if (hp < minSelfHealth.get()) continue;
            }
            list.add(new PlaceData(pos.toImmutable(), tDmg, sDmg));
        }
        return list;
    }

    // ─── BUSCAR ANCLA EXISTENTE — excluye usedPositions ──────────────────────
    private BlockPos findAnchor(BlockPos origin) {
        for (BlockPos pos : BlockPos.iterate(origin.add(-2, -1, -2), origin.add(2, 2, 2))) {
            if (mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) continue;
            if (PlayerUtils.distanceTo(pos) > range.get()) continue;
            if (usedPositions.contains(pos.toImmutable())) continue;
            return pos.toImmutable();
        }
        return null;
    }

    // ─── HOTBAR HUB ───────────────────────────────────────────────────────────
    private void manageHotbar() {
        if (mc.player == null) return;
        int aSlot = anchorSlot.get() - 1;
        int gSlot = glowstoneSlot.get() - 1;
        if (mc.player.getInventory().getStack(aSlot).getItem() != Items.RESPAWN_ANCHOR) {
            var a = InvUtils.find(Items.RESPAWN_ANCHOR);
            if (a.found() && a.slot() != aSlot) InvUtils.move().from(a.slot()).toHotbar(aSlot);
        }
        if (mc.player.getInventory().getStack(gSlot).getItem() != Items.GLOWSTONE) {
            var g = InvUtils.find(Items.GLOWSTONE);
            if (g.found() && g.slot() != gSlot) InvUtils.move().from(g.slot()).toHotbar(gSlot);
        }
    }

    private void placeAnchor(BlockPos pos) {
        int aSlot = anchorSlot.get() - 1;
        if (enableHub.get() && mc.player.getInventory().getStack(aSlot).getItem() == Items.RESPAWN_ANCHOR) {
            if (autoSwitch.get()) InvUtils.swap(aSlot, false);
        } else {
            var found = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
            if (!found.found()) return;
            if (autoSwitch.get()) InvUtils.swap(found.slot(), false);
        }
        BlockPos support = pos.down();
        boolean hasSolid = mc.world.getBlockState(support).isSolidBlock(mc.world, support);
        BlockPos interactPos = hasSolid ? support : pos;
        Direction face       = hasSolid ? Direction.UP : Direction.DOWN;
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(hitVec, face, interactPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void chargeAnchor(BlockPos pos) {
        int gSlot = glowstoneSlot.get() - 1;
        if (enableHub.get() && mc.player.getInventory().getStack(gSlot).getItem() == Items.GLOWSTONE) {
            if (autoSwitch.get()) InvUtils.swap(gSlot, false);
        } else {
            var found = InvUtils.findInHotbar(Items.GLOWSTONE);
            if (!found.found()) return;
            if (autoSwitch.get()) InvUtils.swap(found.slot(), false);
        }
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(hitVec, Direction.UP, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void explodeAnchor(BlockPos pos) {
        if (mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
            int aSlot = anchorSlot.get() - 1;
            if (mc.player.getInventory().getStack(aSlot).getItem() != Items.GLOWSTONE) {
                InvUtils.swap(aSlot, false);
            } else {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() != Items.GLOWSTONE) {
                        InvUtils.swap(i, false); break;
                    }
                }
            }
        }
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(hitVec, Direction.UP, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private BlockPos getPredictedBlockPos(PlayerEntity target) {
        if (!enablePrediction.get()) return target.getBlockPos();
        Vec3d vel = target.getVelocity();
        int t = predictTicks.get();
        double px = target.getX() + vel.x * t;
        double pz = target.getZ() + vel.z * t;
        double py = target.getY(), vy = vel.y;
        for (int i = 0; i < t; i++) { vy = (vy - 0.08) * 0.98; py += vy; }
        return new BlockPos((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
    }
}
