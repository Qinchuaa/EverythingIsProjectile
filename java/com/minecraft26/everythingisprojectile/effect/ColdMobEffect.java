package com.minecraft26.everythingisprojectile.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;

public final class ColdMobEffect extends MobEffect {
    private static final int FREEZE_DAMAGE_INTERVAL = 40;

    public ColdMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8FD6E8);
        addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            Identifier.fromNamespaceAndPath("everythingisprojectile", "effect.freezing_slowness"),
            -0.45D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        applyFreezingState(mob);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        if (!mob.canFreeze()) {
            return true;
        }

        applyFreezingState(mob);
        if (mob.isFullyFrozen() && level.getGameTime() % FREEZE_DAMAGE_INTERVAL == 0L) {
            mob.hurtServer(level, mob.damageSources().freeze(), 1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    private static void applyFreezingState(LivingEntity mob) {
        if (!mob.canFreeze()) {
            return;
        }

        mob.setIsInPowderSnow(true);
        mob.setTicksFrozen(mob.getTicksRequiredToFreeze());
    }
}
