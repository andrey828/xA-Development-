package com.example.addon;

import com.example.addon.modules.LanzaDMG;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(AddonTemplate.class);
    public static final Category CATEGORY = new Category("UltraAddon");
    
    // Esto es lo que te pide el error del HUD
    public static final HudGroup HUD_GROUP = new HudGroup("UltraHUD");

    @Override
    public void onInitialize() {
        LOG.info("Inicializando UltraAddon...");

        // Registro de tu módulo
        Modules.get().add(new LanzaDMG());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}

