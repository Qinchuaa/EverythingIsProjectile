package com.minecraft26.everythingisprojectile.item;

import com.minecraft26.everythingisprojectile.gauntlet.GauntletSlotData;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ProjectileGauntletItem extends Item {
    private final Tier tier;

    public ProjectileGauntletItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (quickEquip(level, player, hand, heldStack)) {
                return successResult(level);
            }

            if (quickUnequip(level, player)) {
                return successResult(level);
            }

            return InteractionResult.FAIL;
        }

        return quickEquip(level, player, hand, heldStack) ? successResult(level) : InteractionResult.FAIL;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    public Tier tier() {
        return this.tier;
    }

    public double launchVelocityMultiplier() {
        return this.tier.launchVelocityMultiplier();
    }

    public boolean isUnbreakable() {
        return this.tier.unbreakable();
    }

    public boolean canCarry(BlockState state) {
        return !state.is(Blocks.BEDROCK) || this.tier.canCarryBedrock();
    }

    public static boolean isGauntlet(ItemStack stack) {
        return stack.getItem() instanceof ProjectileGauntletItem;
    }

    public static double launchVelocityMultiplier(ItemStack stack) {
        return stack.getItem() instanceof ProjectileGauntletItem gauntlet ? gauntlet.launchVelocityMultiplier() : 1.0D;
    }

    public static boolean isUnbreakable(ItemStack stack) {
        return stack.getItem() instanceof ProjectileGauntletItem gauntlet && gauntlet.isUnbreakable();
    }

    public static boolean canCarry(ItemStack stack, BlockState state) {
        return stack.getItem() instanceof ProjectileGauntletItem gauntlet && gauntlet.canCarry(state);
    }

    private static boolean quickEquip(Level level, Player player, InteractionHand hand, ItemStack heldStack) {
        if (!level.isClientSide()) {
            ItemStack previous = GauntletSlotData.get(player);
            ItemStack equipped = heldStack.copyWithCount(1);
            GauntletSlotData.set(player, equipped);
            removeOneFromHand(player, hand, heldStack);
            storeOrDrop(player, previous);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), com.minecraft26.everythingisprojectile.registry.ModSounds.GAUNTLET_EQUIP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            syncSlot(player);
        }

        return true;
    }

    private static boolean quickUnequip(Level level, Player player) {
        ItemStack slotted = GauntletSlotData.get(player);
        if (slotted.isEmpty()) {
            return false;
        }

        if (!level.isClientSide()) {
            ItemStack removed = slotted.copy();
            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
            GauntletSlotData.set(player, ItemStack.EMPTY);
            syncSlot(player);
        }

        return true;
    }

    private static void removeOneFromHand(Player player, InteractionHand hand, ItemStack heldStack) {
        if (heldStack.getCount() <= 1) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            return;
        }

        ItemStack remaining = heldStack.copy();
        remaining.shrink(1);
        player.setItemInHand(hand, remaining);
    }

    private static void storeOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    public static void syncSlot(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.syncGauntletSlot(serverPlayer, GauntletSlotData.get(serverPlayer));
            serverPlayer.containerMenu.broadcastChanges();
        }
    }

    private static InteractionResult successResult(Level level) {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    public enum Tier {
        LEATHER(1.00D, false, false),
        IRON(1.30D, false, false),
        DIAMOND(1.50D, false, false),
        CREATIVE(2.00D, true, true);

        private final double launchVelocityMultiplier;
        private final boolean unbreakable;
        private final boolean canCarryBedrock;

        Tier(double launchVelocityMultiplier, boolean unbreakable, boolean canCarryBedrock) {
            this.launchVelocityMultiplier = launchVelocityMultiplier;
            this.unbreakable = unbreakable;
            this.canCarryBedrock = canCarryBedrock;
        }

        public double launchVelocityMultiplier() {
            return this.launchVelocityMultiplier;
        }

        public boolean unbreakable() {
            return this.unbreakable;
        }

        public boolean canCarryBedrock() {
            return this.canCarryBedrock;
        }
    }
}
