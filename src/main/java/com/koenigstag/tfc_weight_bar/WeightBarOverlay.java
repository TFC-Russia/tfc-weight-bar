package com.koenigstag.tfc_weight_bar;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

// HUD weight bar, the thick bar from the TFCSU (TFC Russia) 1.7.10 HUD, and above it the cart bar while a cart burdens
// the player
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class WeightBarOverlay {
  // texture rows (each 90x10): background, fill
  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/weight_bar.png");
  private static final ResourceLocation LAYER_ID =
      ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "weight_bar");
  private static final int TEXTURE_WIDTH = 128;
  private static final int TEXTURE_HEIGHT = 32;

  private static final int BAR_WIDTH = 90;
  private static final int BAR_HEIGHT = 10;
  private static final int FILL_V = 10;

  // The fill texture turns pink from this pixel on, its last section before the end
  private static final double FILL_LAST_SECTION = 75.0 / BAR_WIDTH;

  // The cart bar's fill is tinted wood brown, to tell it from the grey weight bar below it
  private static final float CART_TINT_RED = 0.85f;
  private static final float CART_TINT_GREEN = 0.62f;
  private static final float CART_TINT_BLUE = 0.38f;

  private static final int NORMAL_TEXT_COLOR = 0xFFFFFF;
  private static final int EXHAUSTED_TEXT_COLOR = 0xFFFF00;
  private static final int OVERBURDENED_TEXT_COLOR = 0xFF0000;

  // TFC registers its thirst and food bars above the vanilla food level, food on top. TFC 4.2 builds their names
  // without a namespace, so they come out as minecraft:food and minecraft:thirst; tfc:food is tried first in case
  // that gets fixed.
  private static final List<ResourceLocation> TFC_FOOD_OVERLAYS = List.of(
      ResourceLocation.fromNamespaceAndPath("tfc", "food"),
      ResourceLocation.withDefaultNamespace("food"));

  /**
   * 1.21 replaced the Forge overlay list with the vanilla layered draw: a layer is registered by name and is handed
   * only the graphics and the frame timer. The running bar offset survived the move, it just sits on the vanilla Gui
   * now instead of on ForgeGui, and TFC stacks its own food and thirst bars with it the same way.
   */
  @SubscribeEvent
  public static void onRegisterLayers(RegisterGuiLayersEvent event) {
    for (ResourceLocation tfcFood : TFC_FOOD_OVERLAYS) {
      try {
        // stack the weight bar on top of the TFC food bar
        event.registerAbove(tfcFood, LAYER_ID, WeightBarOverlay::render);
        return;
      } catch (IllegalArgumentException | IllegalStateException e) {
        // not registered under this name
      }
    }

    Constants.LOGGER.info("No tfc food layer to stack on, falling back to the vanilla food level");
    event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, LAYER_ID, WeightBarOverlay::render);
  }

  private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
    final Minecraft minecraft = Minecraft.getInstance();
    final WeightSyncPacket data = WeightBarClientData.get();

    if (Config.enableWeightCalculations == false || Config.getWeightDisplay() != Config.WeightDisplay.HUD || data == null) {
      return;
    }

    // What ForgeGui.shouldDrawSurvivalElements used to check: no bars in creative or spectator
    if (minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()
        || !(minecraft.getCameraEntity() instanceof Player)) {
      return;
    }

    final Gui gui = minecraft.gui;

    // The layer comes after the health, armour and food layers, so each side's running height already counts its
    // bars: the health side stacks up from the left half of the hotbar, the food side from the right half
    final boolean onHealth = Config.getBarPosition() == Config.BarPosition.HEALTH;
    final int x = onHealth ? graphics.guiWidth() / 2 - 91 : graphics.guiWidth() / 2 + 1;

    final double weightFill = data.maxWeight() <= 0 ? 0 : (double) data.weight() / (double) data.maxWeight();
    drawBar(gui, graphics, minecraft.font, x, onHealth, weightFill, false,
        data.weight() + "/" + data.maxWeight(), getTextColor(data.exhausted(), data.overburdened()));

    data.cart().filter(cart -> Config.isCartBarShown()).ifPresent(cart -> drawBar(gui, graphics, minecraft.font, x, onHealth, getCartFill(cart), true,
        Math.round(cart.load()) + "/" + Math.round(cart.overburdenedLoad()),
        getTextColor(cart.isExhausted(), cart.isOverburdened())));
  }

  /**
   * Draws a bar on top of the bars already on that side, and counts it in that side's height for the next one.
   */
  private static void drawBar(Gui gui, GuiGraphics graphics, Font font, int x, boolean onHealth, double fill,
      boolean cartTint, String text, int textColor) {
    // bottom aligned with the row the bars below ended on, 1px gap to the bar below
    final int y = graphics.guiHeight() - (onHealth ? gui.leftHeight : gui.rightHeight) - 1;
    final int fillWidth = (int) (BAR_WIDTH * Mth.clamp(fill, 0, 1));

    graphics.blit(TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    if (cartTint) {
      graphics.setColor(CART_TINT_RED, CART_TINT_GREEN, CART_TINT_BLUE, 1f);
    }
    graphics.blit(TEXTURE, x, y, 0, FILL_V, fillWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    if (cartTint) {
      graphics.setColor(1f, 1f, 1f, 1f);
    }

    if (Config.areBarNumbersShown()) {
      drawBarText(graphics, font, text, textColor, x, y);
    }

    // leave the next bar room above this one
    if (onHealth) {
      gui.leftHeight += BAR_HEIGHT + 1;
    } else {
      gui.rightHeight += BAR_HEIGHT + 1;
    }
  }

  /**
   * How full the cart bar is. TFC Astikor Carts makes the player Exhausted past one load and Overburdened past another,
   * at a ratio of its own, so the bar puts the first where its pink last section starts and the second at its end:
   * pink means Exhausted and full means Overburdened, as on the weight bar. Too many large items fill it too.
   */
  static double getCartFill(final CartLoad cart) {
    if (cart.overburdenedLoad() <= 0 || cart.hasTooManyLargeItems()) {
      return 1;
    }
    if (cart.exhaustedLoad() <= 0 || cart.exhaustedLoad() >= cart.overburdenedLoad()) {
      return cart.load() / cart.overburdenedLoad();
    }
    if (cart.load() <= cart.exhaustedLoad()) {
      return FILL_LAST_SECTION * cart.load() / cart.exhaustedLoad();
    }
    return FILL_LAST_SECTION + (1 - FILL_LAST_SECTION)
        * (cart.load() - cart.exhaustedLoad()) / (cart.overburdenedLoad() - cart.exhaustedLoad());
  }

  private static int getTextColor(boolean exhausted, boolean overburdened) {
    if (overburdened) {
      return OVERBURDENED_TEXT_COLOR;
    }
    return exhausted ? EXHAUSTED_TEXT_COLOR : NORMAL_TEXT_COLOR;
  }

  private static void drawBarText(GuiGraphics graphics, Font font, String text, int color, int x, int y) {
    // same text size and placement as the TFC health bar
    final PoseStack pose = graphics.pose();
    pose.pushPose();
    pose.translate(x + BAR_WIDTH / 2.0, y + 2.5, 0);
    pose.scale(0.8f, 0.8f, 1f);
    graphics.drawString(font, text, -font.width(text) / 2, 0, color, true);
    pose.popPose();
  }

  @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
  public static class GameEvents {
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      // do not show the previous world weight in the next one
      WeightBarClientData.reset();
    }
  }
}
