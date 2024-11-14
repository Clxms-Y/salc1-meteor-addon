package com.bstar.addon.modules;

import com.bstar.addon.Addon;
import com.bstar.addon.util.DupeSequencer;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HorseScreen;

public class AutoDuper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> mountDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("mount-delay")
        .description("Delay for mounting in seconds.")
        .defaultValue(1.0)
        .min(0)
        .sliderMax(2)
        .build()
    );

    private final Setting<Double> keypressDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("keypress-delay")
        .description("Delay for key presses in seconds.")
        .defaultValue(0.1)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> inventoryDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("inventory-delay")
        .description("Delay for inventory actions in seconds.")
        .defaultValue(0.25)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> moveItemsDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("move-items-delay")
        .description("Delay for moving items in seconds.")
        .defaultValue(0.1)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> chestApplyDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("chest-apply-delay")
        .description("Delay for applying chest in seconds.")
        .defaultValue(0.1)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> dismountDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("dismount-delay")
        .description("Delay for dismounting in seconds.")
        .defaultValue(0.1)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final DupeSequencer sequencer;
    private boolean wasInInventory = false;

    public AutoDuper() {
        super(Addon.CATEGORY, "auto-duper", "Automatically dupes items using donkey method.");
        sequencer = new DupeSequencer();
    }

    @Override
    public void onActivate() {
        updateDelays();
        sequencer.toggle();
        wasInInventory = false;
    }

    @Override
    public void onDeactivate() {
        sequencer.reset();
        wasInInventory = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Check for inventory closed with escape
        if (mc.currentScreen instanceof HorseScreen) {
            wasInInventory = true;
        } else if (wasInInventory && mc.currentScreen == null) {
            // Inventory was closed
            wasInInventory = false;
            if (sequencer.getCurrentStage() >= 4 && sequencer.getCurrentStage() <= 7) {
                // If we were in the middle of inventory operations, disable the module
                toggle();
                info("Inventory closed - stopping dupe sequence");
            }
        }

        sequencer.tick();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    private void updateDelays() {
        // Convert seconds to ticks (20 ticks = 1 second)
        sequencer.setMountDelay((int) (mountDelay.get() * 20));
        sequencer.setKeyPressDelay((int) (keypressDelay.get() * 20));
        sequencer.setInventoryDelay((int) (inventoryDelay.get() * 20));
        sequencer.setMoveItemsDelay((int) (moveItemsDelay.get() * 20));
        sequencer.setChestApplyDelay((int) (chestApplyDelay.get() * 20));
        sequencer.setDismountDelay((int) (dismountDelay.get() * 20));
    }
}
