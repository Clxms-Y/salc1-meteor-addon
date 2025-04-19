package com.duper.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class DupeSequencer {
    private boolean isRunning = false;
    private boolean shulkersOnly = false;
    private boolean mountWithoutChest = false;
    private int currentStage = 0;
    private int tickDelay = 0;
    private int lastNonChestSlot = 0;
    private final MinecraftClient client = MinecraftClient.getInstance();

    // Delay fields
    private int mountDelay = 20;
    private int keyPressDelay = 2;
    private int inventoryDelay = 5;
    private int moveItemsDelay = 2;
    private int chestApplyDelay = 2;
    private int dismountDelay = 2;

    // Getters and setters for delays
    public void setMountDelay(int delay) { this.mountDelay = delay; }
    public void setKeyPressDelay(int delay) { this.keyPressDelay = delay; }
    public void setInventoryDelay(int delay) { this.inventoryDelay = delay; }
    public void setMoveItemsDelay(int delay) { this.moveItemsDelay = delay; }
    public void setChestApplyDelay(int delay) { this.chestApplyDelay = delay; }
    public void setDismountDelay(int delay) { this.dismountDelay = delay; }
    public void setShulkersOnly(boolean shulkersOnly) { this.shulkersOnly = shulkersOnly; }
    public void setMountWithoutChest(boolean mountWithoutChest) {
        this.mountWithoutChest = mountWithoutChest;
        if (mountWithoutChest) {
            // When enabling mountWithoutChest, switch to a non-chest slot immediately
            if (client.player != null) {
                lastNonChestSlot = client.player.getInventory().selectedSlot;
                client.player.getInventory().selectedSlot = findSafeSlot();
            }
        }
    }

    public void reset() {
        isRunning = false;
        currentStage = 0;
        tickDelay = 0;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void toggle() {
        isRunning = !isRunning;
        currentStage = 0;
        tickDelay = 0;
        if (isRunning && mountWithoutChest) {
            // When starting, ensure we're on a non-chest slot
            lastNonChestSlot = client.player.getInventory().selectedSlot;
            client.player.getInventory().selectedSlot = findSafeSlot();
        }
        sendDebugMessage("Dupe " + (isRunning ? "Started" : "Stopped") + " (Stage: " + currentStage + ")");
    }

    public void tick() {
        if (!isRunning || --tickDelay > 0) return;

        switch (currentStage) {
            case 0: // Select chest in hotbar (if needed)
                if (!mountWithoutChest) {
                    int chestSlot = findChestInHotbar();
                    if (chestSlot != -1) {
                        sendDebugMessage("Selecting chest in hotbar");
                        client.player.getInventory().selectedSlot = chestSlot;
                        tickDelay = keyPressDelay;
                        currentStage = 1;
                    } else {
                        sendDebugMessage("No chest found in hotbar! Stopping.");
                        isRunning = false;
                    }
                } else {
                    // Ensure we're on a non-chest slot when mounting
                    client.player.getInventory().selectedSlot = findSafeSlot();
                    currentStage = 1;
                }
                break;

            case 1: // Mount donkey
                if (!(client.player.getVehicle() instanceof DonkeyEntity)) {
                    DonkeyEntity nearestDonkey = findNearestDonkey();
                    if (nearestDonkey != null) {
                        sendDebugMessage("Mounting donkey");
                        if (mountWithoutChest && !hasChestInInventory()) {
                            sendDebugMessage("No chest found in inventory! Stopping.");
                            isRunning = false;
                            return;
                        }
                        client.interactionManager.interactEntity(client.player, nearestDonkey, Hand.MAIN_HAND);
                        tickDelay = keyPressDelay;
                        currentStage = 2;
                    } else {
                        sendDebugMessage("No donkey found nearby! Stopping.");
                        isRunning = false;
                    }
                } else {
                    currentStage = 3;
                }
                break;

            case 2: // Check if mounted
                if (client.player.getVehicle() instanceof DonkeyEntity) {
                    tickDelay = mountDelay;
                    currentStage = 3;
                } else {
                    currentStage = 1;
                }
                break;

            case 3: // Open inventory
                if (client.player.getVehicle() instanceof DonkeyEntity) {
                    sendDebugMessage("Opening inventory");
                    client.player.networkHandler.sendPacket(
                        new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY)
                    );
                    tickDelay = inventoryDelay;
                    currentStage = 4;
                } else {
                    currentStage = 1;
                }
                break;

            case 4: // Verify inventory opened
                if (client.currentScreen instanceof HorseScreen) {
                    tickDelay = keyPressDelay;
                    currentStage = 5;
                } else {
                    currentStage = 3;
                }
                break;

            case 5: // Move items to donkey
                if (client.currentScreen instanceof HorseScreen) {
                    sendDebugMessage("Moving items to donkey");
                    moveItemsToDonkey();
                    tickDelay = moveItemsDelay;
                    currentStage = 6;
                }
                break;

            case 6: // Apply chest
                if (client.currentScreen instanceof HorseScreen) {
                    sendDebugMessage("Applying chest");
                    if (mountWithoutChest) {
                        // Store current slot
                        lastNonChestSlot = client.player.getInventory().selectedSlot;
                    }

                    // Find and select chest
                    int chestSlot = findChestInHotbar();
                    if (chestSlot != -1) {
                        client.player.getInventory().selectedSlot = chestSlot;
                    } else {
                        sendDebugMessage("No chest found for applying! Stopping.");
                        isRunning = false;
                        return;
                    }

                    // Apply chest
                    DonkeyEntity donkey = (DonkeyEntity) client.player.getVehicle();
                    client.interactionManager.interactEntity(client.player, donkey, Hand.MAIN_HAND);

                    if (mountWithoutChest) {
                        // Switch back to a safe slot
                        client.player.getInventory().selectedSlot = findSafeSlot();
                    }

                    tickDelay = chestApplyDelay;
                    currentStage = 7;
                }
                break;

            case 7: // Remove items from donkey
                if (client.currentScreen instanceof HorseScreen) {
                    sendDebugMessage("Taking items from donkey");
                    moveItemsFromDonkey();
                    tickDelay = moveItemsDelay;
                    currentStage = 8;
                }
                break;

            case 8: // Close inventory
                if (client.currentScreen != null) {
                    sendDebugMessage("Closing inventory");
                    client.player.closeHandledScreen();
                    tickDelay = keyPressDelay;
                    currentStage = 9;
                }
                break;

            case 9: // Dismount
                if (client.player.getVehicle() instanceof DonkeyEntity) {
                    sendDebugMessage("Dismounting donkey");
                    client.options.sneakKey.setPressed(true);
                    tickDelay = dismountDelay;
                    currentStage = 10;
                } else {
                    currentStage = 0;
                }
                break;

            case 10: // Release dismount key
                client.options.sneakKey.setPressed(false);
                tickDelay = dismountDelay;
                if (client.player.getVehicle() == null) {
                    if (mountWithoutChest) {
                        // Ensure we're on a non-chest slot for next cycle
                        client.player.getInventory().selectedSlot = findSafeSlot();
                    }
                    currentStage = 0;
                } else {
                    currentStage = 9;
                }
                break;
        }
    }

    private int findSafeSlot() {
        // First try to use the last non-chest slot if it's safe
        if (client.player.getInventory().getStack(lastNonChestSlot).getItem() != Items.CHEST) {
            return lastNonChestSlot;
        }

        // Otherwise find the first non-chest slot
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).getItem() != Items.CHEST) {
                return i;
            }
        }

        // If somehow all slots have chests, return slot 0
        return 0;
    }

    private void sendDebugMessage(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§b[AutoDuper] " + message), false);
        }
    }

    private int findChestInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).getItem() == Items.CHEST) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasChestInInventory() {
        // Check hotbar
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).getItem() == Items.CHEST) {
                return true;
            }
        }
        // Check main inventory
        for (int i = 9; i < 36; i++) {
            if (client.player.getInventory().getStack(i).getItem() == Items.CHEST) {
                return true;
            }
        }
        return false;
    }

    private DonkeyEntity findNearestDonkey() {
        double searchRadius = 5.0;
        DonkeyEntity closest = null;
        double closestDistance = searchRadius * searchRadius;

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof DonkeyEntity donkey) {
                double distance = client.player.squaredDistanceTo(entity);
                if (distance < closestDistance && !donkey.hasPassengers()) {
                    closestDistance = distance;
                    closest = donkey;
                }
            }
        }

        if (closest != null) {
            double deltaX = closest.getX() - client.player.getX();
            double deltaY = (closest.getY() + closest.getHeight() / 2) - (client.player.getY() + client.player.getEyeHeight(client.player.getPose()));
            double deltaZ = closest.getZ() - client.player.getZ();

            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float yaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
            float pitch = (float) Math.toDegrees(-Math.atan2(deltaY, horizontalDistance));

            client.player.setYaw(yaw);
            client.player.setPitch(pitch);
        }

        return closest;
    }

    private void moveItemsToDonkey() {
        if (!(client.currentScreen instanceof HorseScreen)) return;

        HorseScreenHandler handler = ((HorseScreen) client.currentScreen).getScreenHandler();

        // Find all shulker slots in player inventory (17-52)
        List<Integer> shulkerSlots = new ArrayList<>();
        for (int slot = 17; slot <= 52; slot++) {
            if (!handler.getSlot(slot).getStack().isEmpty()) {
                String itemId = handler.getSlot(slot).getStack().getItem().toString();
                if (itemId.contains("shulker_box")) {
                    shulkerSlots.add(slot);
                }
            }
        }

        // Move items to donkey inventory (slots 2-16)
        int itemsMoved = 0;
        int shulkerIndex = 0;

        for (int donkeySlot = 2; donkeySlot <= 16 && shulkerIndex < shulkerSlots.size(); donkeySlot++) {
            if (handler.getSlot(donkeySlot).getStack().isEmpty()) {
                int shulkerSlot = shulkerSlots.get(shulkerIndex);

                // Pick up the item
                client.interactionManager.clickSlot(handler.syncId, shulkerSlot, 0, SlotActionType.PICKUP, client.player);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Place in donkey slot
                client.interactionManager.clickSlot(handler.syncId, donkeySlot, 0, SlotActionType.PICKUP, client.player);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!handler.getSlot(donkeySlot).getStack().isEmpty()) {
                    itemsMoved++;
                    shulkerIndex++;
                }
            }
        }
    }

    private void moveItemsFromDonkey() {
        if (!(client.currentScreen instanceof HorseScreen)) return;

        HorseScreenHandler handler = ((HorseScreen) client.currentScreen).getScreenHandler();

        // Move items from donkey inventory (slots 2-16)
        for (int donkeySlot = 2; donkeySlot < 17; donkeySlot++) {
            if (!handler.getSlot(donkeySlot).getStack().isEmpty()) {
                if (shulkersOnly) {
                    String itemId = handler.getSlot(donkeySlot).getStack().getItem().toString();
                    if (!itemId.contains("shulker_box")) continue;
                }

                client.interactionManager.clickSlot(handler.syncId, donkeySlot, 0, SlotActionType.QUICK_MOVE, client.player);
            }
        }
    }
}
