package com.example.addon;

import com.example.addon.modules.LanzaDMG;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(AddonTemplate.class);
    public static final Category CATEGORY = new Category("UltraAddon");

    @Override
    public void onInitialize() {
        LOG.info("Inicializando UltraAddon...");

        // Registro de módulos
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

