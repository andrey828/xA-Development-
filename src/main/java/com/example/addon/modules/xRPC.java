package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

public class xRPC extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> line1 = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-1")
            .defaultValue(List.of("xA Addon on top", "xA Development"))
            .build()
    );

    private final Setting<List<String>> line2 = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-2")
            .defaultValue(List.of("Using xA modules", "Best Addon"))
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(
        new IntSetting.Builder()
            .name("refresh-delay")
            .defaultValue(100)
            .min(20)
            .sliderMax(400)
            .build()
    );

    private static final RichPresence presence = new RichPresence();

    private int ticks;
    private int index1;
    private int index2;

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence for xA.");
    }

    @Override
    public void onActivate() {

        try {
            DiscordIPC.start(1483491540784644377L, null);
        } catch (Exception ignored) {}

        presence.setStart(System.currentTimeMillis() / 1000L);

        ticks = 0;
        index1 = 0;
        index2 = 0;

        updateRPC();
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        ticks++;

        if (ticks >= delay.get()) {
            ticks = 0;
            updateRPC();
        }
    }

    private void updateRPC() {

        List<String> l1 = line1.get();
        List<String> l2 = line2.get();

        if (l1.isEmpty() || l2.isEmpty()) return;

        String details;
        String state;

        if (mc.player == null) {
            details = "Main Menu";
            state = "Idle";
        } else {
            details = l1.get(index1);
            state = l2.get(index2);
        }

        index1 = (index1 + 1) % l1.size();
        index2 = (index2 + 1) % l2.size();

        presence.setDetails(details);
        presence.setState(state);

        presence.setLargeImage("25565", "xA Addon");

        DiscordIPC.setActivity(presence);
    }
}
