package com.minecraft26.everythingisprojectile.ammo;

import com.minecraft26.everythingisprojectile.registry.ModEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

final class ImpactEffectResolver {
    private static final int SECOND = 20;
    private static final int FLOWING_AMOUNT = 7;

    private ImpactEffectResolver() {
    }

    static void applyDirectHitEffects(HitContext context, @Nullable BlockState state) {
        if (!(context.hitEntity() instanceof LivingEntity living) || state == null || context.level().isClientSide()) {
            return;
        }

        int headhurtDuration = resolveHeadhurtDuration(context.ammoStack(), state);
        if (headhurtDuration > 0) {
            applyEffect(living, ModEffects.HEADHURT.get(), headhurtDuration);
            return;
        }

        if (isFreezingBlock(state)) {
            applyEffect(living, ModEffects.FREEZING.get(), freezingDuration(state));
            return;
        }

        if (isBleedingBlock(state)) {
            applyEffect(living, ModEffects.BLEEDING.get(), bleedingDuration(state));
            return;
        }

        if (isBlindingBlock(state)) {
            applyVanillaEffect(living, MobEffects.BLINDNESS, blindingDuration(state));
            return;
        }

        if (isHotBlock(state)) {
            applyEffect(living, ModEffects.ON_FIRE.get(), fireDuration(state));
            return;
        }

        if (isNauseatingBlock(state)) {
            applyEffect(living, ModEffects.NAUSEA.get(), nauseaDuration(state));
        }
    }

    static int resolveHeadhurtDuration(ItemStack ammoStack, @Nullable BlockState state) {
        if (state == null) {
            return 0;
        }

        int headhurtDuration = 0;
        if (BlockAmmoSupport.isUltraHeavy(state) || BlockAmmoSupport.isHeavy(state)) {
            headhurtDuration += headhurtDuration(state);
        }
        headhurtDuration += containerImpactHeadhurtDuration(ammoStack, state);
        return headhurtDuration;
    }

    static void applyCustomEffect(LivingEntity living, MobEffect effect, int durationTicks) {
        if (durationTicks > 0) {
            applyEffect(living, effect, durationTicks);
        }
    }

    static void applyLandingBlockEffects(HitContext context, @Nullable BlockState state) {
        if (!(context.level() instanceof ServerLevel serverLevel) || state == null || context.blockHit() == null) {
            return;
        }

        if (isHotBlock(state)) {
            placeFireCross(serverLevel, context.blockHit(), state);
        }
        if (isFreezingBlock(state)) {
            placeWaterCross(serverLevel, context.blockHit());
        }
    }

    private static void applyEffect(LivingEntity living, MobEffect effect, int durationTicks) {
        living.addEffect(new MobEffectInstance(holder(effect), durationTicks, 0));
    }

    private static void applyVanillaEffect(LivingEntity living, net.minecraft.core.Holder<MobEffect> effect, int durationTicks) {
        living.addEffect(new MobEffectInstance(effect, durationTicks, 0));
    }

    private static boolean isBleedingBlock(BlockState state) {
        SoundType soundType = state.getSoundType();
        return soundType == SoundType.GLASS
            || state.is(Blocks.CACTUS)
            || state.is(Blocks.GLASS)
            || state.is(Blocks.TINTED_GLASS);
    }

