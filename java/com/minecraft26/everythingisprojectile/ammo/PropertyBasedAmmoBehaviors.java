package com.minecraft26.everythingisprojectile.ammo;

import com.minecraft26.everythingisprojectile.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

abstract class AbstractBlockAmmoBehavior implements AmmoBehavior {
    private static final double VELOCITY_DAMAGE_CAP = 0.35D;
    private static final double VELOCITY_DAMAGE_SCALE = 0.45D;

    @Override
    public void onHit(HitContext context) {
        BlockState state = BlockAmmoSupport.resolveBlockState(context.ammoStack());
        ProjectileTraits traits = this.traits(context.ammoStack(), state, null);
        hitEntity(context, traits);
        ImpactEffectResolver.applyDirectHitEffects(context, state);
        ImpactEffectResolver.applyLandingBlockEffects(context, state);
        applyAreaDamage(context, traits);
        playBlockImpactEffects(context, state, traits);
        afterHit(context, state, traits);
    }

    protected void afterHit(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
    }

    protected void hitEntity(HitContext context, ProjectileTraits traits) {
        Entity target = context.hitEntity();
        if (target == null) {
            return;
        }

        float damage = computeDirectDamage(context, traits);
        Entity owner = context.projectile().getOwner();
        target.hurt(context.level().damageSources().thrown(context.projectile(), owner == null ? context.projectile() : owner), damage);

        if (target instanceof LivingEntity livingEntity && owner instanceof LivingEntity ownerLiving) {
            livingEntity.knockback(
                0.45D * traits.knockbackMultiplier(),
                ownerLiving.getX() - livingEntity.getX(),
                ownerLiving.getZ() - livingEntity.getZ()
            );
        }
    }

    protected float computeDirectDamage(HitContext context, ProjectileTraits traits) {
        double speedBonus = Math.min(context.projectile().getDeltaMovement().length(), VELOCITY_DAMAGE_CAP)
            * ModConfig.velocityDamageMultiplier
            * VELOCITY_DAMAGE_SCALE;
        if (traits.fixedDamage() > 0.0D) {
            return (float) (traits.fixedDamage() + speedBonus);
        }

        return (float) (ModConfig.baseDamage * traits.damageMultiplier() + speedBonus);
    }

    protected void applyAreaDamage(HitContext context, ProjectileTraits traits) {
        if (traits.areaDamage() <= 0.0D || traits.areaRadius() <= 0.0D) {
            return;
        }

        Vec3 center = context.blockHit() != null
            ? context.blockHit().getLocation()
            : context.hitEntity() != null ? context.hitEntity().position() : context.projectile().position();
        Entity owner = context.projectile().getOwner();
        Entity directTarget = context.hitEntity();
        AABB area = new AABB(center, center).inflate(traits.areaRadius());

        for (LivingEntity living : context.level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (living == owner || living == directTarget) {
                continue;
            }

            living.hurt(
                context.level().damageSources().thrown(context.projectile(), owner == null ? context.projectile() : owner),
                (float) traits.areaDamage()
            );
        }
    }

    protected void playBlockImpactEffects(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
        if (state == null) {
            return;
        }

        BlockPos impactPos = context.blockHit() != null ? context.blockHit().getBlockPos() : context.projectile().blockPosition();
        context.level().playSound(
            null,
            impactPos,
            state.getSoundType().getBreakSound(),
            SoundSource.BLOCKS,
            (float) (1.35F * traits.impactVolumeMultiplier()),
            0.96F
        );
        context.level().levelEvent(2001, impactPos, Block.getId(state));

        if (context.level() instanceof ServerLevel serverLevel) {
            int particleCount = Math.max(6, (int) Math.round(18 * traits.particleMultiplier()));
            serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                context.projectile().getX(),
                context.projectile().getY() + 0.15D,
                context.projectile().getZ(),
                particleCount,
                0.24D * traits.particleMultiplier(),
                0.18D * traits.particleMultiplier(),
                0.24D * traits.particleMultiplier(),
                0.06D
            );
        }
    }
}

final class UltraHeavyBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(0.52D, 0.92D, 1.35D, 0.72D, 2.2D, 10.0D, 0.0D, 0.0D, 1.55D, 0.9D, 1.25D, true, true, 0.01D, false, 0.45D, 0);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && BlockAmmoSupport.isUltraHeavy(state);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }
}

final class HeavyBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(0.72D, 0.96D, 1.18D, 0.86D, 1.65D, 6.0D, 0.0D, 0.0D, 1.3D, 0.95D, 1.15D, true, true, 0.014D, false, 0.45D, 0);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && !BlockAmmoSupport.isUltraHeavy(state) && BlockAmmoSupport.isHeavy(state);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }
}

final class FragileBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(1.02D, 0.8D, 0.9D, 1.1D, 0.75D, 1.0D, 1.5D, 1.6D, 0.7D, 1.75D, 0.92D, false, false, 0.0D, false, 0.45D, 0);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        if (state == null) {
            return false;
        }

        float hardness = BlockAmmoSupport.destroySpeed(state);
        SoundType soundType = state.getSoundType();
        return (hardness <= 0.25F && (state.canBeReplaced() || !state.blocksMotion()))
            || soundType == SoundType.GLASS
            || soundType == SoundType.CANDLE
            || soundType == SoundType.POWDER_SNOW;
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }
}

final class ElasticBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(0.92D, 0.85D, 0.78D, 0.85D, 0.55D, 0.0D, 0.0D, 0.0D, 0.7D, 1.0D, 1.0D, false, false, 0.0D, true, 0.8D, 4);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        if (state == null) {
            return false;
        }

        return state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }
}

