package com.minecraft26.everythingisprojectile.gauntlet;

import com.minecraft26.everythingisprojectile.ammo.AmmoItemSupport;
import com.minecraft26.everythingisprojectile.ammo.AmmoBehaviorRegistry;
import com.minecraft26.everythingisprojectile.item.ProjectileGauntletItem;
import com.minecraft26.everythingisprojectile.config.ModConfig;
import com.minecraft26.everythingisprojectile.entity.AnyProjectileEntity;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import com.minecraft26.everythingisprojectile.registry.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import com.minecraft26.everythingisprojectile.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

public final class GauntletFiringEvents {
    private static final int MAX_DRAW_DURATION = 72000;
    private static final int MIN_DRAW_DURATION = 10;
    private static final double MIGHT_ENCHANTMENT_VELOCITY_PER_LEVEL = 0.10D;
    private static final KineticWeapon TRIDENT_STYLE_ANIMATION = new KineticWeapon(
        10,
        0,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        0.0F,
        1.0F,
        Optional.empty(),
        Optional.empty()
    );

    private GauntletFiringEvents() {
    }

    public static void register() {
        PlayerInteractEvent.RightClickItem.BUS.addListener((java.util.function.Predicate<PlayerInteractEvent.RightClickItem>) GauntletFiringEvents::onRightClickItem);
        PlayerInteractEvent.RightClickBlock.BUS.addListener((java.util.function.Predicate<PlayerInteractEvent.RightClickBlock>) GauntletFiringEvents::onRightClickBlock);
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener((java.util.function.Predicate<PlayerInteractEvent.EntityInteractSpecific>) GauntletFiringEvents::onEntityInteractSpecific);
        PlayerInteractEvent.LeftClickBlock.BUS.addListener((java.util.function.Predicate<PlayerInteractEvent.LeftClickBlock>) GauntletFiringEvents::onLeftClickBlock);
        AttackEntityEvent.BUS.addListener((java.util.function.Predicate<AttackEntityEvent>) GauntletFiringEvents::onAttackEntity);
        ItemTossEvent.BUS.addListener((java.util.function.Predicate<ItemTossEvent>) GauntletFiringEvents::onItemToss);
        LivingEvent.LivingTickEvent.BUS.addListener(GauntletFiringEvents::onLivingTick);
        LivingEntityUseItemEvent.Start.BUS.addListener(GauntletFiringEvents::onUseStart);
        LivingEntityUseItemEvent.Stop.BUS.addListener(GauntletFiringEvents::onUseStop);
    }

