package com.minecraft26.everythingisprojectile.network;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import com.minecraft26.everythingisprojectile.network.packet.ClientboundGauntletCarrySyncPacket;
import com.minecraft26.everythingisprojectile.network.packet.ClientboundGauntletSlotSyncPacket;
import com.minecraft26.everythingisprojectile.network.packet.ServerboundGauntletEmergencyEscapePacket;
import com.minecraft26.everythingisprojectile.network.packet.ServerboundGauntletPickupBlockPacket;
import com.minecraft26.everythingisprojectile.network.packet.ServerboundGauntletSlotClickPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class NetworkHandler {
    private static final SimpleChannel CHANNEL = ChannelBuilder
        .named(Identifier.fromNamespaceAndPath(EverythingIsProjectileMod.MODID, "main"))
        .networkProtocolVersion(1)
        .simpleChannel();

    private static boolean registered;
    private static int nextMessageId;

    private NetworkHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        CHANNEL.messageBuilder(ClientboundGauntletSlotSyncPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ClientboundGauntletSlotSyncPacket::encode)
            .decoder(ClientboundGauntletSlotSyncPacket::decode)
            .consumerMainThread(ClientboundGauntletSlotSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(ClientboundGauntletCarrySyncPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ClientboundGauntletCarrySyncPacket::encode)
            .decoder(ClientboundGauntletCarrySyncPacket::decode)
            .consumerMainThread(ClientboundGauntletCarrySyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(ServerboundGauntletSlotClickPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ServerboundGauntletSlotClickPacket::encode)
            .decoder(ServerboundGauntletSlotClickPacket::decode)
            .consumerMainThread(ServerboundGauntletSlotClickPacket::handle)
            .add();

        CHANNEL.messageBuilder(ServerboundGauntletEmergencyEscapePacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ServerboundGauntletEmergencyEscapePacket::encode)
            .decoder(ServerboundGauntletEmergencyEscapePacket::decode)
            .consumerMainThread(ServerboundGauntletEmergencyEscapePacket::handle)
            .add();

        CHANNEL.messageBuilder(ServerboundGauntletPickupBlockPacket.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ServerboundGauntletPickupBlockPacket::encode)
            .decoder(ServerboundGauntletPickupBlockPacket::decode)
            .consumerMainThread(ServerboundGauntletPickupBlockPacket::handle)
            .add();

        CHANNEL.build();
        registered = true;
    }

    public static void syncGauntletSlot(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        CHANNEL.send(new ClientboundGauntletSlotSyncPacket(stack), PacketDistributor.PLAYER.with(player));
    }

    public static void syncGauntletCarry(ServerPlayer player, boolean carrying, net.minecraft.world.item.ItemStack stack, int lockedSlot) {
        CHANNEL.send(new ClientboundGauntletCarrySyncPacket(carrying, stack, lockedSlot), PacketDistributor.PLAYER.with(player));
    }

    public static void sendGauntletClick(int mouseButton) {
        CHANNEL.send(new ServerboundGauntletSlotClickPacket(mouseButton), PacketDistributor.SERVER.noArg());
    }

    public static void sendEmergencyEscape() {
        CHANNEL.send(ServerboundGauntletEmergencyEscapePacket.INSTANCE, PacketDistributor.SERVER.noArg());
    }

    public static void sendPickupBlock() {
        CHANNEL.send(ServerboundGauntletPickupBlockPacket.INSTANCE, PacketDistributor.SERVER.noArg());
    }

    public static void writeItemStack(FriendlyByteBuf buffer, net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
    }

    public static net.minecraft.world.item.ItemStack readItemStack(FriendlyByteBuf buffer) {
        return net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    }
}
