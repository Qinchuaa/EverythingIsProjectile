package com.minecraft26.everythingisprojectile.ammo;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class ProjectileFurnaceData {
    private static final String PROJECTILE_BURNING_FURNACE_TAG = "ProjectileBurningFurnace";
    private static final String PROJECTILE_BURN_TIME_REMAINING_TAG = "ProjectileBurnTimeRemaining";
    private static final String PROJECTILE_BURN_TIME_TOTAL_TAG = "ProjectileBurnTimeTotal";

    private ProjectileFurnaceData() {
    }

    public static void writeBurningData(ItemStack stack, BlockState state, CompoundTag blockEntityTag) {
        if (!(state.getBlock() instanceof AbstractFurnaceBlock)) {
            return;
        }

        int burnTimeRemaining = blockEntityTag.getIntOr("lit_time_remaining", 0);
        int burnTimeTotal = blockEntityTag.getIntOr("lit_total_time", 0);
        boolean burning = (state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT))
            || burnTimeRemaining > 0;
        if (!burning) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(PROJECTILE_BURNING_FURNACE_TAG, true);
            tag.putInt(PROJECTILE_BURN_TIME_REMAINING_TAG, burnTimeRemaining);
            tag.putInt(PROJECTILE_BURN_TIME_TOTAL_TAG, burnTimeTotal);
        });
    }

    public static int readStoredBurnDuration(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        CompoundTag tag = customData.copyTag();
        if (!tag.getBooleanOr(PROJECTILE_BURNING_FURNACE_TAG, false)) {
            return 0;
        }

        int burnTimeRemaining = tag.getIntOr(PROJECTILE_BURN_TIME_REMAINING_TAG, 0);
        int burnTimeTotal = tag.getIntOr(PROJECTILE_BURN_TIME_TOTAL_TAG, 0);
        return Math.max(burnTimeRemaining, burnTimeTotal);
    }
}
