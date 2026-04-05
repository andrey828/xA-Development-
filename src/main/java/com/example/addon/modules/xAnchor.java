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
import java.util.List;

public class xAnchor extends Module {

    // ─── Colores del render (naranja oscuro — diferencia visual con xCrystal) ──
    private static final Color SIDE_COLOR    = new Color(160, 80, 0, 55);
    private static final Color OUTLINE_COLOR = new Color(230, 140, 0, 220);

    // ─── Power de explosión del Respawn Anchor (igual que cama) ───────────────
    private static final double ANCHOR_POWER = 5.0;

    // ─── Grupos de settings ───────────────────────────────────────────────────
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHotbar  = settings.createGroup("Hotbar Hub");
    private final SettingGroup sgDamage  = settings.createGroup("Damage");
    private final SettingGroup sgPredict = settings.createGroup("Prediction");
    private final SettingGroup sgRender  = settings.createGroup("Render");

    // ── General ───────────────────────────────────────────────────────────────
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range").description("Rango de acción.")
        .defaultValue(5.0).min(1).sliderMax(6).build());

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Place Delay").description("Ticks entre colocaciones.")
        .defaultValue(0).min(0).sliderMax(10).build());

    private final Setting<Integer> chargeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Charge Delay").description("Ticks entre cargas.")
        .defaultValue(0).min(0).sliderMax(10).build());

    private final Setting<Integer> explodeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Explode Delay").description("Ticks entre explosiones.")
        .defaultValue(0).min(0).sliderMax(10).build());

    private final Setting<Integer> multiCharge = sgGeneral.add(new IntSetting.Builder()
        .name("Multi Charge").description("Cargas por tick.")
        .defaultValue(4).min(1).sliderMax(4).build());

    private final Setting<Integer> multiExplode = sgGeneral.add(new IntSetting.Builder()
        .name("Multi Explode").description("Explosiones por tick.")
        .defaultValue(3).min(1).sliderMax(5).build());

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("Air Place").description("Coloca el ancla sin bloque de soporte.")
        .defaultValue(true).build());

    // ── Hotbar Hub ────────────────────────────────────────────────────────────
    private final Setting<Boolean> enableHub = sgHotbar.add(new BoolSetting.Builder()
        .name("Enable Hub")
        .description("Mueve Respawn Anchors y Glowstone a slots fijos automáticamente.")
        .defaultValue(true).build());

    private final Setting<Integer> anchorSlot = sgHotbar.add(new IntSetting.Builder()
        .name("Anchor Slot").description("Slot del hotbar para Respawn Anchor (1-9).")
        .defaultValue(1).min(1).sliderMax(9).build());

    private final Setting<Integer> glowstoneSlot = sgHotbar.add(new IntSetting.Builder()
        .name("Glowstone Slot").description("Slot del hotbar para Glowstone (1-9).")
        .defaultValue(2).min(1).sliderMax(9).build());

    private final Setting<Boolean> autoSwitch = sgHotbar.add(new BoolSetting.Builder()
        .name("Auto Switch").description("Cambia al slot correcto antes de usar el ítem.")
        .defaultValue(true).build());

    // ── Damage ────────────────────────────────────────────────────────────────
    private final Setting<Double> minTargetDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("Min Target Damage").description("Daño mínimo al objetivo para explotar.")
        .defaultValue(4.0).min(0).sliderMax(36).build());

    private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("Max Self Damage").description("Daño propio máximo permitido.")
        .defaultValue(8.0).min(0).sliderMax(36).build());

    private final Setting<Boolean> antiSuicide = sgDamage.add(new BoolSetting.Builder()
        .name("Anti Suicide").description("No explota si te dejaría con poca vida.")
        .defaultValue(true).build());

    private final Setting<Double> minSelfHealth = sgDamage.add(new DoubleSetting.Builder()
        .name("Min Self Health").description("Vida mínima para permitir una explosión.")
        .defaultValue(6.0).min(0).sliderMax(20).build());

    private final Setting<Boolean> smartPosition = sgDamage.add(new BoolSetting.Builder()
        .name("Smart Position")
        .description("Elige la posición que maximiza daño al objetivo con menor riesgo propio.")
        .defaultValue(true).build());

    // ── Predicción ────────────────────────────────────────────────────────────
    private final Setting<Boolean> enablePrediction = sgPredict.add(new BoolSetting.Builder()
        .name("Enable Prediction").description("Predice la posición futura del objetivo.")
        .defaultValue(true).build());

    private final Setting<Integer> predictTicks = sgPredict.add(new IntSetting.Builder()
        .name("Predict Ticks").description("Ticks adelante a predecir.")
        .defaultValue(3).min(1).sliderMax(10).build());

    private final Setting<Boolean> predictFallback = sgPredict.add(new BoolSetting.Builder()
        .name("Fallback to Current Pos")
        .description("Si la posición predicha no tiene lugares, usa la actual.")
        .defaultValue(true).build());

    // ── Render ────────────────────────────────────────────────────────────────
    private final Setting<Boolean> renderAnchor = sgRender.add(new BoolSetting.Builder()
        .name("Render Anchor Pos").description("Pinta en naranja el bloque de colocación/ancla.")
        .defaultValue(true).build());

    // ─── Estado interno ───────────────────────────────────────────────────────
    private int placeTimer   = 0;
    private int chargeTimer  = 0;
    private int explodeTimer = 0;
    private BlockPos renderPos = null;

    public xAnchor() {
        super(AddonTemplate.CATEGORY, "xAnchor", "Anchor Aura con hub, damage calc y predicción — 1.21.x");
    }

    @Override public void onActivate()   { placeTimer = 0; chargeTimer = 0; explodeTimer = 0; renderPos = null; }
    @Override public void onDeactivate() { renderPos = null; }

    // ─── TICK ─────────────────────────────────────────────────────────────────
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Respawn Anchor SOLO explota fuera del Nether
        String dim = mc.world.getRegistryKey().getValue().getPath();
        if (dim.contains("the_nether")) return;

        // Hub de hotbar
        if (enableHub.get()) manageHotbar();

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestHealth);
        if (target == null) { renderPos = null; return; }

        // Predecir posición del objetivo
        BlockPos predictedPos = getPredictedBlockPos(target);

        // Buscar ancla ya colocada cerca del target (predicho o actual)
        BlockPos anchorPos = findAnchor(predictedPos);
        if (anchorPos == null && predictFallback.get() && enablePrediction.get())
            anchorPos = findAnchor(target.getBlockPos());

        if (anchorPos != null) {
            // ── Ancla encontrada → cargar o explotar ──────────────────────────
            renderPos = anchorPos;
            int charges = mc.world.getBlockState(anchorPos).get(RespawnAnchorBlock.CHARGES);

            if (charges < RespawnAnchorBlock.MAX_CHARGES) {
                // Necesita carga
                if (chargeTimer <= 0) {
                    for (int i = 0; i < multiCharge.get(); i++) chargeAnchor(anchorPos);
                    chargeTimer = chargeDelay.get();
                } else {
                    chargeTimer--;
                }
            } else {
                // Lista para explotar — verificar daños
                Vec3d anchorCenter = Vec3d.ofCenter(anchorPos);
                float tDmg = calcDamage(target,    anchorCenter);
                float sDmg = calcDamage(mc.player, anchorCenter);

                boolean safeToExplode = tDmg >= minTargetDamage.get()
                    && sDmg <= maxSelfDamage.get();

                if (antiSuicide.get() && safeToExplode) {
                    float hpAfter = mc.player.getHealth() + mc.player.getAbsorptionAmount() - sDmg;
                    safeToExplode = hpAfter >= minSelfHealth.get();
                }

                if (safeToExplode) {
                    if (explodeTimer <= 0) {
                        for (int i = 0; i < multiExplode.get(); i++) explodeAnchor(anchorPos);
                        explodeTimer = explodeDelay.get();
                    } else {
                        explodeTimer--;
                    }
                }
            }
        } else {
            // ── No hay ancla → buscar lugar y colocar ─────────────────────────
            if (placeTimer <= 0) {
                PlaceData best = findBestPlacePos(target, predictedPos);
                if (best != null) {
                    renderPos = best.pos;
                    placeAnchor(best.pos);
                    placeTimer = placeDelay.get();
                } else {
                    renderPos = null;
                }
            } else {
                placeTimer--;
            }
        }
    }

    // ─── BUSCAR MEJOR POSICIÓN CON FALLBACKS PROGRESIVOS ─────────────────────
    /**
     * Orden de intento:
     * 1. Posición predicha del target, radio normal (±2)
     * 2. Posición actual del target, radio normal (±2)          [si prediction fallback ON]
     * 3. Posición actual del target, radio ampliado (±3)        [último recurso target]
     * 4. Posición del propio jugador, radio normal (±2)         [último recurso player]
     *
     * En cada paso se elige el candidato con mayor score (daño target − daño propio × 0.5).
     */
    private PlaceData findBestPlacePos(PlayerEntity target, BlockPos predictedPos) {
        // Intento 1: posición predicha
        List<PlaceData> candidates = findPlaceCandidates(target, predictedPos, 2);
        if (!candidates.isEmpty()) return best(candidates);

        if (predictFallback.get() && enablePrediction.get()) {
            // Intento 2: posición actual, radio normal
            candidates = findPlaceCandidates(target, target.getBlockPos(), 2);
            if (!candidates.isEmpty()) return best(candidates);
        }

        // Intento 3: posición actual, radio ampliado (+3)
        candidates = findPlaceCandidates(target, target.getBlockPos(), 3);
        if (!candidates.isEmpty()) return best(candidates);

        // Intento 4: alrededor del propio jugador como último recurso
        candidates = findPlaceCandidates(target, mc.player.getBlockPos(), 2);
        if (!candidates.isEmpty()) return best(candidates);

        return null; // no hay ningún lugar válido
    }

    private PlaceData best(List<PlaceData> list) {
        if (!smartPosition.get()) return list.get(0);
        return list.stream().max(Comparator.comparingDouble(PlaceData::score)).orElse(null);
    }

    // ─── RENDER 3D ────────────────────────────────────────────────────────────
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderAnchor.get() || renderPos == null) return;
        event.renderer.box(renderPos, SIDE_COLOR, OUTLINE_COLOR, ShapeMode.Both, 0);
    }

    // ─── CÁLCULO DE DAÑO ──────────────────────────────────────────────────────
    private float calcDamage(LivingEntity entity, Vec3d explosionPos) {
        Vec3d entityPos = new Vec3d(
            entity.getX(),
            entity.getY() + entity.getHeight() / 2.0,
            entity.getZ()
        );
        double dist = entityPos.distanceTo(explosionPos);
        double radius = ANCHOR_POWER * 2.0;
        if (dist > radius) return 0f;

        double exposure = 1.0 - (dist / radius);
        float damage = (float) ((exposure * exposure + exposure) / 2.0 * 7.0 * radius + 1.0);

        int armor = entity.getArmor();
        damage *= (1.0f - Math.min(20.0f, armor) / 25.0f);
        return Math.max(0f, damage);
    }

    // ─── CANDIDATOS PARA COLOCAR ANCLA ───────────────────────────────────────
    private record PlaceData(BlockPos pos, float targetDmg, float selfDmg) {
        double score() { return targetDmg - selfDmg * 0.5; }
    }

    /**
     * @param radius Radio de búsqueda alrededor de origin (normalmente 2, fallback 3).
     */
    private List<PlaceData> findPlaceCandidates(PlayerEntity target, BlockPos origin, int radius) {
        List<PlaceData> list = new ArrayList<>();

        for (BlockPos pos : BlockPos.iterate(
                origin.add(-radius, -1, -radius),
                origin.add( radius,  1,  radius))) {

            if (!mc.world.isAir(pos)) continue;
            if (PlayerUtils.distanceTo(pos) > range.get()) continue;

            BlockPos below = pos.down();
            boolean hasSolid = mc.world.getBlockState(below).isSolidBlock(mc.world, below);
            if (!hasSolid && !airPlace.get()) continue;

            Vec3d anchorCenter = Vec3d.ofCenter(pos);
            float tDmg = calcDamage(target,    anchorCenter);
            float sDmg = calcDamage(mc.player, anchorCenter);

            if (tDmg < minTargetDamage.get()) continue;
            if (sDmg > maxSelfDamage.get())   continue;
            if (antiSuicide.get()) {
                float hpAfter = mc.player.getHealth() + mc.player.getAbsorptionAmount() - sDmg;
                if (hpAfter < minSelfHealth.get()) continue;
            }

            list.add(new PlaceData(pos.toImmutable(), tDmg, sDmg));
        }
        return list;
    }

    // ─── BUSCAR ANCLA EXISTENTE ───────────────────────────────────────────────
    private BlockPos findAnchor(BlockPos origin) {
        for (BlockPos pos : BlockPos.iterate(origin.add(-2, -1, -2), origin.add(2, 2, 2))) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR
                && PlayerUtils.distanceTo(pos) <= range.get()) {
                return pos.toImmutable();
            }
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

    // ─── COLOCAR ANCLA ────────────────────────────────────────────────────────
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

    // ─── CARGAR ANCLA ─────────────────────────────────────────────────────────
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

    // ─── EXPLOTAR ANCLA ───────────────────────────────────────────────────────
    private void explodeAnchor(BlockPos pos) {
        if (mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
            int aSlot = anchorSlot.get() - 1;
            if (mc.player.getInventory().getStack(aSlot).getItem() != Items.GLOWSTONE) {
                InvUtils.swap(aSlot, false);
            } else {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() != Items.GLOWSTONE) {
                        InvUtils.swap(i, false);
                        break;
                    }
                }
            }
        }

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(hitVec, Direction.UP, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // ─── PREDICCIÓN ───────────────────────────────────────────────────────────
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
