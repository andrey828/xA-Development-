package com.example.addon;

import com.example.addon.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;

public class AddonTemplate extends MeteorAddon {
    public static final Category CATEGORY = new Category("xAddon", Items.MACE.getDefaultStack());

    @Override
    public void onInitialize() {
        Modules m = Modules.get();
        
        m.add(new SuperAura());
        m.add(new SuperTotem());
        m.add(new TotemGuard());
        m.add(new UltraMace());
        m.add(new FareWell());
        m.add(new xABackstep());
        m.add(new xArmor());
        m.add(new MegaAutoTotem());
        m.add(new SuperSwap.java());
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
