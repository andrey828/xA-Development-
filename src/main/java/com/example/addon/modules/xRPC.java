package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private RichPresence presence;
    private int ticks;
    private int index1;
    private int index2;
    private long startTime;
    private final AtomicBoolean ipcStarted = new AtomicBoolean(false);

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence for xA.");
    }

    @Override
    public void onActivate() {
        presence  = new RichPresence();
        ticks     = 0;
        index1    = 0;
        index2    = 0;
        startTime = System.currentTimeMillis() / 1000L;
        startIPC();
    }

    @Override
    public void onDeactivate() {
        if (ipcStarted.get()) {
            try { DiscordIPC.stop(); } catch (Exception ignored) {}
            ipcStarted.set(false);
        }
    }

    private void startIPC() {
        try {
            DiscordIPC.start(1483491540784644377L, null);
            ipcStarted.set(true);
            presence.setStart(startTime);
            updateRPC();
        } catch (Exception e) {
            ipcStarted.set(false);
        }
    }

    private String getServerName() {
        // En un servidor online devuelve la IP, en singleplayer devuelve null
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().address;
        }
        // Singleplayer / Realm
        if (mc.isIntegratedServerRunning()) {
            return "Singleplayer";
        }
        return null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        try { DiscordIPC.stop(); } catch (Exception ignored) {}
        startIPC();

        if (!ipcStarted.get()) return;

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

        String details = l1.get(index1 % l1.size());
        String state   = l2.get(index2 % l2.size());
        index1 = (index1 + 1) % l1.size();
        index2 = (index2 + 1) % l2.size();

        presence.setDetails(details);
        presence.setState(state);

        // Muestra el servidor debajo del estado
        String server = getServerName();
        if (server != null) {
            presence.setSmallImage("25565", "Jugando en: " + server);
        } else {
            presence.setSmallImage(null, null); // Menú principal, sin servidor
        }

        presence.setLargeImage("25565", "xA Addon");
        DiscordIPC.setActivity(presence);
    }
}
