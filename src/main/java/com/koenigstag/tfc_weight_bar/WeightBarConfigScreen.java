package com.koenigstag.tfc_weight_bar;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Context;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Element;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.Nullable;

// Client only: TFCWeightBar calls it on the client, so a dedicated server never loads the screen classes
public final class WeightBarConfigScreen {
  private static final Component FORCED_BY_SERVER =
      Component.translatable(Constants.MOD_ID + ".configuration.forced_by_server");

  private WeightBarConfigScreen() {}

  /**
   * Mods > TFC Weight Bar > Config: NeoForge's own config screen, for the client config and, in single player, the
   * server config. It saves the file, NeoForge reloads it, and Config picks the values up from the reload, so the HUD
   * changes without a restart.
   */
  public static void register(final ModContainer container) {
    container.registerExtensionPoint(IConfigScreenFactory.class,
        (mod, parent) -> new ConfigurationScreen(mod, parent, WeightBarConfigScreen::lockForcedOptions));
  }

  /**
   * A server that decides a client option for every player wins over the client config, so editing it would change
   * nothing: the option shows the server's value instead, stays off, and its tooltip says why, as on 1.20.1.
   */
  @Nullable
  private static Element lockForcedOptions(final Context context, final String key, @Nullable final Element original) {
    // Outside a world the force values are left over from the last server, which no longer decides anything
    if (original == null || context.modSpec() != Config.CLIENT_SPEC || Minecraft.getInstance().level == null) {
      return original;
    }

    final Component forcedValue = switch (key) {
      case "weight_display" -> Config.forceWeightDisplay == Config.ForceWeightDisplay.CLIENT
          ? null : Config.getWeightDisplay().getTranslatedName();
      case "bar_position" -> Config.forceBarPosition == Config.ForceBarPosition.CLIENT
          ? null : Config.getBarPosition().getTranslatedName();
      // NeoForge's own boolean button reads ON and OFF the same way
      case "show_bar_numbers" -> Config.forceBarNumbers == Config.ForceToggle.CLIENT
          ? null : CommonComponents.optionStatus(Config.areBarNumbersShown());
      default -> null;
    };
    if (forcedValue == null) {
      return original;
    }

    final Button button = Button.builder(forcedValue, pressed -> {})
        .width(Button.DEFAULT_WIDTH)
        .tooltip(Tooltip.create(FORCED_BY_SERVER))
        .build();
    button.active = false;

    final Component tooltip = Component.empty()
        .append(original.tooltip() != null ? original.tooltip() : Component.empty())
        .append("\n\n")
        .append(FORCED_BY_SERVER.copy().withStyle(ChatFormatting.RED));
    return new Element(original.name(), tooltip, button, false);
  }
}
