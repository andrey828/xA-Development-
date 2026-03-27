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

    private static long startTimestamp = -1;

    private boolean ipcConnected = false;
    private int reconnectTicks = 0;

    private static final int RECONNECT_INTERVAL = 600; // ~30s (suficiente para ganar prioridad)

    private boolean initialized = false;

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Clean Discord RPC.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        if (startTimestamp == -1) {
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

            DiscordIPC.start(1483491540784644377L, null);

            rpc.setStart(startTimestamp);
            rpc.setLargeImage("25565", "xA Addon");

            // 🔥 SOLO UNA VEZ
            setRPCContent();

            DiscordIPC.setActivity(rpc);

            ipcConnected = true;

        } catch (Exception e) {
            ipcConnected = false;
        }
    }

    private void reconnectIPC() {
        try {
            DiscordIPC.stop();
            Thread.sleep(100);
        } catch (Exception ignored) {}

        ipcConnected = false;
        connectIPC();
    }

    private void setRPCContent() {
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

        // auto start
        if (!initialized) {
            initialized = true;
            if (!this.isActive()) this.toggle();
            return;
        }

        if (!ipcConnected) {
            connectIPC();
            return;
        }

        // 🔥 SOLO reconectar cada X tiempo (sin actualizar contenido)
        reconnectTicks++;
        if (reconnectTicks >= RECONNECT_INTERVAL) {
            reconnectTicks = 0;
            reconnectIPC();
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
