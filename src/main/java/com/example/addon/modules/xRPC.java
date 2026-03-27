package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Util;

import java.io.File;
import java.util.List;

public class xRPC extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> line1Strings = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-1-messages")
            .defaultValue(List.of("xA Addon on top"))
            .build()
    );

    private final Setting<List<String>> line2Strings = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-2-messages")
            .defaultValue(List.of("Using xA Addon"))
            .build()
    );

    private static final RichPresence rpc = new RichPresence();

    private boolean ipcConnected = false;
    private int retryTicks = 0;
    private static final int RETRY_INTERVAL = 50;

    private int forceReconnectTicks = 0;
    private static final int FORCE_RECONNECT_INTERVAL = 200;

    private static final int START_DELAY = 100;
    private int startDelayTicks = 0;

    private boolean autoStarted = false;

    private long startTimestamp = 0; // CLAVE

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        if (startTimestamp == 0) {
            startTimestamp = System.currentTimeMillis() / 1000L;
        }
        connectIPC();
    }

    @Override
    public void onDeactivate() {
        try {
            DiscordIPC.stop();
        } catch (Exception ignored) {}
        ipcConnected = false;
    }

    private void connectIPC() {
        try {
            patchDiscordIPC();

            try {
                DiscordIPC.stop();
            } catch (Exception ignored) {}

            Thread.sleep(200);

            DiscordIPC.start(1483491540784644377L, null);

            // NO SE RESETEA
            rpc.setStart(startTimestamp);

            rpc.setLargeImage("25565", "xA Addon");

            ipcConnected = true;
            updateRPC();

        } catch (Exception e) {
            ipcConnected = false;
        }
    }

    private void patchDiscordIPC() {
        String[] paths = {
            System.getenv("LOCALAPPDATA"),
            System.getenv("APPDATA"),
            System.getenv("USERPROFILE"),
            "/run/user/1000",
            "/tmp",
            System.getProperty("java.io.tmpdir")
        };

        for (String path : paths) {
            if (path == null) continue;
            File dir = new File(path);
            if (!dir.exists()) continue;

            for (int i = 0; i <= 9; i++) {
                File pipe = new File(dir, "discord-ipc-" + i);
                if (pipe.exists()) {
                    System.setProperty("java.io.tmpdir", path);
                    return;
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (startDelayTicks < START_DELAY) {
            startDelayTicks++;
            return;
        }

        if (!autoStarted) {
            autoStarted = true;
            if (!this.isActive()) this.toggle();
        }

        if (!ipcConnected) {
            retryTicks++;
            if (retryTicks >= RETRY_INTERVAL) {
                retryTicks = 0;
                connectIPC();
            }
            return;
        }

        forceReconnectTicks++;
        if (forceReconnectTicks >= FORCE_RECONNECT_INTERVAL) {
            forceReconnectTicks = 0;

            try {
                DiscordIPC.stop();
            } catch (Exception ignored) {}

            ipcConnected = false;
            connectIPC();
            return;
        }

        updateRPC();
    }

    private void updateRPC() {
        if (mc.player == null) {
            rpc.setDetails("xA Addon");
            rpc.setState("En el menu");
        } else {
            rpc.setDetails(line1Strings.get().get(0));

            String server = getServerName();
            if (server != null) {
                rpc.setState(line2Strings.get().get(0) + " | " + server);
            } else {
                rpc.setState(line2Strings.get().get(0));
            }
        }

        try {
            DiscordIPC.setActivity(rpc);
        } catch (Exception e) {
            ipcConnected = false;
        }
    }

    private String getServerName() {
        if (mc.getCurrentServerEntry() != null) return mc.getCurrentServerEntry().address;
        if (mc.isInSingleplayer()) return "Singleplayer";
        return null;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton btn = theme.button("Join xA Discord");
        btn.action = () -> Util.getOperatingSystem().open("https://discord.gg/8ezp4sthqG");
        return btn;
    }
}
