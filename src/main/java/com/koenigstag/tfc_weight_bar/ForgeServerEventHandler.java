package com.koenigstag.tfc_weight_bar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.dries007.tfc.config.TFCConfig;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Bus.FORGE, value = Dist.DEDICATED_SERVER)
public final class ForgeServerEventHandler {

  public static void init() {
    final IEventBus bus = MinecraftForge.EVENT_BUS;

    bus.register(ForgeServerEventHandler.class);

    Constants.LOGGER.info("Setup server event handler complete");
  }

  @SubscribeEvent
  public static void onLevelTick(TickEvent.LevelTickEvent event) {
    // Before the level ticks its entities: a cart burdens whoever pulls it as it ticks, and up to TFC Astikor Carts 1.1.8
    // that includes the rider of its animal, whom a TFC horse then kicks off; and a held animal moves as it ticks.
    // Every tick, limits on or off, so that held animals get their speed back when they are turned off.
    if (event.phase == TickEvent.Phase.START && event.level instanceof ServerLevel level) {
      WeightBarHelpers.tickCarts(level);
    }
  }

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    // fires for both START and END phases, calculate once per tick
    if (Config.enableWeightCalculations == false || event.phase != TickEvent.Phase.END) {
      return;
    }

    // When facing up in the rain, player slowly recovers thirst.
    final Player player = event.player;
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
