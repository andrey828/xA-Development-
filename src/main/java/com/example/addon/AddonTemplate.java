package com.example.addon;

import com.example.addon.modules.LanzaDMG;
import com.example.addon.modules.SuperAura;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(AddonTemplate.class);
    public static final Category CATEGORY = new Category("xAddon");

    @Override
    public void onInitialize() {
        LOG.info("Inicializando xAddon - Modo Anarquía");

        // Agregamos los módulos a la lista de Meteor
        Modules.get().add(new LanzaDMG());
        Modules.get().add(new SuperAura());
    }

    @Override
    public void onRegisterCategories() {
        // Registramos la columna en la interfaz
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}

