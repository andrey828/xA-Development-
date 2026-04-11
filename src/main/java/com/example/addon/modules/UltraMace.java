package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class UltraMace extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHits = settings.createGroup("Hits Config");

    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
            .name("Mace Power (Fall height)")
            .description("Altura base del golpe principal")
            .defaultValue(23)
            .min(1)
            .sliderRange(1, 1000)
            .build());

    private final Setting<Integer> sendPacketsAmount = sgGeneral.add(new IntSetting.Builder()
            .name("No Ground Packets")
            .defaultValue(4)
            .min(1)
            .sliderRange(1, 30)
            .build());

    private final Setting<Boolean> packetDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("Disable When Blocked")
            .description("No envía paquetes si el enemigo bloquea")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> alwaysTF = sgGeneral.add(new BoolSetting.Builder()
            .name("Do TotemFail")
            .defaultValue(false)
            .build());

    private final Setting<Integer> hitAmount = sgGeneral.add(new IntSetting.Builder()
            .name("Hit Amount")
            .description("Cuántos Hits Extras")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 50)
            .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("Auto Switch (Mace)")
            .description("Cambia automáticamente al Mace")
            .defaultValue(true)
            .build());

    private final Setting<Server> serverType = sgGeneral.add(new EnumSetting.Builder<Server>()
            .name("ServerType")
            .description("Tipo de servidor")
            .defaultValue(Server.Paper)
            .build());

    // --- Hits Config Group ---
    private final Setting<Integer> activeHits = sgHits.add(new IntSetting.Builder()
            .name("Active Hits")
            .description("Agg hits(1-50)")
            .defaultValue(4)
            .min(1)
            .sliderRange(1, 50)
            .build());

    private final List<Setting<Integer>> hitHeights = new ArrayList<>();
    private LivingEntity targetPlayer;
    private boolean attackBool;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "xMace", "Hace Tu Mazo Mas Fuerte Para Que Rompas Papois");
        int[] defaults = {23, 40, 40, 50, 60, 70, 80, 100};
        for (int i = 1; i <= 50; i++) {
            int finalI = i;
            int def = (i <= defaults.length) ? defaults[i - 1] : 50 + (i * 10);
            hitHeights.add(sgHits.add(new IntSetting.Builder()
                    .name("Hit " + i)
                    .description("Altura del hit " + i)
                    .defaultValue(def)
                    .min(1)
                    .sliderRange(1, 1000)
                    .visible(() -> activeHits.get() >= finalI)
                    .build()
            ));
        }
    }

    private List<Integer> getActiveHitList() {
        int count = Math.min(activeHits.get(), hitHeights.size());
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(hitHeights.get(i).get());
        return result;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.getNetworkHandler() == null || attackBool) return;
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            if (!String.valueOf(accessor.meteor$getType()).contains("ATTACK")) return;
            Entity entity = accessor.meteor$getEntity();
            if (!(entity instanceof LivingEntity targetEntity)) return;
            if (targetEntity instanceof PlayerEntity p && Friends.get().isFriend(p)) return;
            if (targetEntity == mc.player) return;
            event.cancel();
            attackBool = true;
            targetPlayer = targetEntity;
            int maceSlot = -1;
            if (autoSwitch.get()) {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }
                if (maceSlot != -1) InvUtils.swap(maceSlot, false);
            }
            List<Integer> hits = getActiveHitList();
            if (alwaysTF.get()) {
                for (int i = 0; i < hitAmount.get(); i++) {
                    for (int height : hits) {
                        serverBasedTeleport(targetEntity, height);
                        sendAttack(targetEntity);
                    }
                }
            } else {
                for (int i = 0; i < hitAmount.get(); i++) {
                    serverBasedTeleport(targetEntity, fallHeight.get());
                    sendAttack(targetEntity);
                }
            }
            if (autoSwitch.get() && maceSlot != -1) InvUtils.swap(maceSlot, false);
            attackBool = false;
        }
    }

    private void serverBasedTeleport(LivingEntity enemyPlayer, int height) {
        if (packetDisable.get() && (enemyPlayer.isBlocking() || enemyPlayer.isUsingItem() || enemyPlayer.isDead())) return;
        switch (serverType.get()) {
            case Spigot -> LerpUpDown(enemyPlayer, height);
            case Paper -> performSilentTp(enemyPlayer, height);
        }
    }

    private void LerpUpDown(LivingEntity targetEntity, int altura) {
        if (mc.player == null || targetEntity == mc.player) return;
        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int steps = Math.max(3, altura / 10);
        for (int i = 1; i <= steps; i++) {
            double currentY = origin.y + (altura * (i / (double) steps));
            sendPos(origin.x, currentY, origin.z, false);
        }
        sendPos(origin.x, origin.y, origin.z, false);
    }

    private void performSilentTp(LivingEntity targetEntity, int altura) {
        if (mc.player == null || targetEntity == mc.player) return;
        Vec3d previouspos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int blocks = altura;
        boolean shortTp = blocks <= 22;
        if (mc.player.hasVehicle() && mc.player.getVehicle() != null) {
            var vehicle = mc.player.getVehicle();
            Vec3d vPos = new Vec3d(vehicle.getX(), vehicle.getY(), vehicle.getZ());
            for (int i = 0; i < sendPacketsAmount.get(); i++) {
                mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(vPos, vehicle.getYaw(), vehicle.getPitch(), false));
            }
            double maxHeight = shortTp ? Math.min(vPos.y + 22, vPos.y + blocks) : vPos.y + blocks;
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(vPos, vehicle.getYaw(), vehicle.getPitch(), false));
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(vPos, vehicle.getYaw(), vehicle.getPitch(), false));
        } else {
            for (int i = 0; i < sendPacketsAmount.get(); i++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
            }
            double maxHeight = shortTp ? Math.min(mc.player.getY() + 22, mc.player.getY() + blocks) : mc.player.getY() + blocks;
            PlayerMoveC2SPacket movepacket = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), maxHeight, mc.player.getZ(), false, false);
            ((IPlayerMoveC2SPacket) movepacket).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(movepacket);
            PlayerMoveC2SPacket homepacket = new PlayerMoveC2SPacket.PositionAndOnGround(previouspos.x, previouspos.y, previouspos.z, false, false);
            ((IPlayerMoveC2SPacket) homepacket).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(homepacket);
        }
    }

    private void sendAttack(LivingEntity target) {
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false);
        ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(packet);
    }

    public enum Server {
        Spigot, Paper
    }
}
