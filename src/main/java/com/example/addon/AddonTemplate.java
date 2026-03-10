package com.example.addon;

import com.example.addon.modules.LanzaDMG;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.item.Items; // Import extra para seguridad

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(AddonTemplate.class);
    
    // Cambiamos el nombre de la categoría a algo simple para probar
    public static final Category CATEGORY = new Category("Ultra");

    @Override
    public void onInitialize() {
        LOG.info("Cargando UltraAddon en 1.21.4...");

        // Registro seguro
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

