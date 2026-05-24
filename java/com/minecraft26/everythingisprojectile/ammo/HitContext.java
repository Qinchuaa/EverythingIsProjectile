package com.minecraft26.everythingisprojectile.ammo;

import com.minecraft26.everythingisprojectile.entity.AnyProjectileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public record HitContext(
    Level level,
    AnyProjectileEntity projectile,
    ItemStack ammoStack,
    HitResult hitResult
) {
    @Nullable
    public Entity hitEntity() {
        return hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
    }

    @Nullable
    public BlockHitResult blockHit() {
        return hitResult instanceof BlockHitResult blockHitResult ? blockHitResult : null;
    }
}
