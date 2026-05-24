package com.minecraft26.everythingisprojectile.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;

public final class HeadhurtMobEffect extends MobEffect {
    public HeadhurtMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x6F7F91);
        addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            Identifier.fromNamespaceAndPath("everythingisprojectile", "effect.headhurt_slowness"),
            -0.30D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 10 == 0;
    }
}
