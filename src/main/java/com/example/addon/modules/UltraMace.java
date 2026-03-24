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

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("Mace Power")
        .defaultValue(23)
        .min(1)
        .sliderRange(1, 255)
        .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Switch")
        .defaultValue(true)
        .build());

    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "xMace", "Máximo poder del mazo con Infinite Reach.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.getNetworkHandler() == null || isWorking) return;

        if (event.packet instanceof IPlayerMoveC2SPacket move && move.meteor$getTag() == 1337) return;

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
                } catch (Exception ex) {
                    oldSlot = 0;
                }
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

            if (maceSlot != -1) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));

            SuperAura aura = Modules.get().get(SuperAura.class);
            if (aura != null && aura.isActive()) {
                Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
                aura.teleportToAndBack(target, () -> executeHits(target, targetPos), (pos, onGround) -> sendPos(pos.x, pos.y, pos.z, onGround));
            } else {
                Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                executeHits(target, playerPos);
            }

            if (maceSlot != -1) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            
            isWorking = false;
        }
    }

    private void executeHits(Entity target, Vec3d origin) {
        lerpUpDown(origin, fallHeight.get());
        sendAttack(target);
    }

    private void lerpUpDown(Vec3d origin, int height) {
        Vec3d top = origin.add(0, height, 0);
        sendPos(top.x, top.y, top.z, false);
        sendPos(origin.x, origin.y, origin.z, true);
    }

    private void sendAttack(Entity target) {
        try {
            SuperAura.isSendingAttack = true;
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        } finally {
            SuperAura.isSendingAttack = false;
        }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
