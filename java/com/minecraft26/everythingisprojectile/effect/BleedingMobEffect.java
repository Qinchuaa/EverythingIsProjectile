package com.minecraft26.everythingisprojectile.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BleedingMobEffect extends MobEffect {
    public static final String NO_REGEN_UNTIL_TAG = "everythingisprojectile.bleeding_no_regen_until";
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final int NO_REGEN_TICKS = 100;
    private static final float SAFE_HEALTH_THRESHOLD = 10.0F;

    public BleedingMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8C1B1B);
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        if (hasReachedSafeHealthThreshold(mob)) {
            clearNoRegenWindow(mob);
            return;
        }
        applyNoRegenWindow(mob);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        if (hasReachedSafeHealthThreshold(mob)) {
            clearNoRegenWindow(mob);
            return true;
        }

        mob.hurtServer(level, mob.damageSources().wither(), 1.0F);
        applyNoRegenWindow(mob);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % DAMAGE_INTERVAL_TICKS == 0;
    }

    public static boolean isNaturalRegenBlocked(Player player) {
        return player.getPersistentData().getIntOr(NO_REGEN_UNTIL_TAG, 0) > player.tickCount;
    }

    private static boolean hasReachedSafeHealthThreshold(LivingEntity mob) {
        return mob instanceof Player && mob.getHealth() <= SAFE_HEALTH_THRESHOLD;
    }

    private static void applyNoRegenWindow(LivingEntity mob) {
        if (mob instanceof Player player) {
            player.getPersistentData().putInt(NO_REGEN_UNTIL_TAG, player.tickCount + NO_REGEN_TICKS);
        }
    }

    private static void clearNoRegenWindow(LivingEntity mob) {
        if (mob instanceof Player player) {
            player.getPersistentData().remove(NO_REGEN_UNTIL_TAG);
        }
    }
}
