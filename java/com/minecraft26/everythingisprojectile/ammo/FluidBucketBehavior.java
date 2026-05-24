package com.minecraft26.everythingisprojectile.ammo;

import com.minecraft26.everythingisprojectile.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

final class FluidBucketBehavior implements AmmoBehavior {
    private static final ProjectileTraits WATER_TRAITS = new ProjectileTraits(
        0.96D, 0.9D, 1.0D, 0.92D, 0.2D, 0.0D, 0.0D, 0.0D, 0.2D, 0.9D, 0.9D, false, false, 0.0D, false, 0.45D, 0
    );
    private static final ProjectileTraits LAVA_TRAITS = new ProjectileTraits(
        0.9D, 0.86D, 1.04D, 0.9D, 0.35D, 2.0D, 0.0D, 0.0D, 0.45D, 1.1D, 1.0D, false, false, 0.0D, false, 0.45D, 0
    );
    private static final int FLOWING_AMOUNT = 7;
    private static final int CROSS_ARM_LENGTH = 1;

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return AmmoItemSupport.isFluidBucket(stack);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return stack.is(Items.LAVA_BUCKET) ? LAVA_TRAITS : WATER_TRAITS;
    }

    @Override
    public void onHit(HitContext context) {
        ItemStack stack = context.ammoStack();
        if (stack.is(Items.LAVA_BUCKET)) {
            applyLavaEntityHit(context);
        }

        if (context.blockHit() != null && context.level() instanceof ServerLevel serverLevel) {
            placeFluidCross(serverLevel, context.blockHit(), resolveFlowingFluid(stack));
            playBucketImpactEffects(serverLevel, context.blockHit().getLocation().x, context.blockHit().getLocation().y, context.blockHit().getLocation().z, stack);
        }
    }

    private static void applyLavaEntityHit(HitContext context) {
        if (!(context.hitEntity() instanceof LivingEntity living) || !(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity owner = context.projectile().getOwner();
        living.hurt(serverLevel.damageSources().thrown(context.projectile(), owner == null ? context.projectile() : owner), 2.0F);
        living.addEffect(new MobEffectInstance(holder(ModEffects.ON_FIRE.get()), 10, 0));
    }

    private static void placeFluidCross(ServerLevel level, BlockHitResult hitResult, FlowingFluid fluid) {
        BlockPos origin = resolveOrigin(level, hitResult, fluid);
        FluidState flowingState = fluid.getFlowing(FLOWING_AMOUNT, false);

        tryPlace(level, origin, flowingState);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = origin.relative(direction, CROSS_ARM_LENGTH);
            tryPlace(level, targetPos, flowingState);
        }
    }

    private static BlockPos resolveOrigin(ServerLevel level, BlockHitResult hitResult, FlowingFluid fluid) {
        BlockPos hitPos = hitResult.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        return hitState.canBeReplaced(fluid) ? hitPos : hitPos.relative(hitResult.getDirection());
    }

    private static void tryPlace(ServerLevel level, BlockPos pos, FluidState fluidState) {
        if (!level.isInWorldBounds(pos)) {
            return;
        }

        BlockState existingState = level.getBlockState(pos);
        if (!existingState.isAir() && !existingState.canBeReplaced(fluidState.getType())) {
            return;
        }

        if (level.setBlock(pos, fluidState.createLegacyBlock(), 3)) {
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
        }
    }

    private static void playBucketImpactEffects(ServerLevel level, double x, double y, double z, ItemStack stack) {
        boolean lava = stack.is(Items.LAVA_BUCKET);
        level.playSound(
            null,
            BlockPos.containing(x, y, z),
            lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
            SoundSource.BLOCKS,
            1.0F,
            1.0F
        );
        level.sendParticles(
            lava ? ParticleTypes.LAVA : ParticleTypes.SPLASH,
            x,
            y + 0.1D,
            z,
            lava ? 12 : 16,
            0.22D,
            0.08D,
            0.22D,
            lava ? 0.02D : 0.08D
        );
    }

    private static FlowingFluid resolveFlowingFluid(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET) ? (FlowingFluid) Fluids.LAVA : (FlowingFluid) Fluids.WATER;
    }

    private static Holder<MobEffect> holder(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }
}
