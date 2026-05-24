package com.minecraft26.everythingisprojectile.network.packet;

import com.minecraft26.everythingisprojectile.client.GauntletSlotClientState;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ClientboundGauntletSlotSyncPacket(ItemStack stack) {
    public static void encode(ClientboundGauntletSlotSyncPacket packet, FriendlyByteBuf buffer) {
        NetworkHandler.writeItemStack(buffer, packet.stack);
    }

    public static ClientboundGauntletSlotSyncPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundGauntletSlotSyncPacket(NetworkHandler.readItemStack(buffer));
    }

    public static void handle(ClientboundGauntletSlotSyncPacket packet, CustomPayloadEvent.Context context) {
        GauntletSlotClientState.setStack(packet.stack);
    }
}
