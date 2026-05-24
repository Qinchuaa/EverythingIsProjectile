package com.minecraft26.everythingisprojectile.gauntlet;

import com.minecraft26.everythingisprojectile.ammo.ProjectileFurnaceData;
import com.minecraft26.everythingisprojectile.item.ProjectileGauntletItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import org.jspecify.annotations.Nullable;

public final class GauntletCarryData {
    private static final String CARRIED_STACK_TAG = "GauntletCarriedStack";
    private static final String ORIGINAL_MAIN_HAND_TAG = "GauntletOriginalMainHand";
    private static final String ORIGINAL_OFF_HAND_TAG = "GauntletOriginalOffHand";
    private static final String LOCKED_SLOT_TAG = "GauntletLockedSlot";
    private static final String ORIGIN_BLOCK_STATE_TAG = "GauntletOriginBlockState";
    private static final String ORIGIN_DIMENSION_TAG = "GauntletOriginDimension";
    private static final String ORIGIN_POS_X_TAG = "GauntletOriginPosX";
    private static final String ORIGIN_POS_Y_TAG = "GauntletOriginPosY";
    private static final String ORIGIN_POS_Z_TAG = "GauntletOriginPosZ";

    private GauntletCarryData() {
    }

    public static boolean hasCarriedBlock(Player player) {
        return !getCarriedStack(player).isEmpty();
    }

