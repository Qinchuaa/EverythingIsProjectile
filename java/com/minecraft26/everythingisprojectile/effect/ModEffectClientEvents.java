package com.minecraft26.everythingisprojectile.effect;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import com.minecraft26.everythingisprojectile.registry.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
public final class ModEffectClientEvents {
    private static final float HEADHURT_NAUSEA_SCALE = 0.55F;
    private static final float NAUSEA_OVERLAY_ALPHA_SCALE = 0.65F;
    private static final float HEADHURT_BLINDNESS_ALPHA = 0.82F;
    private static final float HEADHURT_MIN_VIEW_DISTANCE = 5.0F;
    private static final Identifier CUSTOM_EFFECT_OVERLAY = Identifier.fromNamespaceAndPath(EverythingIsProjectileMod.MODID, "custom_effect_overlay");

    private ModEffectClientEvents() {
    }

    public static void register() {
        ComputeFovModifierEvent.BUS.addListener(ModEffectClientEvents::onComputeFov);
        AddGuiOverlayLayersEvent.BUS.addListener(ModEffectClientEvents::onAddGuiOverlayLayers);
        ViewportEvent.RenderFog.BUS.addListener(ModEffectClientEvents::onRenderFog);
        ViewportEvent.ComputeFogColor.BUS.addListener(ModEffectClientEvents::onComputeFogColor);
    }

    private static void onComputeFov(ComputeFovModifierEvent event) {
        if (event.getPlayer().isUsingItem()) {
            return;
        }

        if (event.getPlayer().getEffect(headhurtHolder()) == null && event.getPlayer().getEffect(freezingHolder()) == null) {
            return;
        }

        if (event.getNewFovModifier() < 1.0F) {
            event.setNewFovModifier(1.0F);
        }
    }

    private static void onAddGuiOverlayLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(CUSTOM_EFFECT_OVERLAY, ModEffectClientEvents::extractEffectOverlay);
    }

    private static void extractEffectOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        float nauseaStrength = getCustomNauseaStrength(minecraft, partialTick);
        if (nauseaStrength > 0.0F) {
            renderNauseaOverlay(minecraft.gui, graphics, nauseaStrength);
        }

        float blindnessStrength = getHeadhurtBlend(minecraft, partialTick);
        if (blindnessStrength > 0.0F) {
            renderHeadhurtBlindness(graphics, blindnessStrength);
        }
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        float partialTick = event.getPartialTick();
        float blindnessStrength = getHeadhurtBlend(minecraft, partialTick);
        if (blindnessStrength <= 0.0F) {
            return;
        }

        float renderDistance = event.getFarPlaneDistance();
        float targetDistance = Mth.lerp(blindnessStrength, renderDistance, HEADHURT_MIN_VIEW_DISTANCE);
        event.setNearPlaneDistance(targetDistance * 0.25F);
        event.setFarPlaneDistance(targetDistance);
    }

    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        float blindnessStrength = getHeadhurtBlend(minecraft, event.getPartialTick());
        if (blindnessStrength <= 0.0F) {
            return;
        }

        float brightness = Mth.square(1.0F - blindnessStrength);
        event.setRed(event.getRed() * brightness);
        event.setGreen(event.getGreen() * brightness);
        event.setBlue(event.getBlue() * brightness);
    }

    private static Holder<MobEffect> headhurtHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.HEADHURT.get());
    }

    private static Holder<MobEffect> nauseaHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.NAUSEA.get());
    }

    private static Holder<MobEffect> freezingHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.FREEZING.get());
    }

    private static float getCustomNauseaStrength(Minecraft minecraft, float partialTick) {
        float headhurt = getHeadhurtBlend(minecraft, partialTick) * HEADHURT_NAUSEA_SCALE;
        float nausea = minecraft.player.getEffectBlendFactor(nauseaHolder(), partialTick);
        return Math.max(headhurt, nausea);
    }

    private static float getHeadhurtBlend(Minecraft minecraft, float partialTick) {
        return minecraft.player.getEffectBlendFactor(headhurtHolder(), partialTick);
    }

    private static void renderNauseaOverlay(Gui gui, GuiGraphicsExtractor graphics, float strength) {
        float screenEffectScale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        if (screenEffectScale >= 1.0F) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        float overlayStrength = strength * (1.0F - screenEffectScale);
        float size = Mth.lerp(overlayStrength, 2.0F, 1.0F);

        graphics.pose().pushMatrix();
        graphics.pose().translate(screenWidth / 2.0F, screenHeight / 2.0F);
        graphics.pose().scale(size, size);
        graphics.pose().translate(-screenWidth / 2.0F, -screenHeight / 2.0F);
        graphics.blit(
            RenderPipelines.GUI_NAUSEA_OVERLAY,
            Gui.NAUSEA_LOCATION,
            0,
            0,
            0.0F,
            0.0F,
            screenWidth,
            screenHeight,
            screenWidth,
            screenHeight,
            ARGB.colorFromFloat(
                Mth.clamp(overlayStrength * NAUSEA_OVERLAY_ALPHA_SCALE, 0.0F, 1.0F),
                0.2F * overlayStrength,
                0.4F * overlayStrength,
                0.2F * overlayStrength
            )
        );
        graphics.pose().popMatrix();
    }

    private static void renderHeadhurtBlindness(GuiGraphicsExtractor graphics, float strength) {
        int alpha = Mth.floor(Mth.clamp(strength * HEADHURT_BLINDNESS_ALPHA, 0.0F, 1.0F) * 255.0F);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.color(alpha, 0, 0, 0));
    }
}
