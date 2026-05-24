package com.minecraft26.everythingisprojectile.gauntlet;

import com.minecraft26.everythingisprojectile.item.ProjectileGauntletItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GauntletSlotData {
    private static final String SLOT_TAG = "GauntletSlot";
    private static final String STARTER_GRANTED_TAG = "GauntletStarterGranted";

    private GauntletSlotData() {
    }

    private static RegistryAccess registryAccess(Player player) {
        return player.registryAccess();
    }

    public static ItemStack get(Player player) {
        RegistryAccess access = registryAccess(player);
        var ops = access.createSerializationContext(NbtOps.INSTANCE);
        return player.getPersistentData().read(SLOT_TAG, ItemStack.OPTIONAL_CODEC, ops).orElse(ItemStack.EMPTY);
    }

    public static void set(Player player, ItemStack stack) {
        CompoundTag data = player.getPersistentData();
        ItemStack sanitized = sanitize(stack);
        if (sanitized.isEmpty()) {
            data.remove(SLOT_TAG);
        } else {
            RegistryAccess access = registryAccess(player);
            var ops = access.createSerializationContext(NbtOps.INSTANCE);
            data.store(SLOT_TAG, ItemStack.OPTIONAL_CODEC, ops, sanitized);
        }
    }

    public static boolean mayEquip(ItemStack stack) {
        return stack.isEmpty() || ProjectileGauntletItem.isGauntlet(stack);
    }

    public static boolean isEquipped(Player player) {
        return ProjectileGauntletItem.isGauntlet(get(player));
    }

    public static boolean hasStarterGranted(Player player) {
        return player.getPersistentData().getBooleanOr(STARTER_GRANTED_TAG, false);
    }

    public static void markStarterGranted(Player player) {
        player.getPersistentData().putBoolean(STARTER_GRANTED_TAG, true);
    }

    public static void copyFrom(Player source, Player target) {
        set(target, get(source));
        if (hasStarterGranted(source)) {
            markStarterGranted(target);
        }
    }

    private static ItemStack sanitize(ItemStack stack) {
        if (!mayEquip(stack)) {
            return ItemStack.EMPTY;
        }

        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }
}
