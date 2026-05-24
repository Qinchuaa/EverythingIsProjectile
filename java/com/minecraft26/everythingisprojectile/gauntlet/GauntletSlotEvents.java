package com.minecraft26.everythingisprojectile.gauntlet;

import com.minecraft26.everythingisprojectile.item.ProjectileGauntletItem;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import com.minecraft26.everythingisprojectile.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class GauntletSlotEvents {
    private GauntletSlotEvents() {
    }

    public static void register() {
        PlayerEvent.Clone.BUS.addListener(GauntletSlotEvents::onClone);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(GauntletSlotEvents::onLoggedIn);
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(GauntletSlotEvents::onRespawn);
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(GauntletSlotEvents::onChangedDimension);
    }

    private static void onClone(PlayerEvent.Clone event) {
        GauntletSlotData.copyFrom(event.getOriginal(), event.getEntity());
        GauntletCarryData.copyFrom(event.getOriginal(), event.getEntity());
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureStarterGauntlet(player);
            NetworkHandler.syncGauntletSlot(player, GauntletSlotData.get(player));
            NetworkHandler.syncGauntletCarry(player, GauntletCarryData.hasCarriedBlock(player), GauntletCarryData.getCarriedStack(player), GauntletCarryData.getLockedSlot(player));
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncGauntletSlot(player, GauntletSlotData.get(player));
            NetworkHandler.syncGauntletCarry(player, GauntletCarryData.hasCarriedBlock(player), GauntletCarryData.getCarriedStack(player), GauntletCarryData.getLockedSlot(player));
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncGauntletSlot(player, GauntletSlotData.get(player));
            NetworkHandler.syncGauntletCarry(player, GauntletCarryData.hasCarriedBlock(player), GauntletCarryData.getCarriedStack(player), GauntletCarryData.getLockedSlot(player));
        }
    }

    private static void ensureStarterGauntlet(ServerPlayer player) {
        ItemStack slotted = GauntletSlotData.get(player);
        if (ProjectileGauntletItem.isGauntlet(slotted)) {
            if (!GauntletSlotData.hasStarterGranted(player)) {
                GauntletSlotData.markStarterGranted(player);
            }
            return;
        }

        if (GauntletSlotData.hasStarterGranted(player)) {
            return;
        }

        GauntletSlotData.set(player, ModItems.starterGauntlet());
        GauntletSlotData.markStarterGranted(player);
    }
}
