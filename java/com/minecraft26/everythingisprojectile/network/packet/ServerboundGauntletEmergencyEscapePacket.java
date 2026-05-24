package com.minecraft26.everythingisprojectile.network.packet;

import com.minecraft26.everythingisprojectile.gauntlet.GauntletCarryData;
import com.minecraft26.everythingisprojectile.gauntlet.GauntletSlotData;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public final class ServerboundGauntletEmergencyEscapePacket {
    public static final ServerboundGauntletEmergencyEscapePacket INSTANCE = new ServerboundGauntletEmergencyEscapePacket();

    private ServerboundGauntletEmergencyEscapePacket() {
    }

    public static void encode(ServerboundGauntletEmergencyEscapePacket packet, FriendlyByteBuf buffer) {
    }

    public static ServerboundGauntletEmergencyEscapePacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(ServerboundGauntletEmergencyEscapePacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        ItemStack gauntletStack = GauntletSlotData.get(player);
        ItemStack carriedStack = GauntletCarryData.getCarriedStack(player);
        boolean restoredBlock = carriedStack.isEmpty() || GauntletCarryData.restoreCarriedBlock(player);

        if (!carriedStack.isEmpty()) {
            GauntletCarryData.restoreOriginalHands(player);
            if (!restoredBlock) {
                giveBackOrDrop(player, carriedStack.copyWithCount(1));
            }
        }

        if (!gauntletStack.isEmpty()) {
            GauntletSlotData.set(player, ItemStack.EMPTY);
            giveBackOrDrop(player, gauntletStack.copyWithCount(1));
        }

        player.containerMenu.broadcastChanges();
        NetworkHandler.syncGauntletCarry(player, GauntletCarryData.hasCarriedBlock(player), GauntletCarryData.getCarriedStack(player), GauntletCarryData.getLockedSlot(player));
        NetworkHandler.syncGauntletSlot(player, GauntletSlotData.get(player));
    }

    private static void giveBackOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
