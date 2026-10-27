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

// HUD weight bar, the thick bar from the TFCSU (TFC Russia) 1.7.10 HUD
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class WeightBarOverlay {
  // texture rows (each 90x10): background, fill
  private static final ResourceLocation TEXTURE = new ResourceLocation(Constants.MOD_ID, "textures/gui/weight_bar.png");
  private static final int TEXTURE_WIDTH = 128;
  private static final int TEXTURE_HEIGHT = 32;

  private static final int BAR_WIDTH = 90;
  private static final int BAR_HEIGHT = 10;
  private static final int FILL_V = 10;

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

    if (Config.enableWeightCalculations == false || Config.enableUIBar == false || data == null) {
      return;
    }

    if (minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()
        || !(minecraft.getCameraEntity() instanceof Player)) {
      return;
    }

    gui.setupOverlayRenderState(true, false);

    // bottom aligned with the 9px row like TFC 5px bars, 1px gap to the bar below
    final int x = width / 2 + 1;
    final int y = height - gui.rightHeight - 1;

    final double barPercentage = data.maxWeight() <= 0 ? 0
        : Mth.clamp((double) data.weight() / (double) data.maxWeight(), 0, 1);
    final int fillWidth = (int) (BAR_WIDTH * barPercentage);

    graphics.blit(TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    graphics.blit(TEXTURE, x, y, 0, FILL_V, fillWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

    drawWeightText(graphics, minecraft.font, data, x, y);

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

  @Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
  public static class ForgeEvents {
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      // do not show the previous world weight in the next one
      WeightBarClientData.reset();
    }
  }
}
