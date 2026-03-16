package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FareWell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiSpam = settings.createGroup("Anti-Spam");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .defaultValue(15.0)
            .min(1.0)
            .sliderMax(50.0)
            .build()
    );

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("messages")
            .defaultValue(new ArrayList<>(List.of(
                    "xA Addon on top {player}",
                    "A dormir xA on top {player}!",
                    "UltraMace manda.",
                    "Llora un poco mas {player}"
            )))
            .build()
    );

    // --- NUEVA OPCIÓN PARA TÓTEMS ---
    private final Setting<Boolean> onTotemPop = sgGeneral.add(new BoolSetting.Builder()
            .name("on-totem-pop")
            .description("Manda mensaje también cuando alguien rompa tótem.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiSpamBypass = sgAntiSpam.add(new BoolSetting.Builder()
            .name("anti-spam-bypass")
            .defaultValue(true)
            .build()
    );

    private final Random random = new Random();

    public FareWell() {
        // Nombre cambiado a xAutoez como pediste
        super(AddonTemplate.CATEGORY, "xAutoez", "Envia mensajes pesados cuando alguien muere o rompe tótem cerca.");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.world == null || mc.player == null) return;

        if (event.packet instanceof EntityStatusS2CPacket packet) {
            byte status = packet.getStatus();
            
            // 3 = Muerte, 35 = Romper Tótem
            if (status == 3 || (onTotemPop.get() && status == 35)) {
                Entity entity = packet.getEntity(mc.world);
                
                if (entity instanceof PlayerEntity victim) {
                    if (victim == mc.player) return;
                    if (Friends.get().isFriend(victim)) return;
                    
                    if (mc.player.distanceTo(victim) <= range.get()) {
                        sendJijijaMessage(victim.getName().getString(), status == 35);
                    }
                }
            }
        }
    }

    private void sendJijijaMessage(String victimName, boolean isTotemPop) {
        if (messages.get().isEmpty()) return;

        String template = messages.get().get(random.nextInt(messages.get().size()));
        
        // Si quieres diferenciar el mensaje de tótem podrías, pero aquí uso el mismo template
        String finalMsg = template.replace("{player}", victimName);

        if (antiSpamBypass.get()) {
            finalMsg += " [" + getRandomString(3) + "]";
        }

        if (mc.getNetworkHandler() != null) {
            mc.player.networkHandler.sendChatMessage(finalMsg);
        }
    }

    private String getRandomString(int length) {
        String chars = "ABC123XYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
