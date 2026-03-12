package com.example.addon;

import com.example.addon.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;

public class AddonTemplate extends MeteorAddon {
    public static final Category CATEGORY = new Category("xAddon", Items.DIAMOND_SWORD.getDefaultStack());

    @Override
    public void onInitialize() {
        // Aquí NO registramos la categoría, solo añadimos los módulos
        Modules m = Modules.get();
        m.add(new SuperAura());
        m.add(new TotemGuard());
        m.add(new LanzaDMG());
    }

    @Override
    public void onRegisterCategories() {
        // ESTO es lo que pide el crash. El registro va AQUÍ.
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}

