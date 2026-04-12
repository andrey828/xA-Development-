package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ClipEscape extends Module {

    public enum ClipMode {
        VClip,
        HClip
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ClipMode> clipMode = sgGeneral.add(new EnumSetting.Builder<ClipMode>()
        .name("clip-mode")
        .description("Tipo de clip (.vclip o .hclip)")
        .defaultValue(ClipMode.VClip)
        .build()
    );

    private final Setting<Integer> clipAmount = sgGeneral.add(new IntSetting.Builder()
        .name("clip-amount")
        .description("Número de bloques para clippear (puede ser negativo)")
        .defaultValue(120)
        .min(-5000)
        .max(5000)
        .sliderRange(-500, 500)
        .build()
    );

    public ClipEscape() {
        super(AddonTemplate.CATEGORY, "xClipEscape", "Manda .vclip o .hclip ");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.player.networkHandler == null) {
            toggle();
            return;
        }

        String mode = clipMode.get() == ClipMode.VClip ? "vclip" : "hclip";
        
        String comando = "." + mode + " (" + clipAmount.get() + ")";

        mc.player.networkHandler.sendChatMessage(comando);

        mc.player.networkHandler.sendChatMessage("Escapando como siempre, q runer q eres :v");
    }
}
