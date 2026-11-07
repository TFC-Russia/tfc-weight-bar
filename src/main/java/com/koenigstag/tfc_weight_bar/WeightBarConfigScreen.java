package com.koenigstag.tfc_weight_bar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.Locale;

/**
 * Mods > TFC Weight Bar > Config. Forge 1.20.1 builds no screen from a config spec and keeps that button off until a
 * mod registers one, so this is the screen: the client options, applied the moment they change. The server options
 * stay in the server config file.
 */
public class WeightBarConfigScreen extends Screen {
  private static final String KEY = Constants.MOD_ID + ".configuration.";

  private final Screen parent;

  private WeightBarConfigScreen(final Screen parent) {
    super(Component.translatable(KEY + "title"));
    this.parent = parent;
  }

  // Client only: TFCWeightBar calls it on the client, so a dedicated server never loads the screen classes
  public static void register() {
    ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
        () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new WeightBarConfigScreen(parent)));
  }

  @Override
  protected void init() {
    // Each button starts on the value in effect, which is the server's when it decides the option for everyone
    final int x = width / 2 - 100;
    int y = height / 4;

    addOption(CycleButton.<Config.WeightDisplay>builder(
            display -> Component.translatable(KEY + "weight_display." + display.name().toLowerCase(Locale.ROOT)))
        .withValues(Config.WeightDisplay.values())
        .withInitialValue(Config.getWeightDisplay())
        .create(x, y, 200, 20, Component.translatable(KEY + "weight_display"),
            (button, value) -> Config.setWeightDisplay(value)),
        "weight_display", Config.forceWeightDisplay != Config.ForceWeightDisplay.CLIENT);
    y += 24;

    addOption(CycleButton.<Config.BarPosition>builder(
            position -> Component.translatable(KEY + "bar_position." + position.name().toLowerCase(Locale.ROOT)))
        .withValues(Config.BarPosition.values())
        .withInitialValue(Config.getBarPosition())
        .create(x, y, 200, 20, Component.translatable(KEY + "bar_position"),
            (button, value) -> Config.setBarPosition(value)),
        "bar_position", Config.forceBarPosition != Config.ForceBarPosition.CLIENT);
    y += 24;

    addOption(CycleButton.onOffBuilder(Config.areBarNumbersShown())
        .create(x, y, 200, 20, Component.translatable(KEY + "show_bar_numbers"),
            (button, value) -> Config.setShowBarNumbers(value)),
        "show_bar_numbers", Config.forceBarNumbers != Config.ForceToggle.CLIENT);

    addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
        .bounds(x, height - 28, 200, 20)
        .build());
  }

  /**
   * A server that decides an option for every player wins over the client config, so the button would change nothing:
   * it stays off, and its tooltip says why.
   */
  private void addOption(final CycleButton<?> button, final String key, final boolean forcedByServer) {
    button.active = !forcedByServer;
    button.setTooltip(Tooltip.create(Component.translatable(KEY + (forcedByServer ? "forced_by_server" : key + ".tooltip"))));
    addRenderableWidget(button);
  }

  @Override
  public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }
}
