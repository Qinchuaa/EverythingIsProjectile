package com.minecraft26.everythingisprojectile.registry;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import com.minecraft26.everythingisprojectile.entity.AnyProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EverythingIsProjectileMod.MODID);

    public static final RegistryObject<EntityType<AnyProjectileEntity>> ANY_PROJECTILE = ENTITY_TYPES.register("any_projectile",
        () -> EntityType.Builder.<AnyProjectileEntity>of(AnyProjectileEntity::new, MobCategory.MISC)
            .sized(0.8F, 0.8F)
            .clientTrackingRange(6)
            .updateInterval(1)
            .build(ENTITY_TYPES.key("any_projectile")));

    private ModEntities() {
    }
}
