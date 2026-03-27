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
import meteordevelopment.meteorclient.utils.Utils;
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
    private boolean autoStarted = true;

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence .");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
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
            DiscordIPC.start(1483491540784644377L, null);

            rpc.setStart(System.currentTimeMillis() / 1000L);
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

        // Auto start module when Minecraft starts
        if (!autoStarted) {
            autoStarted = true;
            if (!this.isActive()) this.toggle();
        }

        // Retry connection
        if (!ipcConnected) {
            retryTicks++;
            if (retryTicks >= RETRY_INTERVAL) {
                retryTicks = 0;
                connectIPC();
            }
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
