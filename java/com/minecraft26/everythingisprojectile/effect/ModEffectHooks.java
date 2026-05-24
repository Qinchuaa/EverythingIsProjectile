package com.minecraft26.everythingisprojectile.effect;

import com.minecraft26.everythingisprojectile.registry.ModEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;

public final class ModEffectHooks {
    private static final String NAUSEA_FOOD_LEVEL_TAG = "everythingisprojectile.nausea_food_level";
    private static final String NAUSEA_SATURATION_TAG = "everythingisprojectile.nausea_saturation";

    private ModEffectHooks() {
    }

    public static void register() {
        LivingEvent.LivingTickEvent.BUS.addListener(ModEffectHooks::onLivingTick);
        LivingHealEvent.BUS.addListener(ModEffectHooks::onLivingHeal);
        LivingEntityUseItemEvent.Start.BUS.addListener(ModEffectHooks::onUseStart);
        LivingEntityUseItemEvent.Stop.BUS.addListener(ModEffectHooks::onUseStop);
        LivingEntityUseItemEvent.Finish.BUS.addListener(ModEffectHooks::onUseFinish);
        MobEffectEvent.Remove.BUS.addListener(ModEffectHooks::onEffectRemoved);
        MobEffectEvent.Expired.BUS.addListener(ModEffectHooks::onEffectExpired);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide() || !(living instanceof Player player) || !player.isInWaterOrRain()) {
            return;
        }

        if (player.getEffect(onFireHolder()) != null) {
            player.clearFire();
            player.removeEffect(onFireHolder());
        }
    }

    private static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || !BleedingMobEffect.isNaturalRegenBlocked(player)) {
            return;
        }

        if (!player.hasEffect(MobEffects.REGENERATION)) {
            event.setAmount(0.0F);
        }
    }

    private static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)
            || player.level().isClientSide()
            || player.getEffect(customNauseaHolder()) == null
            || event.getItem().get(DataComponents.FOOD) == null) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        data.putInt(NAUSEA_FOOD_LEVEL_TAG, player.getFoodData().getFoodLevel());
        data.putFloat(NAUSEA_SATURATION_TAG, player.getFoodData().getSaturationLevel());
    }

    private static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            clearNauseaFoodSnapshot(player);
        }
    }

    private static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        if (event.getItem().get(DataComponents.FOOD) != null && player.getEffect(customNauseaHolder()) != null) {
            halveFoodRestore(player);
            extendNausea(player, 100);
        }

        clearNauseaFoodSnapshot(player);
    }

    private static void halveFoodRestore(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NAUSEA_FOOD_LEVEL_TAG) || !data.contains(NAUSEA_SATURATION_TAG)) {
            return;
        }

        int beforeFood = data.getIntOr(NAUSEA_FOOD_LEVEL_TAG, player.getFoodData().getFoodLevel());
        float beforeSaturation = data.getFloatOr(NAUSEA_SATURATION_TAG, player.getFoodData().getSaturationLevel());
        FoodData foodData = player.getFoodData();
        int actualFoodGain = Math.max(0, foodData.getFoodLevel() - beforeFood);
        float actualSaturationGain = Math.max(0.0F, foodData.getSaturationLevel() - beforeSaturation);

        foodData.setFoodLevel(beforeFood + actualFoodGain / 2);
        foodData.setSaturation(beforeSaturation + actualSaturationGain / 2.0F);
    }

    private static void extendNausea(Player player, int extraTicks) {
        MobEffectInstance nausea = player.getEffect(customNauseaHolder());
        if (nausea == null) {
            return;
        }

        player.addEffect(
            new MobEffectInstance(
                customNauseaHolder(),
                nausea.getDuration() + extraTicks,
                nausea.getAmplifier(),
                nausea.isAmbient(),
                nausea.isVisible(),
                nausea.showIcon()
            )
        );
    }

    private static void clearNauseaFoodSnapshot(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(NAUSEA_FOOD_LEVEL_TAG);
        data.remove(NAUSEA_SATURATION_TAG);
    }

    private static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) {
            return;
        }

        clearFreezingState(event.getEntity(), event.getEffectInstance().getEffect());
    }

    private static void onEffectExpired(MobEffectEvent.Expired event) {
        clearFreezingState(event.getEntity(), event.getEffectInstance().getEffect());
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> customNauseaHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.NAUSEA.get());
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> freezingHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.FREEZING.get());
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> onFireHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.ON_FIRE.get());
    }

    private static void clearFreezingState(LivingEntity living, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (effect.equals(freezingHolder())) {
            living.setTicksFrozen(0);
        }
    }
}
