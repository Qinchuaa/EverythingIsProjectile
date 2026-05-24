package com.minecraft26.everythingisprojectile.registry;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import com.minecraft26.everythingisprojectile.item.ProjectileGauntletItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EverythingIsProjectileMod.MODID);

    public static final RegistryObject<Item> PROJECTILE_GAUNTLET = ITEMS.register("projectile_gauntlet",
        () -> new ProjectileGauntletItem(ProjectileGauntletItem.Tier.LEATHER, new Item.Properties()
            .stacksTo(1)
            .enchantable(15)
            .durability(150)
            .setId(ITEMS.key("projectile_gauntlet"))));

    public static final RegistryObject<Item> IRON_PROJECTILE_GAUNTLET = ITEMS.register("iron_projectile_gauntlet",
        () -> new ProjectileGauntletItem(ProjectileGauntletItem.Tier.IRON, new Item.Properties()
            .stacksTo(1)
            .enchantable(18)
            .durability(500)
            .setId(ITEMS.key("iron_projectile_gauntlet"))));

    public static final RegistryObject<Item> DIAMOND_PROJECTILE_GAUNTLET = ITEMS.register("diamond_projectile_gauntlet",
        () -> new ProjectileGauntletItem(ProjectileGauntletItem.Tier.DIAMOND, new Item.Properties()
            .stacksTo(1)
            .enchantable(22)
            .durability(1500)
            .setId(ITEMS.key("diamond_projectile_gauntlet"))));

    public static final RegistryObject<Item> CREATIVE_PROJECTILE_GAUNTLET = ITEMS.register("creative_projectile_gauntlet",
        () -> new ProjectileGauntletItem(ProjectileGauntletItem.Tier.CREATIVE, new Item.Properties()
            .stacksTo(1)
            .enchantable(30)
            .setId(ITEMS.key("creative_projectile_gauntlet"))));

    private ModItems() {
    }

    public static ItemStack starterGauntlet() {
        return new ItemStack(PROJECTILE_GAUNTLET.get());
    }
}
