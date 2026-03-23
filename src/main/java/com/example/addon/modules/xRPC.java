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
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class xRPC extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> line1Strings = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-1-messages")
            .description("Messages used for the first line.")
            .defaultValue(List.of("xA Addon on top", "xA Development"))
            .onChanged(s -> recompileLine1())
            .build()
    );

    private final Setting<Integer> line1UpdateDelay = sgGeneral.add(
        new IntSetting.Builder()
            .name("line-1-update-delay")
            .description("How fast to update the first line in ticks.")
            .defaultValue(60).min(10).sliderRange(10, 200)
            .build()
    );

    private final Setting<DiscordPresence.SelectMode> line1SelectMode = sgGeneral.add(
        new EnumSetting.Builder<DiscordPresence.SelectMode>()
            .name("line-1-select-mode")
            .description("How to select messages for the first line.")
            .defaultValue(DiscordPresence.SelectMode.Sequential)
            .build()
    );

    private final Setting<List<String>> line2Strings = sgGeneral.add(
        new StringListSetting.Builder()
            .name("line-2-messages")
            .description("Messages used for the second line.")
            .defaultValue(List.of("Using xA modules", "Best Addon"))
            .onChanged(s -> recompileLine2())
            .build()
    );

    private final Setting<Integer> line2UpdateDelay = sgGeneral.add(
        new IntSetting.Builder()
            .name("line-2-update-delay")
            .description("How fast to update the second line in ticks.")
            .defaultValue(60).min(10).sliderRange(10, 200)
            .build()
    );

    private final Setting<DiscordPresence.SelectMode> line2SelectMode = sgGeneral.add(
        new EnumSetting.Builder<DiscordPresence.SelectMode>()
            .name("line-2-select-mode")
            .description("How to select messages for the second line.")
            .defaultValue(DiscordPresence.SelectMode.Sequential)
            .build()
    );

    private static final RichPresence rpc = new RichPresence();

    private final List<Script> line1Scripts = new ArrayList<>();
    private final List<Script> line2Scripts = new ArrayList<>();

    private SmallImage currentSmallImage;
    private int ticks;
    private boolean forceUpdate;
    private boolean lastWasInMainMenu;
    private int line1Ticks, line1I;
    private int line2Ticks, line2I;
    public boolean update;

    private boolean ipcConnected = false;
    private int retryTicks = 0;
    private static final int RETRY_INTERVAL = 200;

    public xRPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence for xA.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        ipcConnected = false;
        retryTicks = RETRY_INTERVAL;
        recompileLine1();
        recompileLine2();
        ticks = 0;
        line1Ticks = 0;
        line2Ticks = 0;
        lastWasInMainMenu = false;
        line1I = 0;
        line2I = 0;
        forceUpdate = true;
    }

    @Override
    public void onDeactivate() {
        if (ipcConnected) {
            try { DiscordIPC.stop(); } catch (Exception ignored) {}
        }
        ipcConnected = false;
    }

    private void tryConnect() {
        patchTLauncherIPC();
        try {
            DiscordIPC.start(1483491540784644377L, null);
            rpc.setStart(System.currentTimeMillis() / 1000L);
            rpc.setLargeImage("25565", "xA Addon");
            currentSmallImage = SmallImage.Logo;
            currentSmallImage.apply();
            forceUpdate = true;
            ipcConnected = true;
        } catch (Exception ignored) {
            ipcConnected = false;
        }
    }

    private void patchTLauncherIPC() {
        String[] searchDirs = {
            System.getenv("XDG_RUNTIME_DIR"),
            "/tmp",
            "/run/user/1000",
            System.getenv("XDG_RUNTIME_DIR") != null
                ? System.getenv("XDG_RUNTIME_DIR") + "/app/com.discordapp.Discord"
                : null,
            "/tmp/snap.discord",
            System.getProperty("java.io.tmpdir")
        };

        for (String dir : searchDirs) {
            if (dir == null) continue;
            for (int i = 0; i <= 9; i++) {
                File pipe = new File(dir, "discord-ipc-" + i);
                if (pipe.exists()) {
                    System.setProperty("java.io.tmpdir", dir);
                    return;
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!ipcConnected) {
            if (retryTicks < RETRY_INTERVAL) {
                retryTicks++;
                return;
            }
            retryTicks = 0;
            tryConnect();
            return;
        }

        update = false;

        if (ticks < 200 && !forceUpdate) {
            ticks++;
        } else {
            currentSmallImage = currentSmallImage.next();
            currentSmallImage.apply();
            update = true;
            ticks = 0;
        }

        if (Utils.canUpdate()) {
            handleLines();
        } else if (!lastWasInMainMenu) {
            handleScreens();
        }

        if (update) {
            try {
                DiscordIPC.setActivity(rpc);
            } catch (Exception e) {
                ipcConnected = false;
                retryTicks = 0;
                try { DiscordIPC.stop(); } catch (Exception ignored) {}
            }
        }

        forceUpdate = false;
        lastWasInMainMenu = !Utils
