package com.koenigstag.tfc_weight_bar;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Client only: TFCWeightBar calls it on the client, so a dedicated server never loads the screen classes
public final class WeightBarConfigScreen {
  private WeightBarConfigScreen() {}

  /**
   * Mods > TFC Weight Bar > Config: NeoForge's own config screen, for the client config and, in single player, the
   * server config. It saves the file, NeoForge reloads it, and Config picks the values up from the reload, so the HUD
   * changes without a restart.
   */
  public static void register(final ModContainer container) {
    container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
  }
}
