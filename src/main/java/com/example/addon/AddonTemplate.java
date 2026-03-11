package com.example.addon;

import com.example.addon.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class AddonTemplate extends MeteorAddon {
    // Nombre limpio para que no salga el §b feo que marcaste en la foto
    public static final Category CATEGORY = new Category("xAddon");

    @Override
    public void onInitialize() {
        // Solo registramos los que funcionan al 100%
        Modules m = Modules.get();
        m.add(new SuperAura());
        m.add(new TotemGuard());
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
