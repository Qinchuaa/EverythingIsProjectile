package com.minecraft26.everythingisprojectile.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class OnFireMobEffect extends MobEffect {
    private static final int FIRE_REFRESH_INTERVAL = 30;

    public OnFireMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xF27A21);
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        mob.igniteForSeconds(1.0F);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        mob.igniteForSeconds(1.0F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % FIRE_REFRESH_INTERVAL == 0;
    }
}
