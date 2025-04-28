package com.duper;

import com.duper.modules.AutoDuper;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends MeteorAddon {
    public static final Category CATEGORY = new Category("Auto Duper");

    @Override
    public void onInitialize() {
        Modules.get().add(new AutoDuper());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.duper";
    }
}