final class IceBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(0.94D, 0.88D, 1.0D, 0.95D, 1.4D, 5.0D, 2.0D, 1.8D, 1.0D, 1.25D, 1.05D, false, false, 0.0D, false, 0.45D, 0);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && BlockAmmoSupport.isIce(state);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }

    @Override
    protected void afterHit(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
            ParticleTypes.SNOWFLAKE,
            context.projectile().getX(),
            context.projectile().getY() + 0.15D,
            context.projectile().getZ(),
            14,
            0.16D,
            0.16D,
            0.16D,
            0.0D
        );
    }
}

final class SandLikeBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(0.95D, 0.78D, 1.05D, 1.18D, 0.85D, 2.0D, 2.0D, 1.4D, 0.8D, 1.5D, 1.0D, false, false, 0.0D, false, 0.45D, 0);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && BlockAmmoSupport.isSandLike(state);
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }

    @Override
    protected void afterHit(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
            ParticleTypes.POOF,
            context.projectile().getX(),
            context.projectile().getY() + 0.08D,
            context.projectile().getZ(),
            10,
            0.18D,
            0.08D,
            0.18D,
            0.0D
        );
    }
}

final class LuminousBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(0.92D, 0.9D, 0.92D, 0.92D, 1.12D, 0.0D, 0.0D, 0.0D, 1.0D, 1.2D, 1.05D, true, true, 0.02D, false, 0.45D, 0);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return state != null && !BlockAmmoSupport.isIce(state) && state.getLightEmission() > 0;
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }

    @Override
    protected void afterHit(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
            ParticleTypes.END_ROD,
            context.projectile().getX(),
            context.projectile().getY() + 0.15D,
            context.projectile().getZ(),
            8,
            0.12D,
            0.12D,
            0.12D,
            0.0D
        );
    }
}

final class OrganicBlockBehavior extends AbstractBlockAmmoBehavior {
    private static final ProjectileTraits TRAITS = new ProjectileTraits(0.96D, 0.45D, 0.82D, 1.65D, 0.7D, 1.0D, 0.0D, 0.0D, 0.75D, 1.35D, 0.95D, false, false, 0.0D, false, 0.45D, 0);

    @Override
    public boolean matches(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        if (state == null) {
            return false;
        }

        MapColor color = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        SoundType soundType = state.getSoundType();
        return state.isRandomlyTicking()
            || color == MapColor.PLANT
            || color == MapColor.GRASS
            || soundType == SoundType.GRASS
            || soundType == SoundType.CROP
            || soundType == SoundType.VINE
            || soundType == SoundType.ROOTS
            || soundType == SoundType.STEM
            || soundType == SoundType.MOSS
            || soundType == SoundType.MOSS_CARPET
            || soundType == SoundType.PINK_PETALS
            || soundType == SoundType.LEAF_LITTER;
    }

    @Override
    public ProjectileTraits traits(ItemStack stack, @Nullable BlockState state, @Nullable CompoundTag blockEntityTag) {
        return TRAITS;
    }

    @Override
    protected void afterHit(HitContext context, @Nullable BlockState state, ProjectileTraits traits) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
            ParticleTypes.COMPOSTER,
            context.projectile().getX(),
            context.projectile().getY() + 0.1D,
            context.projectile().getZ(),
            6,
            0.15D,
            0.08D,
            0.15D,
            0.0D
        );
    }
}

final class BlockAmmoSupport {
    private BlockAmmoSupport() {
    }

    static @Nullable BlockState resolveBlockState(ItemStack stack) {
        return AmmoItemSupport.resolveBlockState(stack);
    }

    static float destroySpeed(BlockState state) {
        return state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    static boolean isUltraHeavy(BlockState state) {
        float hardness = destroySpeed(state);
        float resistance = state.getBlock().getExplosionResistance();
        return state.is(Blocks.OBSIDIAN)
            || state.is(Blocks.CRYING_OBSIDIAN)
            || state.is(Blocks.ANCIENT_DEBRIS)
            || state.is(Blocks.NETHERITE_BLOCK)
            || hardness >= 30.0F
            || resistance >= 600.0F;
    }

    static boolean isHeavy(BlockState state) {
        float hardness = destroySpeed(state);
        float resistance = state.getBlock().getExplosionResistance();
        SoundType soundType = state.getSoundType();
        return state.requiresCorrectToolForDrops()
            || hardness >= 1.5F
            || resistance >= 8.0F
            || soundType == SoundType.METAL
            || soundType == SoundType.IRON
            || soundType == SoundType.ANVIL
            || soundType == SoundType.STONE
            || soundType == SoundType.DEEPSLATE
            || soundType == SoundType.COPPER
            || soundType == SoundType.NETHERITE_BLOCK;
    }

    static boolean isIce(BlockState state) {
        return state.is(Blocks.ICE)
            || state.is(Blocks.PACKED_ICE)
            || state.is(Blocks.BLUE_ICE)
            || state.is(Blocks.FROSTED_ICE);
    }

    static boolean isSandLike(BlockState state) {
        SoundType soundType = state.getSoundType();
        return state.is(Blocks.SAND)
            || state.is(Blocks.RED_SAND)
            || state.is(Blocks.GRAVEL)
            || state.is(Blocks.SOUL_SAND)
            || soundType == SoundType.SAND
            || soundType == SoundType.GRAVEL
            || soundType == SoundType.SOUL_SAND;
    }
}
