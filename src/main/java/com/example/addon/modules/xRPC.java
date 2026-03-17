package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import java.util.List;

public class xRPC extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> l1 = sgGeneral.add(new StringListSetting.Builder()
        .name("line-1")
        .defaultValue(List.of("xA Addon on top", "xA Development"))
        .build()
    );
    private final Setting<List<String>> l2 = sgGeneral.add(new StringListSetting.Builder()
        .name("line-2")
        .defaultValue(List.of("Using xA modules", "Best Addon"))
        .build()
    );
    private final Setting<Integer> refreshDelay = sgGeneral.add(new IntSetting.Builder()
        .name("refresh-delay")
        .defaultValue(100)
        .min(0)
        .build()
    );

    private int ticks = 0;
    private int index1 = 0;
    private int index2 = 0;
    private static final RichPresence presence = new RichPresence();

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence for xA.");
        if (!isActive()) toggle();
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(1483491540784644377L, null);
        presence.setStart(System.currentTimeMillis() / 1000L);
        updatePresence();
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent.Pre event) {
        if (ticks > 0) {
            ticks--;
        } else {
            updatePresence();
        }
    }

    public void updatePresence() {
        ticks = refreshDelay.get();
        List<String> messages1 = l1.get();
        List<String> messages2 = l2.get();
        
        if (messages1.isEmpty() || messages2.isEmpty()) return;

        index1 = (index1 + 1) % messages1.size();
        index2 = (index2 + 1) % messages2.size();

        String line1 = mc.player == null ? "In Main Menu" : messages1.get(index1);
        String line2 = mc.player == null ? "In Main Menu" : messages2.get(index2);

        presence.setDetails(line1);
        presence.setState(line2);
        presence.setLargeImage("25565", "xA Addon");
        
        DiscordIPC.setActivity(presence);
    }
}
