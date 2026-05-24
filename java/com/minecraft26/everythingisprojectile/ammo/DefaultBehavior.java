package com.minecraft26.everythingisprojectile.ammo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DefaultBehavior extends AbstractBlockAmmoBehavior {
    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return true;
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return ProjectileTraits.DEFAULT;
    }
}
