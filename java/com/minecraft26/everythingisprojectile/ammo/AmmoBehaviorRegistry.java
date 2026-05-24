package com.minecraft26.everythingisprojectile.ammo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AmmoBehaviorRegistry {
    private static final DefaultBehavior DEFAULT_BEHAVIOR = new DefaultBehavior();
    private static final List<AmmoBehavior> BEHAVIORS = List.of(
        new FluidBucketBehavior(),
        new FurnaceContainerBehavior(),
        new TntBlockBehavior(),
        new BedBlockBehavior(),
        new UltraHeavyBlockBehavior(),
        new IceBlockBehavior(),
        new SandLikeBlockBehavior(),
        new ElasticBlockBehavior(),
        new FragileBlockBehavior(),
        new LuminousBlockBehavior(),
        new HeavyBlockBehavior(),
        new OrganicBlockBehavior(),
        DEFAULT_BEHAVIOR
    );

    private AmmoBehaviorRegistry() {
    }

    public static AmmoBehavior find(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        for (AmmoBehavior behavior : BEHAVIORS) {
            if (behavior.matches(stack, state, blockEntityTag)) {
                return behavior;
            }
        }
        return DEFAULT_BEHAVIOR;
    }

    public static AmmoBehavior findForStack(ItemStack stack) {
        return find(stack, BlockAmmoSupport.resolveBlockState(stack), null);
    }

    public static ProjectileTraits traitsForStack(ItemStack stack) {
        BlockState state = BlockAmmoSupport.resolveBlockState(stack);
        return find(stack, state, null).traits(stack, state, null);
    }
}
