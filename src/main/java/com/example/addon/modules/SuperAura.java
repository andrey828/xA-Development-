package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class SuperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .defaultValue(60.0)
        .range(1, 1000)
        .build()
    );

    private final Setting<Double> stepDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("Step Distance")
        .defaultValue(8.0)
        .range(1, 20)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Switch")
        .defaultValue(true)
        .build()
    );

    public SuperAura() {
        super(AddonTemplate.CATEGORY, "SuperAura", "Pega desde lejos optimizado");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        for (Entity target : mc.world.getEntities()) {
            if (!(target instanceof LivingEntity) || target == mc.player || !target.isAlive()) continue;
            if (mc.player.distanceTo(target) > range.get()) continue;

            attackEntity(target);
            break;
        }
    }

    private void attackEntity(Entity target) {
        // CORRECCIÓN: Usamos coordenadas directas para evitar error de getPos()
        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        double distance = origin.distanceTo(targetPos);

        if (autoSwitch.get()) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
                    // CORRECCIÓN: Usamos mc.player.getInventory().selectedSlot si no es privado, 
                    // o simplemente enviamos el paquete siempre para asegurar.
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
                    break;
                }
            }
        }

        // Ida
        for (double d = stepDistance.get(); d < distance; d += stepDistance.get()) {
            Vec3d path = origin.add(targetPos.subtract(origin).multiply(d / distance));
            sendPos(path.x, path.y, path.z);
        }

        // Ataque
        sendPos(targetPos.x, targetPos.y, targetPos.z);
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);

        // Regreso
        sendPos(origin.x, origin.y, origin.z);
    }

    private void sendPos(double x, double y, double z) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, mc.player.horizontalCollision));
        }
    }
}