    public static ItemStack getCarriedStack(Player player) {
        return player.getPersistentData().read(CARRIED_STACK_TAG, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    public static int getLockedSlot(Player player) {
        return player.getPersistentData().getIntOr(LOCKED_SLOT_TAG, player.getInventory().getSelectedSlot());
    }

    public static ItemStack prepareCarriedStack(Level level, BlockPos pos, BlockState state, ItemStack carriedStack) {
        ItemStack sanitized = sanitizeCarriedStack(carriedStack);
        if (sanitized.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return captureCarriedBlockData(level, pos, state, sanitized);
    }

    public static void startCarrying(Player player, ItemStack carriedStack, BlockState blockState, BlockPos pos) {
        ItemStack sanitized = sanitizeCarriedStack(carriedStack);
        if (sanitized.isEmpty()) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        data.store(CARRIED_STACK_TAG, ItemStack.OPTIONAL_CODEC, sanitized);
        data.store(ORIGINAL_MAIN_HAND_TAG, ItemStack.OPTIONAL_CODEC, player.getMainHandItem().copy());
        data.store(ORIGINAL_OFF_HAND_TAG, ItemStack.OPTIONAL_CODEC, player.getOffhandItem().copy());
        data.putInt(LOCKED_SLOT_TAG, player.getInventory().getSelectedSlot());
        data.put(ORIGIN_BLOCK_STATE_TAG, NbtUtils.writeBlockState(blockState));
        data.putString(ORIGIN_DIMENSION_TAG, player.level().dimension().identifier().toString());
        data.putInt(ORIGIN_POS_X_TAG, pos.getX());
        data.putInt(ORIGIN_POS_Y_TAG, pos.getY());
        data.putInt(ORIGIN_POS_Z_TAG, pos.getZ());
        applyCarriedHands(player);
    }

    public static void applyCarriedHands(Player player) {
        ItemStack carriedStack = getCarriedStack(player);
        if (carriedStack.isEmpty()) {
            return;
        }

        player.getInventory().setSelectedSlot(getLockedSlot(player));
        ItemStack handStack = carriedStack.copyWithCount(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, handStack.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, handStack.copy());
    }

    public static void restoreOriginalHands(Player player) {
        if (!hasCarriedBlock(player)) {
            return;
        }

        player.getInventory().setSelectedSlot(getLockedSlot(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, getStoredStack(player, ORIGINAL_MAIN_HAND_TAG));
        player.setItemInHand(InteractionHand.OFF_HAND, getStoredStack(player, ORIGINAL_OFF_HAND_TAG));
        clear(player);
    }

    public static void clear(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(CARRIED_STACK_TAG);
        data.remove(ORIGINAL_MAIN_HAND_TAG);
        data.remove(ORIGINAL_OFF_HAND_TAG);
        data.remove(LOCKED_SLOT_TAG);
        data.remove(ORIGIN_BLOCK_STATE_TAG);
        data.remove(ORIGIN_DIMENSION_TAG);
        data.remove(ORIGIN_POS_X_TAG);
        data.remove(ORIGIN_POS_Y_TAG);
        data.remove(ORIGIN_POS_Z_TAG);
    }

    public static boolean tryPickupBlock(ServerPlayer player, BlockPos pos) {
        if (!GauntletSlotData.isEquipped(player) || player.getCooldowns().isOnCooldown(GauntletSlotData.get(player))) {
            return false;
        }

        BlockState blockState = player.level().getBlockState(pos);
        if (!ProjectileGauntletItem.canCarry(GauntletSlotData.get(player), blockState)) {
            return false;
        }
        ItemStack pickedStack = blockState.getCloneItemStack(player.level(), pos, false);
        if (pickedStack.isEmpty() || !(pickedStack.getItem() instanceof BlockItem)) {
            return false;
        }
        ItemStack preparedStack = prepareCarriedStack(player.level(), pos, blockState, pickedStack);
        if (preparedStack.isEmpty()) {
            return false;
        }
        if (player.level().destroyBlock(pos, false, player)) {
            startCarrying(player, preparedStack, blockState, pos);
            return true;
        }
        return false;
    }

    public static void copyFrom(Player source, Player target) {
        ItemStack carried = getCarriedStack(source);
        if (carried.isEmpty()) {
            clear(target);
            return;
        }

        CompoundTag targetData = target.getPersistentData();
        targetData.store(CARRIED_STACK_TAG, ItemStack.OPTIONAL_CODEC, carried);
        targetData.store(ORIGINAL_MAIN_HAND_TAG, ItemStack.OPTIONAL_CODEC, getStoredStack(source, ORIGINAL_MAIN_HAND_TAG));
        targetData.store(ORIGINAL_OFF_HAND_TAG, ItemStack.OPTIONAL_CODEC, getStoredStack(source, ORIGINAL_OFF_HAND_TAG));
        targetData.putInt(LOCKED_SLOT_TAG, getLockedSlot(source));
        source.getPersistentData().getCompound(ORIGIN_BLOCK_STATE_TAG).ifPresent(tag -> targetData.put(ORIGIN_BLOCK_STATE_TAG, tag.copy()));
        getOriginDimension(source).ifPresent(dimension -> targetData.putString(ORIGIN_DIMENSION_TAG, dimension.identifier().toString()));
        BlockPos originPos = getOriginPos(source);
        if (originPos != null) {
            targetData.putInt(ORIGIN_POS_X_TAG, originPos.getX());
            targetData.putInt(ORIGIN_POS_Y_TAG, originPos.getY());
            targetData.putInt(ORIGIN_POS_Z_TAG, originPos.getZ());
        }
    }

    public static boolean restoreCarriedBlock(ServerPlayer player) {
        ItemStack carried = getCarriedStack(player);
        BlockPos originPos = getOriginPos(player);
        ServerLevel originLevel = getOriginLevel(player);
        if (carried.isEmpty() || originPos == null || originLevel == null) {
            return false;
        }

        BlockState originState = getOriginBlockState(originLevel, player);
        if (originState.isAir()) {
            originState = carried.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState();
        }

        if (originState.isAir()) {
            return false;
        }

        if (!originLevel.setBlock(originPos, originState, 3)) {
            return false;
        }

        applyStoredBlockEntityData(originLevel, originPos, carried);
        return true;
    }

    private static ItemStack getStoredStack(Player player, String key) {
        return player.getPersistentData().read(key, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    private static BlockState getOriginBlockState(ServerLevel level, Player player) {
        return player.getPersistentData()
            .getCompound(ORIGIN_BLOCK_STATE_TAG)
            .map(tag -> NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), tag))
            .orElse(Blocks.AIR.defaultBlockState());
    }

    private static @Nullable BlockPos getOriginPos(Player player) {
        if (player.getPersistentData().getCompound(ORIGIN_BLOCK_STATE_TAG).isEmpty()) {
            return null;
        }

        CompoundTag data = player.getPersistentData();
        return new BlockPos(
            data.getIntOr(ORIGIN_POS_X_TAG, 0),
            data.getIntOr(ORIGIN_POS_Y_TAG, 0),
            data.getIntOr(ORIGIN_POS_Z_TAG, 0)
        );
    }

    private static @Nullable ServerLevel getOriginLevel(ServerPlayer player) {
        return getOriginDimension(player)
            .map(dimension -> player.level().getServer().getLevel(dimension))
            .orElse(player.level());
    }

    private static java.util.Optional<ResourceKey<Level>> getOriginDimension(Player player) {
        return player.getPersistentData()
            .getString(ORIGIN_DIMENSION_TAG)
            .flatMap(value -> {
                Identifier id = Identifier.tryParse(value);
                return id == null ? java.util.Optional.empty() : java.util.Optional.of(ResourceKey.create(Registries.DIMENSION, id));
            });
    }

    private static ItemStack sanitizeCarriedStack(ItemStack stack) {
        return stack.isEmpty() || !(stack.getItem() instanceof BlockItem) ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private static ItemStack captureCarriedBlockData(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        BlockState carriedState = normalizedCarriedState(state);
        applyCarriedBlockState(stack, carriedState);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !supportsStoredContainerData(state, blockEntity)) {
            return stack;
        }

        CompoundTag blockEntityTag = blockEntity.saveCustomOnly(level.registryAccess());
        stack.set(
            DataComponents.BLOCK_ENTITY_DATA,
            TypedEntityData.of(blockEntity.getType(), blockEntityTag)
        );
        ProjectileFurnaceData.writeBurningData(stack, state, blockEntityTag);
        if (shouldSuppressContentDrops(blockEntity)) {
            ((BaseContainerBlockEntity) blockEntity).clearContent();
            blockEntity.setChanged();
        }
        return stack;
    }

    private static void applyStoredBlockEntityData(ServerLevel level, BlockPos pos, ItemStack stack) {
        TypedEntityData<net.minecraft.world.level.block.entity.BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.getType() == data.type()) {
            data.loadInto(blockEntity, level.registryAccess());
        }
    }

    private static boolean supportsStoredContainerData(BlockState state, BlockEntity blockEntity) {
        if (state.is(Blocks.ENDER_CHEST)) {
            return false;
        }

        return blockEntity instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
            || blockEntity instanceof AbstractFurnaceBlockEntity;
    }

    private static boolean shouldSuppressContentDrops(BlockEntity blockEntity) {
        return blockEntity instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
            || blockEntity instanceof AbstractFurnaceBlockEntity;
    }

    private static BlockState normalizedCarriedState(BlockState state) {
        if (state.getBlock() instanceof AbstractFurnaceBlock && state.hasProperty(AbstractFurnaceBlock.LIT)) {
            return state.setValue(AbstractFurnaceBlock.LIT, false);
        }
        return state;
    }

    private static void applyCarriedBlockState(ItemStack stack, BlockState state) {
        BlockItemStateProperties properties = BlockItemStateProperties.EMPTY;
        for (Property<?> property : state.getProperties()) {
            properties = withStateProperty(properties, state, property);
        }

        if (properties.isEmpty()) {
            stack.remove(DataComponents.BLOCK_STATE);
            return;
        }

        stack.set(DataComponents.BLOCK_STATE, properties);
    }

    private static <T extends Comparable<T>> BlockItemStateProperties withStateProperty(
        BlockItemStateProperties properties,
        BlockState state,
        Property<T> property
    ) {
        return properties.with(property, state);
    }
}
