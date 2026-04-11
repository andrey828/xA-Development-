package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Comparator;

public class Aimbot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Rango máximo para aimear.")
        .defaultValue(6.0).min(1.0).sliderMax(50.0)
        .build());

    private final Setting<Double> smoothing = sgGeneral.add(new DoubleSetting.Builder()
        .name("smoothing")
        .description("Suavidad del aim. Menor = más rápido.")
        .defaultValue(4.0).min(1.0).sliderMax(20.0)
        .build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Solo aimea mientras haces click.")
        .defaultValue(false)
        .build());

    public Aimbot() {
        super(AddonTemplate.CATEGORY, "xAimbot", "Apunta automáticamente al jugador más cercano.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (onlyOnClick.get() && !mc.options.attackKey.isPressed()) return;

        PlayerEntity target = mc.world.getPlayers().stream()
            .filter(p -> p != mc.player)
            .filter(p -> !p.isDead())
            .filter(p -> mc.player.distanceTo(p) <= range.get())
            .filter(p -> !(ignoreFriends.get() && Friends.get().isFriend(p)))
            .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
            .orElse(null);

        if (target == null) return;

        double diffX = target.getX() - mc.player.getX();
        double diffY = (target.getY() + target.getEyeHeight(target.getPose()) / 2)
                     - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = target.getZ() - mc.player.getZ();
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw   = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        float smooth = smoothing.get().floatValue();
        float currentYaw   = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180)  yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;

        mc.player.setYaw(currentYaw   + yawDiff   / smooth);
        mc.player.setPitch(currentPitch + (targetPitch - currentPitch) / smooth);
    }
}
