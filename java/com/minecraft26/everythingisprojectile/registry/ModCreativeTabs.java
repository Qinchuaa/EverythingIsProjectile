package com.minecraft26.everythingisprojectile.registry;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EverythingIsProjectileMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
        .title(net.minecraft.network.chat.Component.translatable("itemGroup.everythingisprojectile.main"))
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .icon(() -> ModItems.PROJECTILE_GAUNTLET.get().getDefaultInstance())
        .displayItems((parameters, output) -> {
            output.accept(ModItems.PROJECTILE_GAUNTLET.get());
            output.accept(ModItems.IRON_PROJECTILE_GAUNTLET.get());
            output.accept(ModItems.DIAMOND_PROJECTILE_GAUNTLET.get());
            output.accept(ModItems.CREATIVE_PROJECTILE_GAUNTLET.get());
        })
        .build());

    private ModCreativeTabs() {
    }
}