    private static boolean onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!shouldSuppressVanillaUse(player, event.getHand())) {
            return false;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        if (canStartCharging(player, event.getHand())) {
            markHeldItemForSpearAnimation(player, event.getHand());
            player.startUsingItem(event.getHand());
        }
        return true;
    }

    private static boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (tryPickupBlock(event)) {
            return true;
        }

        if (!shouldSuppressVanillaUse(player, event.getHand())) {
            return false;
        }

        event.setUseBlock(Result.DENY);
        event.setUseItem(Result.DENY);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (canStartCharging(player, event.getHand())) {
            markHeldItemForSpearAnimation(player, event.getHand());
            player.startUsingItem(event.getHand());
        }
        return true;
    }

    private static boolean onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        if (!shouldSuppressVanillaUse(player, event.getHand())) {
            return false;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        if (canStartCharging(player, event.getHand())) {
            markHeldItemForSpearAnimation(player, event.getHand());
            player.startUsingItem(event.getHand());
        }
        return true;
    }

    private static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || !isChargeAmmo(player, event.getItem())) {
            return;
        }

        markHeldItemForSpearAnimation(player, player.getUsedItemHand());
        event.setDuration(MAX_DRAW_DURATION);
    }

    private static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        InteractionHand usedHand = player.getUsedItemHand();
        ItemStack heldStack = player.getItemInHand(usedHand);
        boolean carryingBlock = GauntletCarryData.hasCarriedBlock(player);
        ItemStack ammoStack = carryingBlock ? GauntletCarryData.getCarriedStack(player) : heldStack.copyWithCount(1);
        clearHeldItemAnimation(player, usedHand);
        if (!isChargeAmmo(player, heldStack)) {
            return;
        }

        ItemStack gauntletStack = GauntletSlotData.get(player);
        if (gauntletStack.isEmpty() || player.getCooldowns().isOnCooldown(gauntletStack)) {
            return;
        }

        int usedTicks = MAX_DRAW_DURATION - event.getDuration();
        float drawPower = getDrawPower(usedTicks);
        if (drawPower <= 0.0F) {
            return;
        }

        var projectileTraits = AmmoBehaviorRegistry.traitsForStack(ammoStack);
        double launchVelocityMultiplier = ProjectileGauntletItem.launchVelocityMultiplier(gauntletStack) * mightVelocityMultiplier(player, gauntletStack);

        if (!player.level().isClientSide()) {
            AnyProjectileEntity projectile = new AnyProjectileEntity(player.level(), player, ammoStack.copyWithCount(1));
            if (carryingBlock) {
                projectile.setRecoverableWhenStuck(false);
            }
            projectile.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot(),
                0.0F,
                (float) (ModConfig.baseVelocity * launchVelocityMultiplier * drawPower),
                (float) (ModConfig.inaccuracy * projectileTraits.inaccuracyMultiplier())
            );
            Vec3 initialMotion = projectile.getDeltaMovement();
            projectile.setDeltaMovement(
                initialMotion.x * projectileTraits.horizontalVelocityMultiplier(),
                initialMotion.y * projectileTraits.verticalVelocityMultiplier(),
                initialMotion.z * projectileTraits.horizontalVelocityMultiplier()
            );
            player.level().addFreshEntity(projectile);
            player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.PROJECTILE_LAUNCH.get(),
                SoundSource.PLAYERS,
                0.9F,
                (float) (0.85F + drawPower * 0.35F + (projectileTraits.horizontalVelocityMultiplier() - 1.0D) * 0.2D)
            );

            if (!player.getAbilities().instabuild && !carryingBlock) {
                consumeAmmoAfterShot(player, usedHand, heldStack);
            }

            if (carryingBlock) {
                GauntletCarryData.restoreOriginalHands(player);
                syncCarryState(player);
            }

            if (!ProjectileGauntletItem.isUnbreakable(gauntletStack)) {
                gauntletStack.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
            }
            GauntletSlotData.set(player, gauntletStack);
            ProjectileGauntletItem.syncSlot(player);
        }

        player.getCooldowns().addCooldown(gauntletStack, ModConfig.cooldownTicks);
    }

    private static boolean onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        return GauntletCarryData.hasCarriedBlock(event.getEntity());
    }

    private static boolean onAttackEntity(AttackEntityEvent event) {
        return GauntletCarryData.hasCarriedBlock(event.getEntity());
    }

    private static boolean onItemToss(ItemTossEvent event) {
        return GauntletCarryData.hasCarriedBlock(event.getPlayer());
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player && GauntletCarryData.hasCarriedBlock(player)) {
            GauntletCarryData.applyCarriedHands(player);
        }
    }

    private static boolean canStartCharging(Player player, InteractionHand hand) {
        ItemStack gauntletStack = GauntletSlotData.get(player);
        if (gauntletStack.isEmpty() || player.getCooldowns().isOnCooldown(gauntletStack)) {
            return false;
        }

        if (GauntletCarryData.hasCarriedBlock(player)) {
            return isChargeAmmo(player, player.getItemInHand(hand));
        }

        ItemStack heldStack = player.getItemInHand(hand);
        if (!isChargeAmmo(player, heldStack)) {
            return false;
        }

        if (hand == InteractionHand.MAIN_HAND && ProjectileGauntletItem.isGauntlet(player.getMainHandItem())) {
            return false;
        }

        return true;
    }

    private static boolean shouldSuppressVanillaUse(Player player, InteractionHand hand) {
        if (!GauntletSlotData.isEquipped(player)) {
            return false;
        }

        ItemStack heldStack = player.getItemInHand(hand);
        if (GauntletCarryData.hasCarriedBlock(player)) {
            ItemStack carriedStack = GauntletCarryData.getCarriedStack(player);
            return !carriedStack.isEmpty() && ItemStack.isSameItemSameComponents(heldStack, carriedStack);
        }

        return AmmoItemSupport.isSupportedAmmo(heldStack) && !ProjectileGauntletItem.isGauntlet(heldStack);
    }

    private static boolean isChargeAmmo(Player player, ItemStack stack) {
        if (GauntletCarryData.hasCarriedBlock(player)) {
            ItemStack carriedStack = GauntletCarryData.getCarriedStack(player);
            return GauntletSlotData.isEquipped(player) && !carriedStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, carriedStack);
        }

        return GauntletSlotData.isEquipped(player) && AmmoItemSupport.isSupportedAmmo(stack) && !ProjectileGauntletItem.isGauntlet(stack);
    }

    private static boolean tryPickupBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()
            || event.getHand() != InteractionHand.MAIN_HAND
            || GauntletCarryData.hasCarriedBlock(player)
            || !GauntletSlotData.isEquipped(player)
            || player.getCooldowns().isOnCooldown(GauntletSlotData.get(player))) {
            return false;
        }

        event.setUseBlock(Result.DENY);
        event.setUseItem(Result.DENY);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.level().isClientSide()) {
            return true;
        }

        if (player instanceof ServerPlayer serverPlayer && GauntletCarryData.tryPickupBlock(serverPlayer, event.getPos())) {
            syncCarryState(serverPlayer);
            serverPlayer.containerMenu.broadcastChanges();
        }
        return true;
    }

    private static void consumeAmmoAfterShot(Player player, InteractionHand hand, ItemStack heldStack) {
        if (AmmoItemSupport.isFluidBucket(heldStack)) {
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            return;
        }

        heldStack.shrink(1);
    }

    private static float getDrawPower(int usedTicks) {
        if (usedTicks < MIN_DRAW_DURATION) {
            return 0.0F;
        }

        float drawPower = usedTicks / 20.0F;
        drawPower = (drawPower * drawPower + drawPower * 2.0F) / 3.0F;
        return Math.min(drawPower, 1.0F);
    }

    private static double mightVelocityMultiplier(Player player, ItemStack gauntletStack) {
        int level = ModEnchantments.getHolder(player.level(), ModEnchantments.MIGHT)
            .map(holder -> gauntletStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(holder))
            .orElse(0);
        return 1.0D + level * MIGHT_ENCHANTMENT_VELOCITY_PER_LEVEL;
    }

    private static void markHeldItemForSpearAnimation(Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (isChargeAmmo(player, heldStack)) {
            heldStack.set(DataComponents.KINETIC_WEAPON, TRIDENT_STYLE_ANIMATION);
        }
    }

    private static void clearHeldItemAnimation(Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!heldStack.isEmpty()) {
            heldStack.remove(DataComponents.KINETIC_WEAPON);
        }
    }

    private static void syncCarryState(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.syncGauntletCarry(
                serverPlayer,
                GauntletCarryData.hasCarriedBlock(serverPlayer),
                GauntletCarryData.getCarriedStack(serverPlayer),
                GauntletCarryData.getLockedSlot(serverPlayer)
            );
        }
    }

}
