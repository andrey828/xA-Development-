package com.example.addon;

import com.example.addon.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;

public class AddonTemplate extends MeteorAddon {

    // Categoría principal
    public static final Category CATEGORY = new Category("xA", Items.MACE.getDefaultStack());

    // Categoría visual
    public static final Category VISUALS = new Category("xA Visuals", Items.ENDER_EYE.getDefaultStack());

    @Override
    public void onInitialize() {
        Modules m = Modules.get();

        m.add(new SuperAura());
        m.add(new TotemGuard());
        m.add(new UltraMace());
        m.add(new FareWell());
        m.add(new xABackstep());
        m.add(new xArmor());
        m.add(new MegaAutoTotem());
        m.add(new xRPC());
        m.add(new FlightPlus());
        m.add(new CustomFOV());
        m.add(new JumpCircles());
        m.add(new xCrystal());
        m.add(new xAnchor());
        m.add(new AdvancedParticles());
        m.add(new Aimbot());
        m.add(new TeleportToPlayer());
        m.add(new ClipEscape());
    }
    

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(VISUALS);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
