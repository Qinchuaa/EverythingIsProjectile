package com.minecraft26.everythingisprojectile.client;

import net.minecraft.world.item.ItemStack;

public final class GauntletSlotClientState {
    private static ItemStack stack = ItemStack.EMPTY;

    private GauntletSlotClientState() {
    }

    public static ItemStack getStack() {
        return stack;
    }

    public static void setStack(ItemStack newStack) {
        stack = newStack.isEmpty() ? ItemStack.EMPTY : newStack.copyWithCount(1);
    }
}
