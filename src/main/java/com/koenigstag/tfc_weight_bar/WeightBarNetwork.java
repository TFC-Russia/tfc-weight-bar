package com.koenigstag.tfc_weight_bar;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 1.21 registers payloads through RegisterPayloadHandlersEvent instead of building a SimpleChannel, so this registers
 * itself and needs no init call from the mod constructor.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class WeightBarNetwork {
  private static final String PROTOCOL_VERSION = "1";

  private WeightBarNetwork() {
  }

  @SubscribeEvent
  public static void register(final RegisterPayloadHandlersEvent event) {
    final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

    registrar.playToClient(WeightSyncPacket.TYPE, WeightSyncPacket.STREAM_CODEC, WeightBarNetwork::handleOnClient);

    Constants.LOGGER.info("Setup network channel complete");
  }

  // client side, the payload context already puts this on the main thread
  private static void handleOnClient(final WeightSyncPacket packet, final IPayloadContext context) {
    WeightBarClientData.update(packet);
  }

  // server side
  public static void sendToPlayer(ServerPlayer player, WeightSyncPacket packet) {
    PacketDistributor.sendToPlayer(player, packet);
  }
}
