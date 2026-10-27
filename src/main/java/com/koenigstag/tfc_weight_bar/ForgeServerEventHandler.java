package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.dries007.tfc.config.TFCConfig;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class ForgeServerEventHandler {

  public static void init() {
    final IEventBus bus = NeoForge.EVENT_BUS;

    bus.register(ForgeServerEventHandler.class);

    Constants.LOGGER.info("Setup server event handler complete");
  }

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    // PlayerTickEvent.Post fires once, after the tick
    if (Config.enableWeightCalculations == false) {
      return;
    }

    // When facing up in the rain, player slowly recovers thirst.
    final Player player = event.getEntity();
    final Level level = player.level();

    // WeightBar calculations onPlayerTick
    if (!level.isClientSide() && !player.getAbilities().invulnerable && TFCConfig.SERVER.enableOverburdening.get()
        && level.getGameTime() % Config.calculateWeightEachNTicks == 0) {
      final int playerInvWeight = WeightBarHelpers.calculatePlayerFullWeight(player);

      // set current weight do display
      WeightBarData.serverChangeCurrentWeight(player, playerInvWeight);
    }
  }
}
