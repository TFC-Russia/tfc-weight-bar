package com.koenigstag.tfc_weight_bar;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class TFCWeightBar {
  public TFCWeightBar() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    modEventBus.addListener(this::commonSetup);

    // ForgeClientEventHandler registers itself through @Mod.EventBusSubscriber, on the client only: on a dedicated
    // server, where the client config never loads, it would send every player the weight message

    ForgeServerEventHandler.init();

    WeightBarNetwork.init();

    Config.init();
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    // Some common setup code
    Constants.debug("Setup complete");
  }
}
