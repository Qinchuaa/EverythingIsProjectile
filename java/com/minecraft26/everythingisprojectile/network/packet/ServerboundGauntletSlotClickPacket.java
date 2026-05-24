package com.minecraft26.everythingisprojectile.network.packet;

import com.minecraft26.everythingisprojectile.gauntlet.GauntletSlotData;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundGauntletSlotClickPacket(int mouseButton) {
    public static void encode(ServerboundGauntletSlotClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mouseButton);
    }

    public static ServerboundGauntletSlotClickPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundGauntletSlotClickPacket(buffer.readVarInt());
    }

    public static void handle(ServerboundGauntletSlotClickPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        ItemStack slotted = GauntletSlotData.get(player);

        if (packet.mouseButton == 1) {
            handleRightClick(player, carried, slotted);
        } else {
            handleLeftClick(player, carried, slotted);
        }

        player.containerMenu.broadcastChanges();
        NetworkHandler.syncGauntletSlot(player, GauntletSlotData.get(player));
    }

    private static void handleLeftClick(ServerPlayer player, ItemStack carried, ItemStack slotted) {
        if (carried.isEmpty()) {
            pickUpSlot(player, slotted);
            return;
        }

        if (!GauntletSlotData.mayEquip(carried)) {
            return;
        }

        ItemStack carryCopy = carried.copyWithCount(1);
        if (slotted.isEmpty()) {
            GauntletSlotData.set(player, carryCopy);
            player.containerMenu.setCarried(ItemStack.EMPTY);
        } else {
            GauntletSlotData.set(player, carryCopy);
            player.containerMenu.setCarried(slotted);
        }
    }

    private static void handleRightClick(ServerPlayer player, ItemStack carried, ItemStack slotted) {
        if (carried.isEmpty()) {
            pickUpSlot(player, slotted);
            return;
        }

        if (!slotted.isEmpty() || !GauntletSlotData.mayEquip(carried)) {
            return;
        }

        GauntletSlotData.set(player, carried.copyWithCount(1));
        if (carried.getCount() <= 1) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        } else {
            ItemStack remaining = carried.copy();
            remaining.shrink(1);
            player.containerMenu.setCarried(remaining);
        }
    }

    private static void pickUpSlot(ServerPlayer player, ItemStack slotted) {
        if (slotted.isEmpty()) {
            return;
        }

        if (!player.containerMenu.getCarried().isEmpty()) {
            return;
        }

        GauntletSlotData.set(player, ItemStack.EMPTY);
        player.containerMenu.setCarried(slotted);
    }
}
