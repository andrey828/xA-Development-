package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.starscript.Script;

import java.util.ArrayList;
import java.util.List;

public class RPC extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> l1 = sgGeneral.add(new StringListSetting.Builder()
        .name("Line 1")
        .defaultValue(List.of("Playing on {server}", "{player}"))
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );
    private final Setting<List<String>> l2 = sgGeneral.add(new StringListSetting.Builder()
        .name("Line 2")
        .defaultValue(List.of("{server.player_count} Players online", "{round(player.health, 1)}hp"))
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );
    private final Setting<Integer> refreshDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Refresh Delay")
        .defaultValue(100)
        .range(0, 1000)
        .sliderRange(0, 1000)
        .build()
    );

    private int ticks = 0;
    private int index1 = 0;
    private int index2 = 0;
    private static final RichPresence presence = new RichPresence();

    public RPC() {
        super(AddonTemplate.CATEGORY, "xRPC", "Discord Rich Presence for xA.");
        
        // Esto hace que se active automáticamente al cargar el addon
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
        List<String> messages1 = getMessages(l1.get());
        List<String> messages2 = getMessages(l2.get());
        
        if (messages1.isEmpty() || messages2.isEmpty()) return;

        index1 = (index1 + 1) % messages1.size();
        index2 = (index2 + 1) % messages2.size();

        presence.setDetails(mc.player == null ? "In Main Menu" : messages1.get(index1));
        presence.setState(mc.player == null ? "In Main Menu" : messages2.get(index2));
        presence.setLargeImage("25565", "xA Addon");
        
        DiscordIPC.setActivity(presence);
    }

    private List<String> getMessages(List<String> stateList) {
        List<String> messages = new ArrayList<>();
        for (String msg : stateList) {
            Script script = MeteorStarscript.compile(msg);
            if (script != null) {
                messages.add(MeteorStarscript.run(script).toString());
            }
        }
        return messages;
    }
}