    static boolean isHotBlock(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK)
            || state.is(Blocks.FIRE)
            || state.is(Blocks.SOUL_FIRE)
            || state.is(Blocks.TORCH)
            || state.is(Blocks.WALL_TORCH)
            || state.is(Blocks.SOUL_TORCH)
            || state.is(Blocks.SOUL_WALL_TORCH)
            || state.is(Blocks.CAMPFIRE) && state.getValue(CampfireBlock.LIT)
            || state.is(Blocks.SOUL_CAMPFIRE) && state.getValue(CampfireBlock.LIT);
    }

    private static boolean isBlindingBlock(BlockState state) {
        return BlockAmmoSupport.isSandLike(state);
    }

    private static boolean isNauseatingBlock(BlockState state) {
        return state.is(Blocks.BROWN_MUSHROOM)
            || state.is(Blocks.RED_MUSHROOM)
            || state.is(Blocks.CRIMSON_FUNGUS)
            || state.is(Blocks.WARPED_FUNGUS)
            || state.is(Blocks.NETHER_WART_BLOCK)
            || state.is(Blocks.WARPED_WART_BLOCK)
            || state.is(Blocks.SHROOMLIGHT);
    }

    static boolean isFreezingBlock(BlockState state) {
        return BlockAmmoSupport.isIce(state)
            || state.is(Blocks.POWDER_SNOW)
            || state.is(Blocks.SNOW_BLOCK)
            || state.is(Blocks.SNOW);
    }

    private static int headhurtDuration(BlockState state) {
        SoundType soundType = state.getSoundType();
        if (state.is(Blocks.NETHERITE_BLOCK)) {
            return 24 * SECOND;
        }
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            return 22 * SECOND;
        }
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.ENDER_CHEST)) {
            return 20 * SECOND;
        }
        if (isAnvilSeries(state) || soundType == SoundType.NETHERITE_BLOCK) {
            return 18 * SECOND;
        }
        if (isIronHeavySeries(state)) {
            return 15 * SECOND;
        }
        if (isCopperSeries(state)) {
            return 13 * SECOND;
        }
        if (isBlackstoneSeries(state)) {
            return 11 * SECOND;
        }
        if (soundType == SoundType.DEEPSLATE) {
            return 10 * SECOND;
        }
        if (isStoneBrickSeries(state)) {
            return 9 * SECOND;
        }
        if (isStoneSeries(state)) {
            return 8 * SECOND;
        }
        if (soundType == SoundType.STONE) {
            return 7 * SECOND;
        }
        return 6 * SECOND;
    }

    private static int nauseaDuration(BlockState state) {
        if (state.is(Blocks.NETHER_WART_BLOCK) || state.is(Blocks.WARPED_WART_BLOCK)) {
            return 22 * SECOND;
        }
        if (state.is(Blocks.SHROOMLIGHT)) {
            return 18 * SECOND;
        }
        if (state.is(Blocks.CRIMSON_FUNGUS) || state.is(Blocks.WARPED_FUNGUS)) {
            return 12 * SECOND;
        }
        return 8 * SECOND;
    }

    private static int freezingDuration(BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) {
            return 24 * SECOND;
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return 20 * SECOND;
        }
        if (state.is(Blocks.FROSTED_ICE)) {
            return 16 * SECOND;
        }
        if (state.is(Blocks.ICE)) {
            return 12 * SECOND;
        }
        if (state.is(Blocks.POWDER_SNOW)) {
            return 10 * SECOND;
        }
        if (state.is(Blocks.SNOW_BLOCK)) {
            return 8 * SECOND;
        }
        if (state.is(Blocks.SNOW)) {
            return 6 * SECOND;
        }
        return 15 * SECOND;
    }

    private static int bleedingDuration(BlockState state) {
        if (state.is(Blocks.CACTUS)) {
            return 18 * SECOND;
        }
        if (state.is(Blocks.TINTED_GLASS)) {
            return 15 * SECOND;
        }
        return 10 * SECOND;
    }

    private static int blindingDuration(BlockState state) {
        if (state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)) {
            return 6 * SECOND;
        }
        if (state.is(Blocks.GRAVEL)) {
            return 4 * SECOND;
        }
        if (state.is(Blocks.RED_SAND)) {
            return 4 * SECOND;
        }
        return 3 * SECOND;
    }

    private static int fireDuration(BlockState state) {
        if (state.is(Blocks.SOUL_FIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return (5 * SECOND) / 2;
        }
        if (state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
            return (4 * SECOND) / 2;
        }
        if (state.is(Blocks.MAGMA_BLOCK)) {
            return (4 * SECOND) / 2;
        }
        if (state.is(Blocks.CAMPFIRE)) {
            return (4 * SECOND) / 2;
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
            return (3 * SECOND) / 2;
        }
        if (state.is(Blocks.FIRE)) {
            return (3 * SECOND) / 2;
        }
        return SECOND;
    }

    private static boolean isAnvilSeries(BlockState state) {
        return state.is(Blocks.ANVIL)
            || state.is(Blocks.CHIPPED_ANVIL)
            || state.is(Blocks.DAMAGED_ANVIL);
    }

    private static boolean isIronHeavySeries(BlockState state) {
        return state.is(Blocks.IRON_BLOCK)
            || state.is(Blocks.RAW_IRON_BLOCK)
            || state.is(Blocks.GOLD_BLOCK)
            || state.is(Blocks.RAW_GOLD_BLOCK)
            || state.is(Blocks.DIAMOND_BLOCK)
            || state.is(Blocks.EMERALD_BLOCK)
            || state.is(Blocks.LAPIS_BLOCK);
    }

    private static boolean isCopperSeries(BlockState state) {
        return state.is(Blocks.COPPER_BLOCK)
            || state.is(Blocks.EXPOSED_COPPER)
            || state.is(Blocks.WEATHERED_COPPER)
            || state.is(Blocks.OXIDIZED_COPPER)
            || state.is(Blocks.CUT_COPPER)
            || state.is(Blocks.EXPOSED_CUT_COPPER)
            || state.is(Blocks.WEATHERED_CUT_COPPER)
            || state.is(Blocks.OXIDIZED_CUT_COPPER);
    }

    private static boolean isBlackstoneSeries(BlockState state) {
        return state.is(Blocks.BLACKSTONE)
            || state.is(Blocks.POLISHED_BLACKSTONE)
            || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
            || state.is(Blocks.BASALT)
            || state.is(Blocks.POLISHED_BASALT)
            || state.is(Blocks.SMOOTH_BASALT);
    }

    private static boolean isStoneBrickSeries(BlockState state) {
        return state.is(Blocks.STONE_BRICKS)
            || state.is(Blocks.MOSSY_STONE_BRICKS)
            || state.is(Blocks.CRACKED_STONE_BRICKS)
            || state.is(Blocks.CHISELED_STONE_BRICKS)
            || state.is(Blocks.DEEPSLATE_BRICKS)
            || state.is(Blocks.DEEPSLATE_TILES);
    }

    private static boolean isStoneSeries(BlockState state) {
        return state.is(Blocks.STONE)
            || state.is(Blocks.SMOOTH_STONE)
            || state.is(Blocks.COBBLESTONE)
            || state.is(Blocks.MOSSY_COBBLESTONE)
            || state.is(Blocks.GRANITE)
            || state.is(Blocks.POLISHED_GRANITE)
            || state.is(Blocks.DIORITE)
            || state.is(Blocks.POLISHED_DIORITE)
            || state.is(Blocks.ANDESITE)
            || state.is(Blocks.POLISHED_ANDESITE)
            || state.is(Blocks.TUFF)
            || state.is(Blocks.CALCITE);
    }

    private static int containerImpactHeadhurtDuration(ItemStack ammoStack, BlockState state) {
        if (state.is(Blocks.ENDER_CHEST)) {
            return 0;
        }

        TypedEntityData<BlockEntityType<?>> blockEntityData = ammoStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || !isContainerBlockWithStoredItems(state, blockEntityData)) {
            return 0;
        }

        int totalItems = countStoredContainerItems(blockEntityData.copyTagWithoutId());
        if (totalItems <= 0) {
            return 0;
        }

        int extraSeconds = Math.min(20, (totalItems + 31) / 32);
        return extraSeconds * SECOND;
    }

    private static boolean isContainerBlockWithStoredItems(BlockState state, TypedEntityData<BlockEntityType<?>> blockEntityData) {
        return state.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock
            && blockEntityData.contains("Items");
    }

    private static int countStoredContainerItems(CompoundTag blockEntityTag) {
        int total = 0;
        for (net.minecraft.nbt.Tag itemEntryTag : blockEntityTag.getListOrEmpty("Items")) {
            if (itemEntryTag instanceof CompoundTag itemEntry) {
                ItemStack storedStack = itemEntry.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
                total += storedStack.getCount();
            }
        }
        return total;
    }

    private static net.minecraft.core.Holder<MobEffect> holder(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    private static void placeFireCross(ServerLevel level, BlockHitResult hitResult, BlockState sourceState) {
        BlockPos origin = resolveSurfaceOrigin(level, hitResult);
        tryPlaceFire(level, origin, sourceState);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            tryPlaceFire(level, origin.relative(direction), sourceState);
        }
    }

    private static void tryPlaceFire(ServerLevel level, BlockPos pos, BlockState sourceState) {
        if (!level.isInWorldBounds(pos)) {
            return;
        }

        BlockState existingState = level.getBlockState(pos);
        if (!existingState.isAir() && !existingState.canBeReplaced()) {
            return;
        }

        BlockState fireState = isSoulHotBlock(sourceState) ? Blocks.SOUL_FIRE.defaultBlockState() : BaseFireBlock.getState(level, pos);
        if (fireState.canSurvive(level, pos)) {
            level.setBlock(pos, fireState, 3);
        }
    }

    private static void placeWaterCross(ServerLevel level, BlockHitResult hitResult) {
        BlockPos origin = resolveSurfaceOrigin(level, hitResult);
        FluidState flowingState = ((FlowingFluid) Fluids.WATER).getFlowing(FLOWING_AMOUNT, false);
        tryPlaceFlowingWater(level, origin, flowingState);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            tryPlaceFlowingWater(level, origin.relative(direction), flowingState);
        }
    }

    private static void tryPlaceFlowingWater(ServerLevel level, BlockPos pos, FluidState fluidState) {
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

    private static BlockPos resolveSurfaceOrigin(ServerLevel level, BlockHitResult hitResult) {
        BlockPos hitPos = hitResult.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        return hitState.canBeReplaced() ? hitPos : hitPos.relative(hitResult.getDirection());
    }

    private static boolean isSoulHotBlock(BlockState state) {
        return state.is(Blocks.SOUL_FIRE)
            || state.is(Blocks.SOUL_CAMPFIRE)
            || state.is(Blocks.SOUL_TORCH)
            || state.is(Blocks.SOUL_WALL_TORCH);
    }
}
