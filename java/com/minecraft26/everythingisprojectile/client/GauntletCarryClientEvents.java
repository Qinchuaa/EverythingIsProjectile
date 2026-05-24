package com.minecraft26.everythingisprojectile.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

public final class GauntletCarryClientEvents {
    private GauntletCarryClientEvents() {
    }

    public static void register() {
        ScreenEvent.Opening.BUS.addListener((Predicate<ScreenEvent.Opening>) GauntletCarryClientEvents::onScreenOpening);
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener((Predicate<InputEvent.InteractionKeyMappingTriggered>) GauntletCarryClientEvents::onInteractionTriggered);
        InputEvent.MouseScrollingEvent.BUS.addListener((Predicate<InputEvent.MouseScrollingEvent>) GauntletCarryClientEvents::onMouseScrolling);
        InputEvent.Key.BUS.addListener(GauntletCarryClientEvents::onKeyInput);
        TickEvent.ClientTickEvent.Pre.BUS.addListener(GauntletCarryClientEvents::onClientTick);
    }

    private static boolean onScreenOpening(ScreenEvent.Opening event) {
        return GauntletCarryClientState.isCarrying() && event.getScreen() instanceof InventoryScreen;
    }

    private static boolean onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        return GauntletCarryClientState.isCarrying();
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (shouldDisableCrosshairTarget(minecraft)) {
            applyMissHitResult(minecraft);
        }

        if (!GauntletCarryClientState.isCarrying()) {
            return;
        }

        if (minecraft.player == null) {
            return;
        }

        minecraft.player.getInventory().setSelectedSlot(GauntletCarryClientState.getLockedSlot());
        clearKey(minecraft.options.keySwapOffhand);
        clearKey(minecraft.options.keyDrop);
        clearKey(minecraft.options.keyInventory);
        clearKey(minecraft.options.keyPickItem);
        for (KeyMapping hotbarKey : minecraft.options.keyHotbarSlots) {
            clearKey(hotbarKey);
        }
    }

    private static void clearKey(KeyMapping keyMapping) {
        while (keyMapping.consumeClick()) {
            // Drain queued presses so the locked slot does not change next frame.
        }
        keyMapping.setDown(false);
    }

    private static boolean onInteractionTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isUseItem() || minecraft.player == null || minecraft.screen != null || GauntletSlotClientState.getStack().isEmpty()) {
            return false;
        }

        if (GauntletCarryClientState.isCarrying() || !minecraft.player.isShiftKeyDown()) {
            applyMissHitResult(minecraft);
            return false;
        }

        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            NetworkHandler.sendPickupBlock();
        }
        event.setSwingHand(false);
        return true;
    }

    private static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (event.getAction() != InputConstants.PRESS || event.getKey() != GLFW.GLFW_KEY_C) {
            return;
        }

        int modifiers = event.getModifiers();
        boolean ctrlDown = (modifiers & InputConstants.MOD_CONTROL) != 0;
        boolean altDown = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        if (!ctrlDown || !altDown || (!GauntletCarryClientState.isCarrying() && GauntletSlotClientState.getStack().isEmpty())) {
            return;
        }

        NetworkHandler.sendEmergencyEscape();
    }

    private static boolean shouldDisableCrosshairTarget(Minecraft minecraft) {
        return minecraft.player != null
            && minecraft.screen == null
            && !GauntletSlotClientState.getStack().isEmpty()
            && (GauntletCarryClientState.isCarrying() || !minecraft.player.isShiftKeyDown());
    }

    private static void applyMissHitResult(Minecraft minecraft) {
        Vec3 eyePosition = minecraft.player.getEyePosition();
        Vec3 lookDirection = minecraft.player.getLookAngle();
        minecraft.crosshairPickEntity = null;
        minecraft.hitResult = BlockHitResult.miss(
            eyePosition.add(lookDirection.scale(5.0D)),
            minecraft.player.getDirection(),
            minecraft.player.blockPosition()
        );
    }
}
