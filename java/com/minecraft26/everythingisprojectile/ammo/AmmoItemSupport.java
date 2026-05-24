package com.minecraft26.everythingisprojectile.ammo;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class AmmoItemSupport {
    private AmmoItemSupport() {
    }

    public static boolean isSupportedAmmo(ItemStack stack) {
        return isThrowableBlock(stack) || isFluidBucket(stack);
    }

    public static boolean isThrowableBlock(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    public static boolean isFluidBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    public static @Nullable BlockState resolveBlockState(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        BlockState state = blockItem.getBlock().defaultBlockState();
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        return properties == null ? state : properties.apply(state);
    }
}
