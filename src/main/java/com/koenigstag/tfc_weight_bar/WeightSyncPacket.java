package com.koenigstag.tfc_weight_bar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// server -> client: current weight state for the HUD bar
// 1.21 replaced the Forge SimpleChannel message with a CustomPacketPayload and a StreamCodec
public record WeightSyncPacket(int weight, int maxWeight, boolean exhausted, boolean overburdened)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<WeightSyncPacket> TYPE =
      new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "weight_sync"));

  public static final StreamCodec<RegistryFriendlyByteBuf, WeightSyncPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, WeightSyncPacket::weight,
      ByteBufCodecs.VAR_INT, WeightSyncPacket::maxWeight,
      ByteBufCodecs.BOOL, WeightSyncPacket::exhausted,
      ByteBufCodecs.BOOL, WeightSyncPacket::overburdened,
      WeightSyncPacket::new);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
