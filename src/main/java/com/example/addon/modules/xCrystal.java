package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class xCrystal extends Module {

    // ─── Colores del render (celeste oscuro) ──────────────────────────────────
    private static final Color SIDE_COLOR = new Color(0, 120, 160, 55);
    private static final Color LINE_COLOR = new Color(0, 200, 230, 220);

    // ─── Grupos de settings ───────────────────────────────────────────────────
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHotbar  = settings.createGroup("Hotbar Hub");
    private final SettingGroup sgDamage  = settings.createGroup("Damage");
    private final SettingGroup sgPredict = settings.createGroup("Prediction");
    private final SettingGroup sgRender  = settings.createGroup("Render");

    // ── General ───────────────────────────────────────────────────────────────
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Target Range").description("Rango de detección de enemigos.")
        .defaultValue(10.0).min(1).sliderMax(15).build());

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Place Range").description("Rango máximo para colocar.")
        .defaultValue(4.5).min(1).sliderMax(6).build());

    private final Setting<Double> explodeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("Explode Range").description("Rango máximo para explotar.")
        .defaultValue(5.0).min(1).sliderMax(8).build());

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Place Delay").description("Ticks entre colocaciones (0 = máximo).")
        .defaultValue(0).min(0).sliderMax(20).build());

    private final Setting<Integer> explodeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Explode Delay").description("Ticks entre explosiones (0 = máximo).")
        .defaultValue(0).min(0).sliderMax(10).build());

    private final Setting<Integer> multiPlace = sgGeneral.add(new IntSetting.Builder()
        .name("Multi Place").description("Cristales/obsidiana a colocar por tick.")
        .defaultValue(1).min(1).sliderMax(5).build());

    private final Setting<Boolean> placeObsidian = sgGeneral.add(new BoolSetting.Builder()
        .name("Place Obsidian").description("Coloca obsidiana automáticamente.")
        .defaultValue(true).build());

    private final Setting<Boolean> onlyExplodeNear = sgGeneral.add(new BoolSetting.Builder()
        .name("Only Explode Near Target").description("Solo explota cristales cerca del objetivo.")
        .defaultValue(true).build());

    // ── Hotbar Hub ────────────────────────────────────────────────────────────
    private final Setting<Boolean> enableHub = sgHotbar.add(new BoolSetting.Builder()
        .name("Enable Hub")
        .description("Mueve End Crystals y Obsidiana a slots fijos del hotbar automáticamente.")
        .defaultValue(true).build());

    private final Setting<Integer> crystalSlot = sgHotbar.add(new IntSetting.Builder()
        .name("Crystal Slot").description("Slot del hotbar para End Crystals (1-9).")
        .defaultValue(1).min(1).sliderMax(9).build());

    private final Setting<Integer> obsidianSlot = sgHotbar.add(new IntSetting.Builder()
        .name("Obsidian Slot").description("Slot del hotbar para Obsidiana (1-9).")
        .defaultValue(2).min(1).sliderMax(9).build());

    private final Setting<Boolean> autoSwitch = sgHotbar.add(new BoolSetting.Builder()
        .name("Auto Switch").description("Cambia al slot correcto antes de usar el ítem.")
        .defaultValue(true).build());

    // ── Damage ────────────────────────────────────────────────────────────────
    private final Setting<Float> minTargetDamage = sgDamage.add(new FloatSetting.Builder()
        .name("Min Target Damage").description("Daño mínimo al objetivo para colocar/explotar.")
        .defaultValue(4f).min(0).sliderMax(36).build());

    private final Setting<Float> maxSelfDamage = sgDamage.add(new FloatSetting.Builder()
        .name("Max Self Damage").description("Daño propio máximo permitido.")
        .defaultValue(8f).min(0).sliderMax(36).build());

    private final Setting<Boolean> antiSuicide = sgDamage.add(new BoolSetting.Builder()
        .name("Anti Suicide").description("No explota si te dejaría con poca vida.")
        .defaultValue(true).build());

    private final Setting<Float> minSelfHealth = sgDamage.add(new FloatSetting.Builder()
        .name("Min Self Health").description("Vida mínima para permitir una explosión.")
        .defaultValue(6f).min(0).sliderMax(20).build());

    private final Setting<Boolean> smartPosition = sgDamage.add(new BoolSetting.Builder()
        .name("Smart Position")
        .description("Elige la posición que maximiza daño al objetivo y minimiza daño propio.")
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
        .description("Si la posición predicha no tiene lugares, usa la posición actual.")
        .defaultValue(true).build());

    // ── Render ────────────────────────────────────────────────────────────────
    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
        .name("Render Place Pos").description("Pinta en celeste oscuro el bloque de colocación.")
        .defaultValue(true).build());

    // ─── Estado interno ───────────────────────────────────────────────────────
    private int placeTimer   = 0;
    private int explodeTimer = 0;
    private int prevSlot     = -1;
    private BlockPos renderPos = null;

    public xCrystal() {
        super(AddonTemplate.CATEGORY, "xCrystal",
            "Crystal Aura con hub, damage calc, anti-suicide y predicción — 1.21.x");
    }

    @Override public void onActivate()   { placeTimer = 0; explodeTimer = 0; prevSlot = -1; renderPos = null; }
    @Override public void onDeactivate() { renderPos = null; }

    // ─── TICK ─────────────────────────────────────────────────────────────────
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 1. Gestionar hotbar hub cada tick
        if (enableHub.get()) manageHotbar();

        PlayerEntity target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestHealth);

        // 2. Explotar cristal con mejor score de daño
        if (explodeTimer <= 0) {
            if (explodeBestCrystal(target)) explodeTimer = explodeDelay.get();
        } else {
            explodeTimer--;
        }

        if (target == null) { renderPos = null; return; }

        // 3. Colocar cristal/obsidiana
        if (placeTimer <= 0) {
            BlockPos predictedPos = getPredictedBlockPos(target);
            int placed = placeBest(target, predictedPos);

            if (placed == 0 && predictFallback.get() && enablePrediction.get())
                placed = placeBest(target, target.getBlockPos());

            if (placed > 0) placeTimer = placeDelay.get();
        } else {
            placeTimer--;
        }
    }

    // ─── EXPLOTAR MEJOR CRISTAL ───────────────────────────────────────────────
    /**
     * Selecciona el cristal con mayor score (daño objetivo - daño propio × 0.5),
     * respetando los filtros de anti-suicide y daño mínimo.
     */
    private boolean explodeBestCrystal(PlayerEntity target) {
        EndCrystalEntity best = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                mc.player.getBoundingBox().expand(explodeRange.get()),
                e -> true)) {

            if (mc.player.distanceTo(crystal) > explodeRange.get()) continue;
            if (onlyExplodeNear.get() && target != null
                && crystal.distanceTo(target) > placeRange.get() + 2) continue;

            Vec3d cPos = crystal.getPos();

            float tDmg = (target != null)
                ? DamageUtils.crystalDamage(target,    cPos, null, mc.world) : 0f;
            float sDmg = DamageUtils.crystalDamage(mc.player, cPos, null, mc.world);

            if (tDmg < minTargetDamage.get()) continue;
            if (sDmg > maxSelfDamage.get())   continue;
            if (antiSuicide.get()) {
                float hpAfter = mc.player.getHealth() + mc.player.getAbsorptionAmount() - sDmg;
                if (hpAfter < minSelfHealth.get()) continue;
            }

            float score = tDmg - sDmg * 0.5f;
            if (score > bestScore) { bestScore = score; best = crystal; }
        }

        if (best == null) return false;
        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    // ─── COLOCAR (multi-place) ────────────────────────────────────────────────
    private int placeBest(PlayerEntity target, BlockPos origin) {
        List<PlaceData> candidates = findCandidates(target, origin);
        if (candidates.isEmpty()) { renderPos = null; return 0; }

        if (smartPosition.get())
            candidates.sort(Comparator.comparingDouble(PlaceData::score).reversed());

        int placed = 0;
        for (PlaceData d : candidates) {
            if (placed >= multiPlace.get()) break;

            var block = mc.world.getBlockState(d.pos).getBlock();
            boolean needsObs = block != Blocks.OBSIDIAN && block != Blocks.BEDROCK;

            if (needsObs && placeObsidian.get()) {
                renderPos = d.pos;
                if (placeObsidianAt(d.pos)) placed++;
            } else if (!needsObs) {
                renderPos = null;
                if (placeCrystal(d.pos)) placed++;
            }
        }
        return placed;
    }

    // ─── CANDIDATOS DE COLOCACIÓN ─────────────────────────────────────────────
    private record PlaceData(BlockPos pos, float targetDmg, float selfDmg) {
        double score() { return targetDmg - selfDmg * 0.5; }
    }

    private List<PlaceData> findCandidates(PlayerEntity target, BlockPos origin) {
        List<PlaceData> list = new ArrayList<>();

        for (BlockPos pos : BlockPos.iterate(origin.add(-2, -1, -2), origin.add(2, 1, 2))) {
            if (PlayerUtils.distanceTo(pos) > placeRange.get()) continue;

            var block = mc.world.getBlockState(pos).getBlock();
            boolean isBase = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
            boolean isAir  = block == Blocks.AIR;

            // Posición para cristal
            if (isBase && mc.world.isAir(pos.up())) {
                if (!mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos.up()), e -> true).isEmpty()) continue;

                Vec3d cp = Vec3d.ofCenter(pos.up());
                float tDmg = DamageUtils.crystalDamage(target,    cp, null, mc.world);
                float sDmg = DamageUtils.crystalDamage(mc.player, cp, null, mc.world);

                if (tDmg < minTargetDamage.get()) continue;
                if (sDmg > maxSelfDamage.get())   continue;
                if (antiSuicide.get()) {
                    float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount() - sDmg;
                    if (hp < minSelfHealth.get()) continue;
                }
                list.add(new PlaceData(pos.toImmutable(), tDmg, sDmg));
            }

            // Posición para obsidiana
            if (placeObsidian.get() && isAir) {
                BlockPos below = pos.down();
                if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below)) continue;
                if (!mc.world.isAir(pos.up())) continue;

                Vec3d cp = Vec3d.ofCenter(pos.up());
                float tDmg = DamageUtils.crystalDamage(target,    cp, null, mc.world);
                float sDmg = DamageUtils.crystalDamage(mc.player, cp, null, mc.world);

                if (tDmg >= minTargetDamage.get() && sDmg <= maxSelfDamage.get())
                    list.add(new PlaceData(pos.toImmutable(), tDmg, sDmg));
            }
        }
        return list;
    }

    // ─── HOTBAR HUB ───────────────────────────────────────────────────────────
    /**
     * Mantiene End Crystals en crystalSlot y Obsidiana en obsidianSlot.
     * Solo mueve si el slot destino no tiene ya el ítem correcto.
     */
    private void manageHotbar() {
        if (mc.player == null) return;
        int cSlot = crystalSlot.get() - 1;
        int oSlot = obsidianSlot.get() - 1;

        if (mc.player.getInventory().getStack(cSlot).getItem() != Items.END_CRYSTAL) {
            var c = InvUtils.find(Items.END_CRYSTAL);
            if (c.found() && c.slot() != cSlot) InvUtils.move().from(c.slot()).toHotbar(cSlot);
        }
        if (mc.player.getInventory().getStack(oSlot).getItem() != Items.OBSIDIAN) {
            var o = InvUtils.find(Items.OBSIDIAN);
            if (o.found() && o.slot() != oSlot) InvUtils.move().from(o.slot()).toHotbar(oSlot);
        }
    }

    // ─── RENDER 3D ────────────────────────────────────────────────────────────
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderPlace.get() || renderPos == null) return;
        event.renderer.box(renderPos, SIDE_COLOR, LINE_COLOR, ShapeMode.Both, 0);
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

    // ─── COLOCAR OBSIDIANA ────────────────────────────────────────────────────
    private boolean placeObsidianAt(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return false;
        int oSlot = obsidianSlot.get() - 1;

        if (enableHub.get() && mc.player.getInventory().getStack(oSlot).getItem() == Items.OBSIDIAN) {
            if (autoSwitch.get()) { prevSlot = mc.player.getInventory().getSelectedSlot(); InvUtils.swap(oSlot, false); }
        } else {
            var obs = InvUtils.findInHotbar(Items.OBSIDIAN);
            if (!obs.found()) {
                var inv = InvUtils.find(Items.OBSIDIAN);
                if (!inv.found()) return false;
                int empty = getEmptyHotbarSlot();
                if (empty == -1) empty = mc.player.getInventory().getSelectedSlot();
                InvUtils.move().from(inv.slot()).toHotbar(empty);
                obs = InvUtils.findInHotbar(Items.OBSIDIAN);
                if (!obs.found()) return false;
            }
            if (autoSwitch.get()) { prevSlot = mc.player.getInventory().getSelectedSlot(); InvUtils.swap(obs.slot(), false); }
        }

        Vec3d hit = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, pos.down(), false));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (autoSwitch.get() && prevSlot != -1) { InvUtils.swap(prevSlot, false); prevSlot = -1; }
        return true;
    }

    // ─── COLOCAR CRISTAL ──────────────────────────────────────────────────────
    private boolean placeCrystal(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return false;
        int cSlot = crystalSlot.get() - 1;
        boolean inOffhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        Hand hand;

        if (inOffhand) {
            hand = Hand.OFF_HAND;
        } else if (enableHub.get() && mc.player.getInventory().getStack(cSlot).getItem() == Items.END_CRYSTAL) {
            if (autoSwitch.get()) { prevSlot = mc.player.getInventory().getSelectedSlot(); InvUtils.swap(cSlot, false); }
            hand = Hand.MAIN_HAND;
        } else {
            var c = InvUtils.findInHotbar(Items.END_CRYSTAL);
            if (!c.found()) return false;
            if (autoSwitch.get() && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
                prevSlot = mc.player.getInventory().getSelectedSlot();
                InvUtils.swap(c.slot(), false);
            }
            hand = Hand.MAIN_HAND;
        }

        Vec3d hit = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(hit, Direction.UP, pos, false));
        mc.player.swingHand(hand);
        if (autoSwitch.get() && prevSlot != -1) { InvUtils.swap(prevSlot, false); prevSlot = -1; }
        return true;
    }

    // ─── UTILIDAD ─────────────────────────────────────────────────────────────
    private int getEmptyHotbarSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        return -1;
    }
}
