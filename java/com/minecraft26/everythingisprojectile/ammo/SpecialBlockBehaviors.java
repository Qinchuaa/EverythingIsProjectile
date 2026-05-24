package com.minecraft26.everythingisprojectile.ammo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class BedBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 36;

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && state.getBlock() instanceof BedBlock;
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return ProjectileTraits.DEFAULT;
    }

    @Override
    protected void afterHit(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
        if (!(context.hitEntity() instanceof ServerPlayer player) || context.level().isClientSide()) {
            return;
        }

        shuffleInventory(player, context.level());
    }

    private static void shuffleInventory(ServerPlayer player, Level level) {
        Inventory inventory = player.getInventory();
        int slotCount = Math.min(inventory.getContainerSize(), PLAYER_INVENTORY_SLOT_COUNT);
        List<ItemStack> shuffledItems = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = inventory.getItem(i);
            shuffledItems.add(stack.copy());
        }

        Collections.shuffle(shuffledItems, new Random(level.getGameTime()));

        for (int i = 0; i < slotCount; i++) {
            inventory.setItem(i, shuffledItems.get(i));
        }

        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }
}

final class TntBlockBehavior implements AmmoBehavior {
    private static final ProjectileTraits TNT_TRAITS = new ProjectileTraits(
        0.9D, 0.9D, 1.0D, 0.95D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, false, false, 0.0D, false, 0.45D, 0
    );
    private static final float SMALL_EXPLOSION_POWER = 1.5F;

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && state.is(Blocks.TNT);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TNT_TRAITS;
    }

    @Override
    public void onHit(HitContext context) {
        if (context.level().isClientSide()) {
            return;
        }

        double x = context.hitEntity() != null ? context.hitEntity().getX() : context.projectile().getX();
        double y = context.hitEntity() != null ? context.hitEntity().getY(0.5D) : context.projectile().getY();
        double z = context.hitEntity() != null ? context.hitEntity().getZ() : context.projectile().getZ();
        if (context.blockHit() != null) {
            x = context.blockHit().getLocation().x;
            y = context.blockHit().getLocation().y;
            z = context.blockHit().getLocation().z;
        }

        context.level().explode(context.projectile(), x, y, z, SMALL_EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
    }
}
