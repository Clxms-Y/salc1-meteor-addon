package com.bstar.addon;

import com.bstar.addon.modules.AutoDuper;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class Addon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Auto Duper");

    @Override
    public void onInitialize() {
        // Just add the module here
        Modules.get().add(new AutoDuper());
    }

    @Override
    public void onRegisterCategories() {
        // Register the category in the proper callback
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.bstar.addon";
    }
}
