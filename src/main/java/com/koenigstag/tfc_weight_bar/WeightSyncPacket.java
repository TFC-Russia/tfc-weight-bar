package com.koenigstag.tfc_weight_bar;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// server -> client: current weight state for the HUD bar
public record WeightSyncPacket(int weight, int maxWeight, boolean exhausted, boolean overburdened) {

  public void encode(FriendlyByteBuf buffer) {
    buffer.writeVarInt(weight);
    buffer.writeVarInt(maxWeight);
    buffer.writeBoolean(exhausted);
    buffer.writeBoolean(overburdened);
  }

  public static WeightSyncPacket decode(FriendlyByteBuf buffer) {
    return new WeightSyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
  }

  // client side, runs on the main thread
  public void handle(Supplier<NetworkEvent.Context> context) {
    WeightBarClientData.update(this);
  }
}
