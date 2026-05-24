package com.minecraft26.everythingisprojectile.network.packet;

import com.minecraft26.everythingisprojectile.client.GauntletCarryClientState;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ClientboundGauntletCarrySyncPacket(boolean carrying, ItemStack stack, int lockedSlot) {
    public static void encode(ClientboundGauntletCarrySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.carrying);
        NetworkHandler.writeItemStack(buffer, packet.stack);
        buffer.writeVarInt(packet.lockedSlot);
    }

    public static ClientboundGauntletCarrySyncPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundGauntletCarrySyncPacket(
            buffer.readBoolean(),
            NetworkHandler.readItemStack(buffer),
            buffer.readVarInt()
        );
    }

    public static void handle(ClientboundGauntletCarrySyncPacket packet, CustomPayloadEvent.Context context) {
        GauntletCarryClientState.update(packet.carrying, packet.stack, packet.lockedSlot);
    }
}
