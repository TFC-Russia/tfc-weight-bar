package com.koenigstag.tfc_weight_bar;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

// HUD weight bar, the thick bar from the TFCSU (TFC Russia) 1.7.10 HUD, and above it the cart bar while a cart burdens
// the player
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class WeightBarOverlay {
  // texture rows (each 90x10): background, fill
  private static final ResourceLocation TEXTURE = new ResourceLocation(Constants.MOD_ID, "textures/gui/weight_bar.png");
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

  // TFC registers its food and thirst bars above vanilla food level
  private static final ResourceLocation TFC_FOOD_OVERLAY = new ResourceLocation("tfc", "food");

  @SubscribeEvent
  public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
    try {
      // stack the weight bar on top of TFC food bar
      event.registerAbove(TFC_FOOD_OVERLAY, "weight_bar", WeightBarOverlay::render);
    } catch (IllegalStateException e) {
      event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), "weight_bar", WeightBarOverlay::render);
    }
  }

  private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
    final Minecraft minecraft = Minecraft.getInstance();
    final WeightSyncPacket data = WeightBarClientData.get();

    if (Config.enableWeightCalculations == false || Config.getWeightDisplay() != Config.WeightDisplay.HUD || data == null) {
      return;
    }

    if (minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()
        || !(minecraft.getCameraEntity() instanceof Player)) {
      return;
    }

    gui.setupOverlayRenderState(true, false);

    // The overlay comes after the health, armour and food overlays, so each side's running height already counts its
    // bars: the health side stacks up from the left half of the hotbar, the food side from the right half
    final boolean onHealth = Config.getBarPosition() == Config.BarPosition.HEALTH;
    final int x = onHealth ? width / 2 - 91 : width / 2 + 1;

    final double weightFill = data.maxWeight() <= 0 ? 0 : (double) data.weight() / (double) data.maxWeight();
    drawBar(gui, graphics, minecraft.font, x, height, onHealth, weightFill, false,
        data.weight() + "/" + data.maxWeight(), getTextColor(data.exhausted(), data.overburdened()));

    final CartLoad cart = data.cart();
    if (cart != null && Config.isCartBarShown()) {
      drawBar(gui, graphics, minecraft.font, x, height, onHealth, getCartFill(cart), true,
          Math.round(cart.load()) + "/" + Math.round(cart.overburdenedLoad()),
          getTextColor(cart.isExhausted(), cart.isOverburdened()));
    }
  }

  /**
   * Draws a bar on top of the bars already on that side, and counts it in that side's height for the next one.
   */
  private static void drawBar(ForgeGui gui, GuiGraphics graphics, Font font, int x, int height, boolean onHealth,
      double fill, boolean cartTint, String text, int textColor) {
    // bottom aligned with the 9px row like TFC 5px bars, 1px gap to the bar below
    final int y = height - (onHealth ? gui.leftHeight : gui.rightHeight) - 1;
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

  @Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
  public static class ForgeEvents {
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      // do not show the previous world weight in the next one
      WeightBarClientData.reset();
    }
  }
}
