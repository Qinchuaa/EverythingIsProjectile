package com.minecraft26.everythingisprojectile.config;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = EverythingIsProjectileMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue COOLDOWN_TICKS = BUILDER
        .comment("Cooldown after firing the gauntlet.")
        .defineInRange("cooldownTicks", 12, 0, 1200);

    private static final ForgeConfigSpec.DoubleValue BASE_VELOCITY = BUILDER
        .comment("Initial projectile velocity.")
        .defineInRange("baseVelocity", 1.8D, 0.1D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue INACCURACY = BUILDER
        .comment("Projectile spread.")
        .defineInRange("inaccuracy", 0.65D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue GRAVITY_SCALE = BUILDER
        .comment("Gravity scale applied each tick.")
        .defineInRange("gravityScale", 1.0D, 0.0D, 5.0D);

    private static final ForgeConfigSpec.IntValue MAX_PROJECTILES_PER_PLAYER = BUILDER
        .comment("Soft cap for simultaneous projectiles owned by one player.")
        .defineInRange("maxProjectilesPerPlayer", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue PROJECTILE_LIFE_TICKS = BUILDER
        .comment("Maximum lifetime for any projectile.")
        .defineInRange("projectileLifeTicks", 1200, 1, 1200);

    private static final ForgeConfigSpec.EnumValue<DropMode> DEFAULT_DROP_MODE = BUILDER
        .comment("How default ammo behaves after impact.")
        .defineEnum("defaultDropMode", DropMode.DROP);

    private static final ForgeConfigSpec.DoubleValue BASE_DAMAGE = BUILDER
        .comment("Base physical damage before speed scaling.")
        .defineInRange("baseDamage", 2.0D, 0.0D, 100.0D);

    private static final ForgeConfigSpec.DoubleValue VELOCITY_DAMAGE_MULTIPLIER = BUILDER
        .comment("Extra damage contributed by projectile speed.")
        .defineInRange("velocityDamageMultiplier", 4.0D, 0.0D, 100.0D);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int cooldownTicks;
    public static double baseVelocity;
    public static double inaccuracy;
    public static double gravityScale;
    public static int maxProjectilesPerPlayer;
    public static int projectileLifeTicks;
    public static DropMode defaultDropMode;
    public static double baseDamage;
    public static double velocityDamageMultiplier;

    private ModConfig() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        cooldownTicks = COOLDOWN_TICKS.get();
        baseVelocity = BASE_VELOCITY.get();
        inaccuracy = INACCURACY.get();
        gravityScale = GRAVITY_SCALE.get();
        maxProjectilesPerPlayer = MAX_PROJECTILES_PER_PLAYER.get();
        projectileLifeTicks = Math.max(PROJECTILE_LIFE_TICKS.get(), 20 * 60);
        defaultDropMode = DEFAULT_DROP_MODE.get();
        baseDamage = BASE_DAMAGE.get();
        velocityDamageMultiplier = VELOCITY_DAMAGE_MULTIPLIER.get();
    }

    public enum DropMode {
        DROP,
        BREAK,
        CHANCE
    }
}
