package com.minecraft26.everythingisprojectile.entity;

import com.minecraft26.everythingisprojectile.ammo.AmmoBehaviorRegistry;
import com.minecraft26.everythingisprojectile.ammo.HitContext;
import com.minecraft26.everythingisprojectile.ammo.ProjectileTraits;
import com.minecraft26.everythingisprojectile.config.ModConfig;
import com.minecraft26.everythingisprojectile.registry.ModEntities;
import com.minecraft26.everythingisprojectile.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class AnyProjectileEntity extends ThrowableItemProjectile {
    private static final String FIRED_TICK_TAG = "FiredGameTime";
    private static final String STUCK_TICK_TAG = "StuckGameTime";
    private static final String STUCK_TAG = "StuckInSurface";
    private static final String STUCK_FACE_TAG = "StuckFace";
    private static final String REMAINING_BOUNCES_TAG = "RemainingBounces";
    private static final String RECOVERABLE_WHEN_STUCK_TAG = "RecoverableWhenStuck";
    private static final double SURFACE_OFFSET = 0.06D;
    private static final EntityDataAccessor<Boolean> DATA_STUCK_IN_SURFACE = SynchedEntityData.defineId(
        AnyProjectileEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Byte> DATA_STUCK_FACE = SynchedEntityData.defineId(
        AnyProjectileEntity.class, EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Boolean> DATA_RECOVERABLE_WHEN_STUCK = SynchedEntityData.defineId(
        AnyProjectileEntity.class, EntityDataSerializers.BOOLEAN
    );
    private long firedGameTime;
    private long stuckGameTime;
    private int remainingBounces = -1;

    public AnyProjectileEntity(EntityType<? extends AnyProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public AnyProjectileEntity(Level level, LivingEntity owner, ItemStack ammoStack) {
        super(ModEntities.ANY_PROJECTILE.get(), owner, level, ammoStack);
        this.firedGameTime = level.getGameTime();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROJECTILE_GAUNTLET.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_STUCK_IN_SURFACE, false);
        entityData.define(DATA_STUCK_FACE, (byte) Direction.UP.ordinal());
        entityData.define(DATA_RECOVERABLE_WHEN_STUCK, true);
    }

    @Override
    public void tick() {
        if (this.firedGameTime == 0L) {
            this.firedGameTime = level().getGameTime();
        }

        if (!level().isClientSide()) {
            long age = isStuckInSurface() ? level().getGameTime() - this.stuckGameTime : level().getGameTime() - this.firedGameTime;
            if (age >= ModConfig.projectileLifeTicks) {
                discard();
                return;
            }

            if (getOwner() instanceof LivingEntity livingOwner && ModConfig.maxProjectilesPerPlayer > 0) {
                long ownedProjectiles = level().getEntities(this, getBoundingBox().inflate(128.0D),
                    entity -> entity instanceof AnyProjectileEntity projectile && projectile.getOwner() == livingOwner).size();
                if (ownedProjectiles > ModConfig.maxProjectilesPerPlayer) {
                    discard();
                    return;
                }
            }
        }

        if (isStuckInSurface()) {
            tickStuckInSurface();
            return;
        }

        super.tick();

        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.03D * ModConfig.gravityScale * getProjectileTraits().gravityMultiplier(), 0.0D));
        }

        updateRotationToMovement();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        handleEntityImpact(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        handleBlockImpact(result);
    }

    private void handleEntityImpact(HitResult hitResult) {
        if (level().isClientSide()) {
            return;
        }

        ItemStack ammoStack = getItem().copyWithCount(1);
        AmmoBehaviorRegistry.findForStack(ammoStack).onHit(new HitContext(level(), this, ammoStack, hitResult));
        discard();
    }

    private void handleBlockImpact(BlockHitResult hitResult) {
        if (level().isClientSide() || isStuckInSurface()) {
            return;
        }

        ItemStack ammoStack = getItem().copyWithCount(1);
        AmmoBehaviorRegistry.findForStack(ammoStack).onHit(new HitContext(level(), this, ammoStack, hitResult));
        ProjectileTraits traits = AmmoBehaviorRegistry.traitsForStack(ammoStack);
        if (traits.bounceOnBlockImpact()) {
            bounceFromSurface(hitResult, traits);
            return;
        }
        if (traits.sticksInSurface()) {
            stickInSurface(hitResult);
            return;
        }
        discard();
    }

    private void stickInSurface(BlockHitResult hitResult) {
        Direction face = hitResult.getDirection();
        Vec3 faceOffset = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(SURFACE_OFFSET);
        Vec3 stuckPosition = hitResult.getLocation().add(faceOffset);

        setStuckInSurface(true);
        setStuckFace(face);
        this.stuckGameTime = level().getGameTime();
        setDeltaMovement(Vec3.ZERO);
        setPos(stuckPosition.x, stuckPosition.y, stuckPosition.z);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    private void tickStuckInSurface() {
        this.baseTick();
        setDeltaMovement(Vec3.ZERO);
    }

    private void bounceFromSurface(BlockHitResult hitResult, ProjectileTraits traits) {
        if (getRemainingBounces() <= 0) {
            discard();
            return;
        }

        Vec3 motion = getDeltaMovement();
        Direction direction = hitResult.getDirection();
        Vec3 normal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        double projection = motion.dot(normal);
        Vec3 reflectedMotion = motion.subtract(normal.scale(2.0D * projection)).scale(traits.bounceDamping());

        if (reflectedMotion.lengthSqr() < 0.01D) {
            discard();
            return;
        }

        this.remainingBounces--;
        Vec3 bouncePosition = hitResult.getLocation().add(normal.scale(SURFACE_OFFSET));
        setPos(bouncePosition.x, bouncePosition.y, bouncePosition.z);
        setDeltaMovement(reflectedMotion);
        updateRotationToMovement();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong(FIRED_TICK_TAG, this.firedGameTime);
        output.putLong(STUCK_TICK_TAG, this.stuckGameTime);
        output.putBoolean(STUCK_TAG, isStuckInSurface());
        output.putInt(STUCK_FACE_TAG, getStuckFace().ordinal());
        output.putInt(REMAINING_BOUNCES_TAG, this.remainingBounces);
        output.putBoolean(RECOVERABLE_WHEN_STUCK_TAG, isRecoverableWhenStuck());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.firedGameTime = input.getLongOr(FIRED_TICK_TAG, 0L);
        this.stuckGameTime = input.getLongOr(STUCK_TICK_TAG, 0L);
        setStuckInSurface(input.getBooleanOr(STUCK_TAG, false));
        int faceOrdinal = input.getIntOr(STUCK_FACE_TAG, Direction.UP.ordinal());
        Direction[] directions = Direction.values();
        setStuckFace(directions[Mth.clamp(faceOrdinal, 0, directions.length - 1)]);
        this.remainingBounces = input.getIntOr(REMAINING_BOUNCES_TAG, -1);
        setRecoverableWhenStuck(input.getBooleanOr(RECOVERABLE_WHEN_STUCK_TAG, true));
    }

    private void updateRotationToMovement() {
        Vec3 movement = getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-7D) {
            return;
        }

        float horizontalSpeed = (float) Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        float targetYaw = (float) (Mth.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG);
        float targetPitch = (float) (Mth.atan2(movement.y, horizontalSpeed) * Mth.RAD_TO_DEG);

        if (this.yRotO == 0.0F && this.xRotO == 0.0F) {
            this.setYRot(targetYaw);
            this.setXRot(targetPitch);
        } else {
            this.setYRot(smoothRotation(this.getYRot(), targetYaw));
            this.setXRot(smoothRotation(this.getXRot(), targetPitch));
        }

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private static float smoothRotation(float current, float target) {
        while (target - current < -180.0F) {
            current -= 360.0F;
        }

        while (target - current >= 180.0F) {
            current += 360.0F;
        }

        return Mth.lerp(0.2F, current, target);
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide() || !isStuckInSurface() || !getProjectileTraits().pickupableWhenStuck() || !isRecoverableWhenStuck()) {
            return;
        }

        if (player.getInventory().add(getItem().copyWithCount(1))) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return isStuckInSurface() || super.isPickable();
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return isStuckInSurface() || super.canBeCollidedWith(other);
    }

    public boolean isStuckInSurface() {
        return this.getEntityData().get(DATA_STUCK_IN_SURFACE);
    }

    public Direction getStuckFace() {
        Direction[] directions = Direction.values();
        byte ordinal = this.getEntityData().get(DATA_STUCK_FACE);
        return directions[Mth.clamp(ordinal, (byte) 0, (byte) (directions.length - 1))];
    }

    private void setStuckInSurface(boolean stuck) {
        this.getEntityData().set(DATA_STUCK_IN_SURFACE, stuck);
    }

    private void setStuckFace(Direction face) {
        this.getEntityData().set(DATA_STUCK_FACE, (byte) face.ordinal());
    }

    public ProjectileTraits getProjectileTraits() {
        return AmmoBehaviorRegistry.traitsForStack(getItem());
    }

    public void setRecoverableWhenStuck(boolean recoverableWhenStuck) {
        this.getEntityData().set(DATA_RECOVERABLE_WHEN_STUCK, recoverableWhenStuck);
    }

    public boolean isRecoverableWhenStuck() {
        return this.getEntityData().get(DATA_RECOVERABLE_WHEN_STUCK);
    }

    private int getRemainingBounces() {
        if (this.remainingBounces < 0) {
            this.remainingBounces = Math.max(0, getProjectileTraits().maxBounces());
        }
        return this.remainingBounces;
    }
}
