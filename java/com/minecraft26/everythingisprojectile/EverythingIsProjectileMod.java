package com.minecraft26.everythingisprojectile;

import com.mojang.logging.LogUtils;
import com.minecraft26.everythingisprojectile.client.GauntletCarryClientEvents;
import com.minecraft26.everythingisprojectile.client.GauntletFirstPersonAnimationEvents;
import com.minecraft26.everythingisprojectile.client.GauntletSlotClientEvents;
import com.minecraft26.everythingisprojectile.config.ModConfig;
import com.minecraft26.everythingisprojectile.effect.ModEffectClientEvents;
import com.minecraft26.everythingisprojectile.effect.ModEffectHooks;
import com.minecraft26.everythingisprojectile.gauntlet.GauntletFiringEvents;
import com.minecraft26.everythingisprojectile.gauntlet.GauntletSlotEvents;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import com.minecraft26.everythingisprojectile.registry.ModCreativeTabs;
import com.minecraft26.everythingisprojectile.registry.ModEntities;
import com.minecraft26.everythingisprojectile.registry.ModEffects;
import com.minecraft26.everythingisprojectile.registry.ModItems;
import com.minecraft26.everythingisprojectile.registry.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(EverythingIsProjectileMod.MODID)
public final class EverythingIsProjectileMod {
    public static final String MODID = "everythingisprojectile";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EverythingIsProjectileMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        ModItems.ITEMS.register(modBusGroup);
        ModSounds.SOUND_EVENTS.register(modBusGroup);
        ModEntities.ENTITY_TYPES.register(modBusGroup);
        ModEffects.MOB_EFFECTS.register(modBusGroup);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBusGroup);
        NetworkHandler.register();
        GauntletSlotEvents.register();
        GauntletFiringEvents.register();
        ModEffectHooks.register();

        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.register();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Initializing {}", MODID);
    }

    public static final class ClientEvents {
        private ClientEvents() {
        }

        public static void register() {
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers.BUS.addListener(ClientEvents::registerEntityRenderers);
            GauntletCarryClientEvents.register();
            GauntletFirstPersonAnimationEvents.register();
            GauntletSlotClientEvents.register();
            ModEffectClientEvents.register();
        }

        private static void registerEntityRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.ANY_PROJECTILE.get(), com.minecraft26.everythingisprojectile.entity.AnyProjectileEntityRenderer::new);
        }
    }
}
