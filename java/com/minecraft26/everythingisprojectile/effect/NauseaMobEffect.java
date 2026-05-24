package com.minecraft26.everythingisprojectile.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class NauseaMobEffect extends MobEffect {
    public NauseaMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x4D8054);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        if (mob instanceof Player player) {
            player.causeFoodExhaustion(0.01F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }
}
