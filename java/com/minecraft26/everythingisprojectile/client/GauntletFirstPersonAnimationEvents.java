package com.minecraft26.everythingisprojectile.client;

import com.minecraft26.everythingisprojectile.ammo.AmmoItemSupport;
import com.minecraft26.everythingisprojectile.item.ProjectileGauntletItem;
import com.minecraft26.everythingisprojectile.registry.ModItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderHandEvent;

import java.util.function.Predicate;

public final class GauntletFirstPersonAnimationEvents {
    private GauntletFirstPersonAnimationEvents() {
    }

    public static void register() {
        RenderHandEvent.BUS.addListener((Predicate<RenderHandEvent>) GauntletFirstPersonAnimationEvents::onRenderHand);
    }

    private static boolean onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player != minecraft.getCameraEntity()) {
            return false;
        }

        ItemStack gauntletStack = GauntletSlotClientState.getStack();
        if (!ProjectileGauntletItem.isGauntlet(gauntletStack)) {
            return false;
        }

        if (!player.isUsingItem() || player.getUsedItemHand() != event.getHand()) {
            return false;
        }

        ItemStack heldStack = event.getItemStack();
        if (heldStack.isEmpty() || !AmmoItemSupport.isSupportedAmmo(heldStack) || ProjectileGauntletItem.isGauntlet(heldStack)) {
            return false;
        }

        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        ItemInHandRenderer renderer = getItemInHandRenderer(minecraft);
        if (renderer == null) {
            return false;
        }

        event.getPoseStack().pushPose();
        applyBowTransform(event, player, arm, heldStack);
        renderer.renderItem(
            player,
            heldStack,
            arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            event.getPoseStack(),
            event.getNodeCollector(),
            event.getPackedLight()
        );
        event.getPoseStack().popPose();
        return true;
    }

    private static ItemInHandRenderer getItemInHandRenderer(Minecraft minecraft) {
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        return dispatcher != null ? dispatcher.getItemInHandRenderer() : null;
    }

    private static void applyItemArmTransform(RenderHandEvent event, HumanoidArm arm) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        event.getPoseStack().translate(invert * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
    }

    private static void applyBowTransform(RenderHandEvent event, Player player, HumanoidArm arm, ItemStack heldStack) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        applyItemArmTransform(event, arm);
        event.getPoseStack().translate(invert * -0.2785682F, 0.18344387F, 0.15731531F);
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-13.935F));
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(invert * 35.3F));
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(invert * -9.785F));

        float timeHeld = heldStack.getUseDuration(player) - (player.getUseItemRemainingTicks() - event.getPartialTick() + 1.0F);
        float power = timeHeld / 20.0F;
        power = (power * power + power * 2.0F) / 3.0F;
        power = Math.min(power, 1.0F);

        if (power > 0.1F) {
            float shakeOffset = Mth.sin((timeHeld - 0.1F) * 1.3F);
            float shakeIntensity = power - 0.1F;
            float shake = shakeOffset * shakeIntensity;
            event.getPoseStack().translate(0.0F, shake * 0.004F, 0.0F);
        }

        event.getPoseStack().translate(0.0F, 0.0F, power * 0.04F);
        event.getPoseStack().scale(1.0F, 1.0F, 1.0F + power * 0.2F);
        event.getPoseStack().mulPose(Axis.YN.rotationDegrees(invert * 45.0F));
    }
}
