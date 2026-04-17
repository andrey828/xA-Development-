package com.example.addon.modules;

//papoi q lees esto, tanto te interesa? es un modulo basico :v
import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;

public class TeleportToPlayer extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> stepSize = sgGeneral.add(new DoubleSetting.Builder()
        .name("step-size")
        .description("Bloques por paquete.")
        .defaultValue(10).min(1.0).sliderMax(30.0)
        .build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("No teleportar a amigos.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> disableAfterTp = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-after-tp")
        .description("Se desactiva solo después de teleportar.")
        .defaultValue(false)
        .build());

    public TeleportToPlayer() {
        super(AddonTemplate.CATEGORY, "xPlayerTP", "Te teleporta al jugador más cercano instantáneamente via paquetes (Inspirado en Flytp De Sunny");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Buscar al objetivo más cercano
        PlayerEntity target = mc.world.getPlayers().stream()
            .filter(p -> p != mc.player)
            .filter(p -> !p.isDead())
            .filter(p -> !(ignoreFriends.get() && Friends.get().isFriend(p)))
            .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
            .orElse(null);

        if (target == null) return;

        Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d destination = new Vec3d(target.getX(), target.getY(), target.getZ());

        double distance = origin.distanceTo(destination);

        double maxStep = stepSize.get();
        int steps = (int) Math.ceil(distance / maxStep);

        for (int i = 1; i <= steps; i++) {
            double ratio = (double) i / steps;
            double x = origin.x + (destination.x - origin.x) * ratio;
            double y = origin.y + (destination.y - origin.y) * ratio;
            double z = origin.z + (destination.z - origin.z) * ratio;

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, false));
        }

        mc.player.setPosition(destination.x, destination.y, destination.z);

        if (disableAfterTp.get()) toggle();
    }
}
