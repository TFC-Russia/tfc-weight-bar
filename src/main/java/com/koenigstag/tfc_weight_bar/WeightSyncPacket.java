package com.koenigstag.tfc_weight_bar;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

// server -> client: current weight state for the HUD bar, and the load of the cart the player draws, if any
public record WeightSyncPacket(int weight, int maxWeight, boolean exhausted, boolean overburdened,
    @Nullable CartLoad cart) {

  public void encode(FriendlyByteBuf buffer) {
    buffer.writeVarInt(weight);
    buffer.writeVarInt(maxWeight);
    buffer.writeBoolean(exhausted);
    buffer.writeBoolean(overburdened);
    buffer.writeBoolean(cart != null);
    if (cart != null) {
      cart.encode(buffer);
    }
  }

  public static WeightSyncPacket decode(FriendlyByteBuf buffer) {
    return new WeightSyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
        buffer.readBoolean() ? CartLoad.decode(buffer) : null);
  }

  // client side, runs on the main thread
  public void handle(Supplier<NetworkEvent.Context> context) {
    WeightBarClientData.update(this);
  }
}
