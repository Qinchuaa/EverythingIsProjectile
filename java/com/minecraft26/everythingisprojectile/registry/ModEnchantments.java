package com.minecraft26.everythingisprojectile.registry;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> MIGHT = ResourceKey.create(
        Registries.ENCHANTMENT,
        Identifier.fromNamespaceAndPath(EverythingIsProjectileMod.MODID, "might")
    );

    private ModEnchantments() {
    }

    public static Optional<Holder.Reference<Enchantment>> getHolder(Level level, ResourceKey<Enchantment> enchantmentKey) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantmentKey);
    }
}
