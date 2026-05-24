package com.minecraft26.everythingisprojectile.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.minecraft26.everythingisprojectile.ammo.AmmoItemSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public class AnyProjectileEntityRenderer extends EntityRenderer<AnyProjectileEntity, AnyProjectileEntityRenderer.AnyProjectileRenderState> {
    private static final float ITEM_PROJECTILE_SCALE = 3.0F;
    private static final float BLOCK_PROJECTILE_SCALE = 1.2F;
    private static final float SPIN_DEGREES_PER_TICK = 25.0F;
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Identifier WATER_STILL_SPRITE = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier LAVA_STILL_SPRITE = Identifier.withDefaultNamespace("block/lava_still");
    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;
    private final TextureAtlas blockAtlas;

    public AnyProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        this.itemModelResolver = context.getItemModelResolver();
        this.blockAtlas = context.getAtlas(AtlasIds.BLOCKS);
    }

    @Override
    public void submit(AnyProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.spin));

        if (state.fluidRenderType != FluidRenderType.NONE) {
            poseStack.scale(BLOCK_PROJECTILE_SCALE, BLOCK_PROJECTILE_SCALE, BLOCK_PROJECTILE_SCALE);
            submitFluidCube(poseStack, submitNodeCollector, state);
        } else if (!state.blockModel.isEmpty()) {
            poseStack.scale(BLOCK_PROJECTILE_SCALE, BLOCK_PROJECTILE_SCALE, BLOCK_PROJECTILE_SCALE);
            submitBlockModel(state.blockModel, state.blockOffsetX, state.blockOffsetY, state.blockOffsetZ, poseStack, submitNodeCollector, state);
            if (!state.secondaryBlockModel.isEmpty()) {
                submitBlockModel(
                    state.secondaryBlockModel,
                    state.secondaryBlockOffsetX,
                    state.secondaryBlockOffsetY,
                    state.secondaryBlockOffsetZ,
                    poseStack,
                    submitNodeCollector,
                    state
                );
            }
        } else {
            poseStack.scale(ITEM_PROJECTILE_SCALE, ITEM_PROJECTILE_SCALE, ITEM_PROJECTILE_SCALE);
            state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public AnyProjectileRenderState createRenderState() {
        return new AnyProjectileRenderState();
    }

    @Override
    public void extractRenderState(AnyProjectileEntity entity, AnyProjectileRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        updateProjectileModel(entity.getItem(), state, entity);
        applyFlightRotation(entity, state, partialTicks);
        state.spin = entity.isStuckInSurface() ? 0.0F : (entity.tickCount + partialTicks) * SPIN_DEGREES_PER_TICK;
    }

    private void updateProjectileModel(ItemStack stack, AnyProjectileRenderState state, AnyProjectileEntity entity) {
        state.item.clear();
        state.blockModel.clear();
        state.secondaryBlockModel.clear();
        state.blockOffsetX = 0.0F;
        state.blockOffsetY = 0.0F;
        state.blockOffsetZ = 0.0F;
        state.secondaryBlockOffsetX = 0.0F;
        state.secondaryBlockOffsetY = 0.0F;
        state.secondaryBlockOffsetZ = 0.0F;
        state.fluidRenderType = FluidRenderType.NONE;

        if (stack.is(Items.WATER_BUCKET)) {
            state.fluidRenderType = FluidRenderType.WATER;
            return;
        }

        if (stack.is(Items.LAVA_BUCKET)) {
            state.fluidRenderType = FluidRenderType.LAVA;
            return;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            BlockState blockState = AmmoItemSupport.resolveBlockState(stack);
            if (blockState == null) {
                blockState = blockItem.getBlock().defaultBlockState();
            }
            if (shouldRenderAsPlacedBlock(blockItem, blockState) && blockState.getRenderShape() == RenderShape.MODEL) {
                if (tryBuildCombinedBlockModels(blockItem, state)) {
                    return;
                }

                this.blockModelResolver.update(state.blockModel, blockState, BLOCK_DISPLAY_CONTEXT);
                return;
            }
        }

        this.itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.GROUND, entity);
    }

    private static boolean shouldRenderAsPlacedBlock(BlockItem blockItem, BlockState blockState) {
        return !(blockItem.getBlock() instanceof CropBlock)
            && !(blockItem.getBlock() instanceof NetherWartBlock)
            && !(blockItem.getBlock() instanceof SweetBerryBushBlock)
            && !(blockItem.getBlock() instanceof TorchflowerCropBlock)
            && !(blockItem.getBlock() instanceof PitcherCropBlock)
            && !(blockItem.getBlock() instanceof CocoaBlock);
    }

    private boolean tryBuildCombinedBlockModels(BlockItem blockItem, AnyProjectileRenderState state) {
        if (blockItem.getBlock() instanceof BedBlock) {
            Direction facing = Direction.NORTH;
            BlockState footState = blockItem.getBlock()
                .defaultBlockState()
                .setValue(BedBlock.FACING, facing)
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.OCCUPIED, false);
            BlockState headState = footState.setValue(BedBlock.PART, BedPart.HEAD);
            this.blockModelResolver.update(state.blockModel, footState, BLOCK_DISPLAY_CONTEXT);
            this.blockModelResolver.update(state.secondaryBlockModel, headState, BLOCK_DISPLAY_CONTEXT);
            state.blockOffsetX = -facing.getStepX() * 0.5F;
            state.blockOffsetZ = -facing.getStepZ() * 0.5F;
            state.secondaryBlockOffsetX = facing.getStepX() * 0.5F;
            state.secondaryBlockOffsetZ = facing.getStepZ() * 0.5F;
            return true;
        }

        if (blockItem.getBlock() instanceof DoorBlock || blockItem.getBlock() instanceof DoublePlantBlock) {
            BlockState lowerState = blockItem.getBlock().defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            BlockState upperState = lowerState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            this.blockModelResolver.update(state.blockModel, lowerState, BLOCK_DISPLAY_CONTEXT);
            this.blockModelResolver.update(state.secondaryBlockModel, upperState, BLOCK_DISPLAY_CONTEXT);
            state.blockOffsetY = -0.5F;
            state.secondaryBlockOffsetY = 0.5F;
            return true;
        }

        return false;
    }

    private void submitBlockModel(
        BlockModelRenderState blockModel,
        float offsetX,
        float offsetY,
        float offsetZ,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        AnyProjectileRenderState state
    ) {
        poseStack.pushPose();
        poseStack.translate(offsetX - 0.5F, offsetY - 0.5F, offsetZ - 0.5F);
        blockModel.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }

    private void submitFluidCube(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, AnyProjectileRenderState state) {
        TextureAtlasSprite sprite = state.fluidRenderType == FluidRenderType.WATER
            ? this.blockAtlas.getSprite(WATER_STILL_SPRITE)
            : this.blockAtlas.getSprite(LAVA_STILL_SPRITE);
        int red = state.fluidRenderType == FluidRenderType.WATER ? 72 : 255;
        int green = state.fluidRenderType == FluidRenderType.WATER ? 116 : 255;
        int blue = state.fluidRenderType == FluidRenderType.WATER ? 255 : 255;
        int alpha = state.fluidRenderType == FluidRenderType.WATER ? 180 : 230;
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
            (pose, buffer) -> renderFluidCube(buffer, pose, sprite, state.lightCoords, red, green, blue, alpha)
        );
    }

    private static void renderFluidCube(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        TextureAtlasSprite sprite,
        int lightCoords,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        float min = -0.5F;
        float max = 0.5F;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        addQuad(buffer, pose, min, min, max, max, min, max, max, max, max, min, max, max, 0.0F, 0.0F, 1.0F, u0, v0, u1, v1, lightCoords, red, green, blue, alpha);
        addQuad(buffer, pose, max, min, min, min, min, min, min, max, min, max, max, min, 0.0F, 0.0F, -1.0F, u0, v0, u1, v1, lightCoords, red, green, blue, alpha);
        addQuad(buffer, pose, min, max, min, min, max, max, max, max, max, max, max, min, 0.0F, 1.0F, 0.0F, u0, v0, u1, v1, lightCoords, red, green, blue, alpha);
        addQuad(buffer, pose, min, min, max, min, min, min, max, min, min, max, min, max, 0.0F, -1.0F, 0.0F, u0, v0, u1, v1, lightCoords, red, green, blue, alpha);
        addQuad(buffer, pose, min, min, min, min, min, max, min, max, max, min, max, min, -1.0F, 0.0F, 0.0F, u0, v0, u1, v1, lightCoords, red, green, blue, alpha);
        addQuad(buffer, pose, max, min, max, max, min, min, max, max, min, max, max, max, 1.0F, 0.0F, 0.0F, u0, v0, u1, v1, lightCoords, red, green, blue, alpha);
    }

    private static void addQuad(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        float nx,
        float ny,
        float nz,
        float u0,
        float v0,
        float u1,
        float v1,
        int lightCoords,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        addVertex(buffer, pose, x1, y1, z1, u0, v1, nx, ny, nz, lightCoords, red, green, blue, alpha);
        addVertex(buffer, pose, x2, y2, z2, u1, v1, nx, ny, nz, lightCoords, red, green, blue, alpha);
        addVertex(buffer, pose, x3, y3, z3, u1, v0, nx, ny, nz, lightCoords, red, green, blue, alpha);
        addVertex(buffer, pose, x4, y4, z4, u0, v0, nx, ny, nz, lightCoords, red, green, blue, alpha);
    }

    private static void addVertex(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        float nx,
        float ny,
        float nz,
        int lightCoords,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        buffer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(lightCoords)
            .setNormal(pose, nx, ny, nz);
    }

    private static void applyFlightRotation(AnyProjectileEntity entity, AnyProjectileRenderState state, float partialTicks) {
        Vec3 movement = entity.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-7D) {
            state.yaw = entity.getYRot();
            state.pitch = entity.getXRot();
            return;
        }

        float horizontalSpeed = (float) Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        float yaw = (float) (Mth.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(movement.y, horizontalSpeed) * Mth.RAD_TO_DEG);
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, yaw);
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, pitch);
    }

    public static final class AnyProjectileRenderState extends ThrownItemRenderState {
        public final BlockModelRenderState blockModel = new BlockModelRenderState();
        public final BlockModelRenderState secondaryBlockModel = new BlockModelRenderState();
        public float blockOffsetX;
        public float blockOffsetY;
        public float blockOffsetZ;
        public float secondaryBlockOffsetX;
        public float secondaryBlockOffsetY;
        public float secondaryBlockOffsetZ;
        public float yaw;
        public float pitch;
        public float spin;
        public FluidRenderType fluidRenderType = FluidRenderType.NONE;
    }

    private enum FluidRenderType {
        NONE,
        WATER,
        LAVA
    }
}
