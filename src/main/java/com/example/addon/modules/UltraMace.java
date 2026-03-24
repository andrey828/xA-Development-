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

public class UltraMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder().name("Mace Power").defaultValue(23).min(1).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("Auto Switch").defaultValue(true).build());

    private boolean isWorking = false;

    public UltraMace() {
        super(AddonTemplate.CATEGORY, "xMace", "Maximum Mace Power.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || isWorking) return;
        if (event.packet instanceof IPlayerMoveC2SPacket move && move.meteor$getTag() == 1337) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            if (!String.valueOf(accessor.meteor$getType()).contains("ATTACK")) return;

            Entity entity = accessor.meteor$getEntity();
            if (!(entity instanceof LivingEntity target) || (target instanceof PlayerEntity p && Friends.get().isFriend(p))) return;

            event.cancel();
            isWorking = true;

            int maceSlot = -1;
            if (autoSwitch.get()) {
                for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) { maceSlot = i; break; }
            }

            if (maceSlot != -1) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));

            SuperAura aura = Modules.get().get(SuperAura.class);
            if (aura != null && aura.isActive()) {
                aura.teleportToAndBack(target, () -> executeHits(target, target.getPos()), (pos, onGround) -> sendPos(pos.x, pos.y, pos.z, onGround));
            } else {
                executeHits(target, mc.player.getPos());
            }

            if (maceSlot != -1) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
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
        } finally { SuperAura.isSendingAttack = false; }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }
}
