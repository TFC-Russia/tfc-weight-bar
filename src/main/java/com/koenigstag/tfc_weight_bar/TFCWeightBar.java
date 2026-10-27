package com.koenigstag.tfc_weight_bar;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Constants.MOD_ID)
public class TFCWeightBar {
  public TFCWeightBar(IEventBus modEventBus, ModContainer container) {
    modEventBus.addListener(this::commonSetup);

    ForgeClientEventHandler.init();

    ForgeServerEventHandler.init();

    // WeightBarNetwork registers itself through RegisterPayloadHandlersEvent

    Config.init(container);
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    // Some common setup code
    Constants.debug("Setup complete");
  }
}
