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

// HUD weight bar, the thick bar from the TFCSU (TFC Russia) 1.7.10 HUD
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

  private static final int NORMAL_TEXT_COLOR = 0xFFFFFF;
  private static final int EXHAUSTED_TEXT_COLOR = 0xFFFF00;
  private static final int OVERBURDENED_TEXT_COLOR = 0xFF0000;

  // TFC registers its food and thirst bars above the vanilla food level
  private static final ResourceLocation TFC_FOOD_OVERLAY = ResourceLocation.fromNamespaceAndPath("tfc", "food");

  /**
   * 1.21 replaced the Forge overlay list with the vanilla layered draw: a layer is registered by name and is handed
   * only the graphics and the frame timer. The running bar offset survived the move, it just sits on the vanilla Gui
   * now instead of on ForgeGui, and TFC stacks its own food and thirst bars with it the same way.
   */
  @SubscribeEvent
  public static void onRegisterLayers(RegisterGuiLayersEvent event) {
    try {
      // stack the weight bar on top of the TFC food bar
      event.registerAbove(TFC_FOOD_OVERLAY, LAYER_ID, WeightBarOverlay::render);
    } catch (IllegalArgumentException | IllegalStateException e) {
      Constants.LOGGER.info("No tfc food layer to stack on, falling back to the vanilla food level");
      event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, LAYER_ID, WeightBarOverlay::render);
    }
  }

  private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
    final Minecraft minecraft = Minecraft.getInstance();
    final WeightSyncPacket data = WeightBarClientData.get();

    if (Config.enableWeightCalculations == false || Config.enableUIBar == false || data == null) {
      return;
    }

    // What ForgeGui.shouldDrawSurvivalElements used to check: no bars in creative or spectator
    if (minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()
        || !(minecraft.getCameraEntity() instanceof Player)) {
      return;
    }

    final Gui gui = minecraft.gui;

    // bottom aligned with the row the bars below ended on, 1px gap to the bar below
    final int x = graphics.guiWidth() / 2 + 1;
    final int y = graphics.guiHeight() - gui.rightHeight - 1;

    final double barPercentage = data.maxWeight() <= 0 ? 0
        : Mth.clamp((double) data.weight() / (double) data.maxWeight(), 0, 1);
    final int fillWidth = (int) (BAR_WIDTH * barPercentage);

    graphics.blit(TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    graphics.blit(TEXTURE, x, y, 0, FILL_V, fillWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

    drawWeightText(graphics, minecraft.font, data, x, y);

    // leave the next bar room above this one
    gui.rightHeight += BAR_HEIGHT + 1;
  }

  private static void drawWeightText(GuiGraphics graphics, Font font, WeightSyncPacket data, int x, int y) {
    final String text = data.weight() + "/" + data.maxWeight();

    int color = NORMAL_TEXT_COLOR;

    if (data.overburdened()) {
      color = OVERBURDENED_TEXT_COLOR;
    } else if (data.exhausted()) {
      color = EXHAUSTED_TEXT_COLOR;
    }

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
