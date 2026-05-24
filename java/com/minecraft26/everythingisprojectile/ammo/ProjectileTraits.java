package com.minecraft26.everythingisprojectile.ammo;

public record ProjectileTraits(
    double horizontalVelocityMultiplier,
    double verticalVelocityMultiplier,
    double gravityMultiplier,
    double inaccuracyMultiplier,
    double damageMultiplier,
    double fixedDamage,
    double areaDamage,
    double areaRadius,
    double knockbackMultiplier,
    double particleMultiplier,
    double impactVolumeMultiplier,
    boolean sticksInSurface,
    boolean pickupableWhenStuck,
    double wallSlideSpeed,
    boolean bounceOnBlockImpact,
    double bounceDamping,
    int maxBounces
) {
    public static final ProjectileTraits DEFAULT = new ProjectileTraits(
        1.0D,
        1.0D,
        1.0D,
        1.0D,
        1.0D,
        0.0D,
        0.0D,
        0.0D,
        1.0D,
        1.0D,
        1.0D,
        true,
        true,
        0.03D,
        false,
        0.45D,
        0
    );
}
