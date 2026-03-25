package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra Heights (Max Power)");

    private final Setting<Integer> blocksPerPacket = sgGeneral.add(new IntSetting.Builder()
        .name("Blocks Per Packet").description("A mayor número, menos paquetes usa. (20-30 es muy seguro)")
        .defaultValue(20).min(5).sliderRange(10, 100).build());

    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("Mace Power").description("Altura del golpe principal.")
        .defaultValue(23).min(1).sliderRange(1, 1000).build());

    private final Setting<Integer> attack1 = sgGeneral.add(new IntSetting.Builder()
        .name("Hit 1").description("Altura del primer golpe.")
        .defaultValue(23).min(1).sliderRange(1, 1000).build());

    private final Setting<Integer> attack2 = sgGeneral.add(new IntSetting.Builder()
        .name("Hit 2").description("Altura del segundo golpe.")
        .defaultValue(40).min(1).sliderRange(1, 1000).build());

    private final Setting<Integer> extraHitsAmount = sgGeneral.add(new IntSetting.Builder()
        .name("Extra Hits Amount").description("Cuántos extra hits adicionales.")
        .defaultValue(0).min(0).sliderRange(0, 30).build());

    private final Setting<Integer> sendPacketsAmount = sgGeneral.add(new IntSetting.Builder()
        .name("No Ground Packets").description("Paquetes sin suelo para TotemFail.")
        .defaultValue(4).min(1).sliderRange(1, 20).build());

    private final Setting<Boolean> alwaysTF = sgGeneral.add(new BoolSetting.Builder()
        .name("Do TotemFail").description("Siempre hace TotemFail.")
        .defaultValue(false).build());

    private final Setting<Integer> hitAmount = sgGeneral.add(new IntSetting.Builder()
        .name("Hit Amount").description("Cuántos hits por ataque.")
        .defaultValue(1).min(1).sliderRange(1, 10).build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Switch").description("Cambia automáticamente al Mace.")
        .defaultValue(true).build());

    private final List<Setting<Integer>> extraHeights = new ArrayList<>();
    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "xMace", "Maximum Mace Power - Low Packets.");

        for (int i = 1; i <= 30; i++) {
            int finalI = i;
            extraHeights.add(sgExtra.add(new IntSetting.Builder()
                .name("Extra Hit " + i)
                .description("Altura del extra hit " + i)
                .defaultValue(50 + (i * 10))
                .min(1)
                .sliderRange(1, 1000)
                .visible(() -> extraHitsAmount.get() >= finalI)
                .build()
            ));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.getNetworkHandler() == null || isWorking) return;
        if (event.packet instanceof IPlayerMoveC2SPacket move && move.meteor$getTag() == 1337) return;
        if (SuperAura.isSendingAttack) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            if (!String.valueOf(accessor.meteor$getType()).contains("ATTACK")) return;

            Entity entity = accessor.meteor$getEntity();
            if (!(entity instanceof LivingEntity target)) return;
            if (target instanceof PlayerEntity player && Friends.get().isFriend(player)) return;

            event.cancel();
            isWorking = true;

            int oldSlot = 0;
            try {
                Field field = mc.player.getInventory().getClass().getDeclaredField("selectedSlot");
                field.setAccessible(true);
                oldSlot = field.getInt(mc.player.getInventory());
            } catch (Exception e) {
                try {
                    Field field = mc.player.getInventory().getClass().getDeclaredField("field_7545");
                    field.setAccessible(true);
                    oldSlot = field.getInt(mc.player.getInventory());
                } catch (Exception ignored) {}
            }

            int maceSlot = -1;
            if (autoSwitch.get()) {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }
            }

            boolean hasMace = maceSlot != -1 || mc.player.getMainHandStack().isOf(Items.MACE);
            if (!hasMace) { isWorking = false; return; }

            if (autoSwitch.get() && maceSlot != -1) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
            }

            Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

            SuperAura aura = Modules.get().get(SuperAura.class);
            boolean auraActive = aura != null && aura.isActive();

            if (auraActive) {
                aura.teleportToAndBack(target, () -> executeHits(target, origin), (pos, onGround) -> sendPos(pos.x, pos.y, pos.z, onGround));
            } else {
                executeHits(target, origin);
            }

            if (autoSwitch.get() && maceSlot != -1) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            }

            isWorking = false;
        }
    }

    private void executeHits(Entity target, Vec3d origin) {
        double px = origin.x;
        double py = origin.y;
        double pz = origin.z;

        if (alwaysTF.get()) {
            for (int i = 0; i < hitAmount.get(); i++) {
                performOptimizedHit(target, px, py, pz, attack1.get());
                performOptimizedHit(target, px, py, pz, attack2.get());
                performOptimizedHit(target, px, py, pz, attack2.get());

                for (int j = 0; j < extraHitsAmount.get(); j++) {
                    performOptimizedHit(target, px, py, pz, extraHeights.get(j).get());
                }
            }

            for (int i = 0; i < sendPacketsAmount.get(); i++) {
                sendPos(px, py + (i * 0.001), pz, false);
            }
            sendAttack(target);
            sendPos(px, py, pz, true);

        } else {
            for (int i = 0; i < hitAmount.get(); i++) {
                performOptimizedHit(target, px, py, pz, fallHeight.get());

                for (int j = 0; j < extraHitsAmount.get(); j++) {
                    performOptimizedHit(target, px, py, pz, extraHeights.get(j).get());
                }
            }

            sendPos(px, py, pz, true);
        }
    }

    private void performOptimizedHit(Entity target, double x, double y, double z, int height) {
        int steps = Math.max(1, height / blocksPerPacket.get());

        for (int i = 1; i <= steps; i++) {
            sendPos(x, y + ((double) height * i / steps), z, false);
        }

        sendPos(x, y, z, false);

        sendAttack(target);
    }

    private void sendAttack(Entity target) {
        SuperAura.isSendingAttack = true;
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        SuperAura.isSendingAttack = false;
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
