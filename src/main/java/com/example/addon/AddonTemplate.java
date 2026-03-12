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
        Modules.registerCategory(CATEGORY);

        Modules m = Modules.get();
        m.add(new SuperAura());
        m.add(new TotemGuard());
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}

