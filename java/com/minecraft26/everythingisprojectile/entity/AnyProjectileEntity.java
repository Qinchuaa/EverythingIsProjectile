package com.minecraft26.everythingisprojectile.entity;

import com.minecraft26.everythingisprojectile.ammo.AmmoBehaviorRegistry;
import com.minecraft26.everythingisprojectile.ammo.HitContext;
import com.minecraft26.everythingisprojectile.ammo.ProjectileTraits;
import com.minecraft26.everythingisprojectile.config.ModConfig;
import com.minecraft26.everythingisprojectile.registry.ModEntities;
import com.minecraft26.everythingisprojectile.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final String LAVA_TICKS_TAG = "LavaTicks";
    private static final String SPAWNED_MINI_DRAGON_TAG = "SpawnedMiniDragon";
    private static final double SURFACE_OFFSET = 0.06D;
    private static final int DEFAULT_LAVA_SURVIVAL_TICKS = 40;
    private static final int MINI_DRAGON_LIFETIME = 200;
    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> DRAGONS_TO_REMOVE = new HashMap<>();
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
    private int lavaTicks;
    private boolean hasSpawnedMiniDragon;

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
            removeExpiredMiniDragons();

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

        if (isDragonEggItem()) {
            return;
        }

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
            trySpawnMiniDragon(hitResult);
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

    // 卡在表面时的每tick更新，处理附着方块检测、岩浆烧毁和龙蛋无敌
    private void tickStuckInSurface() {
        this.baseTick();
        setDeltaMovement(Vec3.ZERO);

        if (level().isClientSide()) {
            return;
        }

        BlockPos attachedBlock = this.blockPosition().relative(getStuckFace().getOpposite());
        if (level().getBlockState(attachedBlock).isAir()) {
            setStuckInSurface(false);
            return;
        }

        if (isDragonEggItem()) {
            return;
        }

        if (isInLava()) {
            lavaTicks++;
            if (lavaTicks >= resolveLavaSurvivalTicks()) {
                discard();
            }
        } else {
            lavaTicks = 0;
        }
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
        output.putInt(LAVA_TICKS_TAG, this.lavaTicks);
        output.putBoolean(SPAWNED_MINI_DRAGON_TAG, this.hasSpawnedMiniDragon);
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
        this.lavaTicks = input.getIntOr(LAVA_TICKS_TAG, 0);
        this.hasSpawnedMiniDragon = input.getBooleanOr(SPAWNED_MINI_DRAGON_TAG, false);
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

    @Override
    public boolean isAttackable() {
        return isStuckInSurface();
    }

    // 使用镐子破坏卡在表面的投射物
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isStuckInSurface()) {
            return false;
        }

        if (isDragonEggItem()) {
            return false;
        }

        if (source.getEntity() instanceof Player player) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.is(ItemTags.PICKAXES)) {
                spawnAtLocation(level, getItem().copyWithCount(1));
                discard();
                return true;
            }
        }

        return false;
    }

    // 判断当前投射物是否为龙蛋
    private boolean isDragonEggItem() {
        return getBlockFromItem() == Blocks.DRAGON_EGG;
    }

    // 根据方块硬度返回在岩浆中存活的tick数
    private int resolveLavaSurvivalTicks() {
        Block block = getBlockFromItem();
        if (block == null) {
            return DEFAULT_LAVA_SURVIVAL_TICKS;
        }

        BlockState state = block.defaultBlockState();

        if (state.is(Blocks.NETHERITE_BLOCK)) {
            return 400;
        }
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            return 360;
        }
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.ENDER_CHEST)) {
            return 320;
        }
        if (isAnvilSeries(state) || state.getSoundType() == SoundType.NETHERITE_BLOCK) {
            return 280;
        }
        if (isIronHeavySeries(state)) {
            return 240;
        }
        if (isCopperSeries(state)) {
            return 200;
        }
        if (isBlackstoneSeries(state)) {
            return 160;
        }
        if (state.getSoundType() == SoundType.DEEPSLATE) {
            return 140;
        }
        if (isStoneBrickSeries(state)) {
            return 120;
        }
        if (isStoneSeries(state)) {
            return 100;
        }
        if (state.getSoundType() == SoundType.STONE) {
            return 80;
        }
        return DEFAULT_LAVA_SURVIVAL_TICKS;
    }

    // 从物品堆中获取方块
    private Block getBlockFromItem() {
        Item item = getItem().getItem();
        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock();
        }
        return null;
    }

    private static boolean isAnvilSeries(BlockState state) {
        return state.is(Blocks.ANVIL)
            || state.is(Blocks.CHIPPED_ANVIL)
            || state.is(Blocks.DAMAGED_ANVIL);
    }

    private static boolean isIronHeavySeries(BlockState state) {
        return state.is(Blocks.IRON_BLOCK)
            || state.is(Blocks.RAW_IRON_BLOCK)
            || state.is(Blocks.GOLD_BLOCK)
            || state.is(Blocks.RAW_GOLD_BLOCK)
            || state.is(Blocks.DIAMOND_BLOCK)
            || state.is(Blocks.EMERALD_BLOCK)
            || state.is(Blocks.LAPIS_BLOCK);
    }

    private static boolean isCopperSeries(BlockState state) {
        return state.is(Blocks.COPPER_BLOCK)
            || state.is(Blocks.EXPOSED_COPPER)
            || state.is(Blocks.WEATHERED_COPPER)
            || state.is(Blocks.OXIDIZED_COPPER)
            || state.is(Blocks.CUT_COPPER)
            || state.is(Blocks.EXPOSED_CUT_COPPER)
            || state.is(Blocks.WEATHERED_CUT_COPPER)
            || state.is(Blocks.OXIDIZED_CUT_COPPER);
    }

    private static boolean isBlackstoneSeries(BlockState state) {
        return state.is(Blocks.BLACKSTONE)
            || state.is(Blocks.POLISHED_BLACKSTONE)
            || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
            || state.is(Blocks.BASALT)
            || state.is(Blocks.POLISHED_BASALT)
            || state.is(Blocks.SMOOTH_BASALT);
    }

    private static boolean isStoneBrickSeries(BlockState state) {
        return state.is(Blocks.STONE_BRICKS)
            || state.is(Blocks.MOSSY_STONE_BRICKS)
            || state.is(Blocks.CRACKED_STONE_BRICKS)
            || state.is(Blocks.CHISELED_STONE_BRICKS)
            || state.is(Blocks.DEEPSLATE_BRICKS)
            || state.is(Blocks.DEEPSLATE_TILES);
    }

    private static boolean isStoneSeries(BlockState state) {
        return state.is(Blocks.STONE)
            || state.is(Blocks.SMOOTH_STONE)
            || state.is(Blocks.COBBLESTONE)
            || state.is(Blocks.MOSSY_COBBLESTONE)
            || state.is(Blocks.GRANITE)
            || state.is(Blocks.POLISHED_GRANITE)
            || state.is(Blocks.DIORITE)
            || state.is(Blocks.POLISHED_DIORITE)
            || state.is(Blocks.ANDESITE)
            || state.is(Blocks.POLISHED_ANDESITE)
            || state.is(Blocks.TUFF)
            || state.is(Blocks.CALCITE);
    }

    // 龙蛋落地时2%概率生成迷你末影龙，每个投射物仅触发一次
    private void trySpawnMiniDragon(BlockHitResult hitResult) {
        if (hasSpawnedMiniDragon || !isDragonEggItem() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        hasSpawnedMiniDragon = true;

        if (RANDOM.nextFloat() >= 0.5F) {
            return;
        }

        EnderDragon dragon = EntityType.ENDER_DRAGON.create(serverLevel, EntitySpawnReason.EVENT);
        if (dragon == null) {
            return;
        }

        dragon.setPos(hitResult.getLocation());
        dragon.setInvulnerable(true);
        dragon.setTarget(null);
        dragon.setNoGravity(true);

        var scaleAttr = dragon.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.0D / 3.0D);
        }

        var speedAttr = dragon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedAttr.getBaseValue() * 3.0D);
        }

        serverLevel.addFreshEntity(dragon);
        DRAGONS_TO_REMOVE.put(dragon.getUUID(), level().getGameTime() + MINI_DRAGON_LIFETIME);
    }

    // 移除过期迷你龙
    private void removeExpiredMiniDragons() {
        if (!(level() instanceof ServerLevel serverLevel) || DRAGONS_TO_REMOVE.isEmpty()) {
            return;
        }

        long gameTime = level().getGameTime();
        var iterator = DRAGONS_TO_REMOVE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (gameTime >= entry.getValue()) {
                Entity entity = serverLevel.getEntity(entry.getKey());
                if (entity != null) {
                    entity.discard();
                }
                iterator.remove();
            }
        }
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
