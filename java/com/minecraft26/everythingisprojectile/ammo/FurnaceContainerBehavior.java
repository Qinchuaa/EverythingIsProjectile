package com.minecraft26.everythingisprojectile.ammo;

import com.minecraft26.everythingisprojectile.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

final class FurnaceContainerBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(
        0.72D, 0.96D, 1.18D, 0.86D, 1.65D, 6.0D, 0.0D, 0.0D, 1.3D, 0.95D, 1.15D, true, true, 0.014D, false, 0.45D, 0
    );
    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int MAX_FIRE_DURATION = 20 * 20;
    private static final double BLOCK_IMPACT_EFFECT_RADIUS = 2.5D;

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && isSupportedFurnace(state);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }

    @Override
    protected void afterHit(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
        if (!(context.level() instanceof ServerLevel serverLevel) || state == null) {
            return;
        }

        Optional<FurnacePayload> payload = FurnacePayload.from(serverLevel, context.ammoStack(), state);
        if (payload.isEmpty()) {
            return;
        }

        FurnacePayload furnacePayload = payload.get();
        spawnOutputs(serverLevel, context, furnacePayload.outputStack());
        applyFurnaceImpactEffects(context, state, furnacePayload.fireDurationTicks());
    }

    private static void applyFurnaceImpactEffects(HitContext context, BlockState state, int fireDurationTicks) {
        int headhurtDuration = ImpactEffectResolver.resolveHeadhurtDuration(context.ammoStack(), state);
        if (context.hitEntity() instanceof LivingEntity directLiving) {
            ImpactEffectResolver.applyCustomEffect(directLiving, ModEffects.ON_FIRE.get(), fireDurationTicks);
            ImpactEffectResolver.applyCustomEffect(directLiving, ModEffects.HEADHURT.get(), headhurtDuration);
            return;
        }

        if (headhurtDuration <= 0 && fireDurationTicks <= 0) {
            return;
        }

        Vec3 center = context.blockHit() != null ? context.blockHit().getLocation() : context.projectile().position();
        Entity owner = context.projectile().getOwner();
        AABB area = new AABB(center, center).inflate(BLOCK_IMPACT_EFFECT_RADIUS);
        for (LivingEntity living : context.level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (living == owner) {
                continue;
            }

            ImpactEffectResolver.applyCustomEffect(living, ModEffects.HEADHURT.get(), headhurtDuration);
            ImpactEffectResolver.applyCustomEffect(living, ModEffects.ON_FIRE.get(), fireDurationTicks);
        }
    }

    private static void spawnOutputs(ServerLevel level, HitContext context, ItemStack outputStack) {
        if (outputStack.isEmpty()) {
            return;
        }

        Vec3 center = context.hitEntity() != null ? context.hitEntity().position() : context.blockHit() != null ? context.blockHit().getLocation() : context.projectile().position();
        int remaining = outputStack.getCount();
        while (remaining > 0) {
            int splitCount = Math.min(remaining, outputStack.getMaxStackSize());
            Containers.dropItemStack(level, center.x, center.y + 0.2D, center.z, outputStack.copyWithCount(splitCount));
            remaining -= splitCount;
        }
    }

    private static boolean isSupportedFurnace(BlockState state) {
        return state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER);
    }

    private record FurnacePayload(ItemStack outputStack, int fireDurationTicks) {
        private static Optional<FurnacePayload> from(ServerLevel level, ItemStack ammoStack, BlockState state) {
            TypedEntityData<BlockEntityType<?>> blockEntityData = ammoStack.get(DataComponents.BLOCK_ENTITY_DATA);
            AbstractFurnaceBlockEntity furnace = createTemporaryFurnace(state);
            if (blockEntityData == null || furnace == null || !blockEntityData.loadInto(furnace, level.registryAccess())) {
                return Optional.empty();
            }

            ItemStack inputStack = furnace.getItem(INPUT_SLOT).copy();
            if (inputStack.isEmpty()) {
                return Optional.empty();
            }

            ItemStack fuelStack = furnace.getItem(FUEL_SLOT).copy();
            int fireDurationTicks = resolveFireDuration(level, ammoStack, fuelStack);
            if (fireDurationTicks <= 0) {
                return Optional.empty();
            }

            ItemStack outputStack = resolveSmeltedOutput(level, state, inputStack);
            if (outputStack.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new FurnacePayload(outputStack, fireDurationTicks));
        }
    }

    private static @Nullable AbstractFurnaceBlockEntity createTemporaryFurnace(BlockState state) {
        BlockPos pos = BlockPos.ZERO;
        if (state.is(Blocks.SMOKER)) {
            return new SmokerBlockEntity(pos, state);
        }
        if (state.is(Blocks.BLAST_FURNACE)) {
            return new BlastFurnaceBlockEntity(pos, state);
        }
        if (state.is(Blocks.FURNACE)) {
            return new FurnaceBlockEntity(pos, state);
        }
        return null;
    }

    private static ItemStack resolveSmeltedOutput(ServerLevel level, BlockState state, ItemStack inputStack) {
        ItemStack singleInput = inputStack.copyWithCount(1);
        SingleRecipeInput recipeInput = new SingleRecipeInput(singleInput);
        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipe = recipeFor(level, state, recipeInput);
        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = recipe.get().value().assemble(recipeInput);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int outputCount = 2 + level.getRandom().nextInt(14);
        return result.copyWithCount(outputCount);
    }

    private static Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipeFor(ServerLevel level, BlockState state, SingleRecipeInput input) {
        if (state.is(Blocks.SMOKER)) {
            return level.recipeAccess().getRecipeFor(RecipeType.SMOKING, input, level);
        }
        if (state.is(Blocks.BLAST_FURNACE)) {
            return level.recipeAccess().getRecipeFor(RecipeType.BLASTING, input, level);
        }
        return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
    }

    private static int resolveFireDuration(ServerLevel level, ItemStack ammoStack, ItemStack fuelStack) {
        int storedDuration = ProjectileFurnaceData.readStoredBurnDuration(ammoStack);
        if (storedDuration > 0) {
            return Math.min(storedDuration / 2, MAX_FIRE_DURATION);
        }

        if (fuelStack.isEmpty()) {
            return 0;
        }

        int burnDuration = level.fuelValues().burnDuration(fuelStack.copyWithCount(1));
        return Math.min(Math.max(burnDuration / 2, 0), MAX_FIRE_DURATION);
    }
}
