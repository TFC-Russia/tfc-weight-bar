package com.koenigstag.tfc_weight_bar;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WeightBarNetwork {
  private static final String PROTOCOL_VERSION = "1";

  private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation(Constants.MOD_ID, "main"),
      () -> PROTOCOL_VERSION,
      PROTOCOL_VERSION::equals,
      PROTOCOL_VERSION::equals);

  public static void init() {
    CHANNEL.messageBuilder(WeightSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(WeightSyncPacket::encode)
        .decoder(WeightSyncPacket::decode)
        .consumerMainThread(WeightSyncPacket::handle)
        .add();

    Constants.info("Setup network channel complete");
  }

  // server side
  public static void sendToPlayer(ServerPlayer player, WeightSyncPacket packet) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
  }
}
