package com.minecraft26.everythingisprojectile.ammo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface AmmoBehavior {
    boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag);

    default ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return ProjectileTraits.DEFAULT;
    }

    void onHit(HitContext context);
}
