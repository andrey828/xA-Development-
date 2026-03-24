package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.misc.DiscordPresence;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.util.Util;

import java.io.File;
import java.util.List;

public class xRPC extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> line1Strings = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-1-messages")
            .defaultValue(List.of("xA Addon on top", "xA Development"))
            .build()
    );

    private final Setting<List<String>> line2Strings = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-2-messages")
            .defaultValue(List.of("Using xA modules", "Best Addon"))
            .build()
    );

    private static final RichPresence rpc = new RichPresence();

    private SmallImage currentSmallImage;
    private boolean ipcConnected = false;
    private int retryTicks = 0;
    private static final int RETRY_INTERVAL = 60;
    private boolean autoStarted = false;

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence for xA.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        tryConnect();
    }

    @Override
    public void onDeactivate() {
        if (ipcConnected) {
            try { DiscordIPC.stop(); } catch (Exception ignored) {}
        }
        ipcConnected = false;
    }

    private void tryConnect() {
        new Thread(() -> {
            patchDiscordIPC();
            try {
                DiscordIPC.start(1483491540784644377L, () -> {
                    rpc.setStart(System.currentTimeMillis() / 1000L);
                    rpc.setLargeImage("25565", "xA Addon");

                    currentSmallImage = SmallImage.Logo;
                    currentSmallImage.apply();

                    ipcConnected = true;
                    updateRPC();
                });

                DiscordIPC.onDisconnect(() -> {
                    ipcConnected = false;
                    retryTicks = 0;
                });

            } catch (Exception e) {
                ipcConnected = false;
            }
        }, "xRPC-IPC").start();
    }

    private void patchDiscordIPC() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String[] paths = {
                System.getenv("LOCALAPPDATA"),
                System.getenv("APPDATA"),
                System.getenv("USERPROFILE") + "\\AppData\\Roaming",
                System.getenv("USERPROFILE") + "\\AppData\\Local",
                "C:\\Windows\\Temp"
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
        } else {
            String[] paths = {
                System.getenv("XDG_RUNTIME_DIR"),
                "/run/user/1000",
                "/tmp",
                "/var/tmp",
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
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (!autoStarted) {
            autoStarted = true;
            if (!this.isActive()) this.toggle();
        }

        if (!ipcConnected) {
            retryTicks++;
            if (retryTicks >= RETRY_INTERVAL) {
                retryTicks = 0;
                tryConnect();
            }
            return;
        }

        DiscordIPC.runCallbacks();
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

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        updateRPC();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton btn = theme.button("Join xA Discord");
        btn.action = () -> Util.getOperatingSystem().open("https://discord.gg/8ezp4sthqG");
        return btn;
    }

    public enum SmallImage {
        Logo("25565", "xA Addon");

        private final String key;
        private final String text;

        SmallImage(String key, String text) {
            this.key = key;
            this.text = text;
        }

        void apply() {
            rpc.setSmallImage(key, text);
        }
    }
}
