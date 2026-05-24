package com.minecraft26.everythingisprojectile.network.packet;

import com.minecraft26.everythingisprojectile.gauntlet.GauntletCarryData;
import com.minecraft26.everythingisprojectile.gauntlet.GauntletSlotData;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;

public final class ServerboundGauntletPickupBlockPacket {
    public static final ServerboundGauntletPickupBlockPacket INSTANCE = new ServerboundGauntletPickupBlockPacket();

    private ServerboundGauntletPickupBlockPacket() {
    }

    public static void encode(ServerboundGauntletPickupBlockPacket packet, FriendlyByteBuf buffer) {
    }

    public static ServerboundGauntletPickupBlockPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ServerboundGauntletPickupBlockPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                || !player.isShiftKeyDown()
                || GauntletCarryData.hasCarriedBlock(player)
                || !GauntletSlotData.isEquipped(player)
                || player.getCooldowns().isOnCooldown(GauntletSlotData.get(player))) {
                return;
            }

            double range = player.blockInteractionRange();
            Vec3 from = player.getEyePosition();
            Vec3 to = from.add(player.getLookAngle().scale(range));
            BlockHitResult hitResult = player.level().clip(
                new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
            );
            if (hitResult.getType() != HitResult.Type.BLOCK
                || !player.isWithinBlockInteractionRange(hitResult.getBlockPos(), 0.0D)) {
                return;
            }

            if (GauntletCarryData.tryPickupBlock(player, hitResult.getBlockPos())) {
                NetworkHandler.syncGauntletCarry(player, true, GauntletCarryData.getCarriedStack(player), GauntletCarryData.getLockedSlot(player));
                player.containerMenu.broadcastChanges();
            }
        });
    }
}
