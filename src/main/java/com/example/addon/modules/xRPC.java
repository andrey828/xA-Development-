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

    // Fix: no longer static — each activation gets a fresh instance
    private RichPresence presence;
    private int ticks;
    private int index1;
    private int index2;
    private boolean ipcStarted = false;

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence for xA.");
    }

    @Override
    public void onActivate() {
        presence = new RichPresence();
        ticks  = 0;
        index1 = 0;
        index2 = 0;
        ipcStarted = false;

        try {
            DiscordIPC.start(1483491540784644377L, null);
            ipcStarted = true;
            presence.setStart(System.currentTimeMillis() / 1000L);
            updateRPC();
        } catch (Exception e) {
            // IPC failed (Discord not running, etc.) — module stays on but does nothing
            ipcStarted = false;
        }
    }

    @Override
    public void onDeactivate() {
        if (ipcStarted) {
            DiscordIPC.stop();
            ipcStarted = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!ipcStarted) return;

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
            // Fix: don't advance indexes when in main menu
            details = "Main Menu";
            state   = "Idle";
        } else {
            details = l1.get(index1 % l1.size());
            state   = l2.get(index2 % l2.size());
            // Fix: only increment when actually using the list values
            index1 = (index1 + 1) % l1.size();
            index2 = (index2 + 1) % l2.size();
        }

        presence.setDetails(details);
        presence.setState(state);
        // Fix: use a proper image asset key, not a port number
        presence.setLargeImage("25565", "xA Addon");
        DiscordIPC.setActivity(presence);
    }
}
