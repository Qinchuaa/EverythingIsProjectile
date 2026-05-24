package com.minecraft26.everythingisprojectile.client;

import net.minecraft.world.item.ItemStack;

public final class GauntletCarryClientState {
    private static boolean carrying;
    private static ItemStack carriedStack = ItemStack.EMPTY;
    private static int lockedSlot;

    private GauntletCarryClientState() {
    }

    public static boolean isCarrying() {
        return carrying;
    }

    public static ItemStack getCarriedStack() {
        return carriedStack;
    }

    public static int getLockedSlot() {
        return lockedSlot;
    }

    public static void update(boolean isCarrying, ItemStack stack, int slot) {
        carrying = isCarrying && !stack.isEmpty();
        carriedStack = carrying ? stack.copyWithCount(1) : ItemStack.EMPTY;
        lockedSlot = slot;
    }
}
