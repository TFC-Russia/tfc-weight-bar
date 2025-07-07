package com.koenigstag.tfc_weight_bar;

import com.koenigstag.tfc_curios_weight.CuriosHelpers;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEventHandler {

  private static int showMessageEachTicks = 20 * 10; // 10 sec

  public static void init() {
    final IEventBus bus = MinecraftForge.EVENT_BUS;

    bus.register(ForgeClientEventHandler.class);

    Constants.LOGGER.info("Setup client event handler complete");
  }

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (Config.enableModCalculations == false) {
      return;
    }

    final Player player = event.player;
    final Level level = player.level();

    if (level.getGameTime() % Config.calculateWeightEachNTicks == 0) {
      final int playerInvWeight = WeightBarHelpers.calculatePlayerFullWeight(player);

      final int maxInvWeight = WeightBarHelpers.getMaxInventoryWeight(player);

      final int hugeHeavyCount = CuriosHelpers.countOverburdened(player);

      final double barPercentage = WeightBarHelpers.getBarPercentage(playerInvWeight, player);

      final String barColor = WeightBarHelpers.getBarColor(playerInvWeight, maxInvWeight, hugeHeavyCount);

      // TODO add GUI/HUD bar

      displayCurrentWeightClientMessage(player, level, playerInvWeight, maxInvWeight, hugeHeavyCount, barPercentage,
          barColor);
    }
  }

  @SubscribeEvent
  public static void itemToolTip(ItemTooltipEvent event) {
    ItemStack stack = event.getItemStack();
    int weight = WeightBarHelpers.getItemStackWeightInt(stack);
    int countOverweight = WeightBarHelpers.getItemOverweightInt(stack);

    String weightText = "Вес: " + weight;

    if (countOverweight >= 1) {
      weightText += " (Слишком тяжелый)";
    }

    Component text = Component.literal(weightText);
    event.getToolTip().add(text);
  }

  private static void displayCurrentWeightClientMessage(Player player, Level level, int playerInvWeight,
      int maxInvWeight,
      int hugeHeavyCount, double barPercentage, String barColor) {
    if (Config.enableUIBar == false && level.getGameTime() % showMessageEachTicks == 0) {
      String message = chatMessageText(playerInvWeight, maxInvWeight, barPercentage);

      MutableComponent msgComponent = Component.literal(message);

      msgComponent.withStyle(getTextStyle(barColor));

      player.displayClientMessage(msgComponent, true);

      Constants.debug(message);
    }
  }

  private static String chatMessageText(int playerInvWeight, int maxInvWeight, double barPercentage) {
    String message = "Weight: " + playerInvWeight + "/" + maxInvWeight + " (" + Math.round(barPercentage * 100) + "%)";

    return message;
  }

  private static Style getTextStyle(String barColor) {
    Style style = Style.EMPTY;

    if (barColor.equals("red")) {
      style.withColor(TextColor.parseColor("RED"));
    } else if (barColor.equals("pink")) {
      style.withColor(TextColor.parseColor("LIGHT_PURPLE"));
    } else if (barColor.equals("gray")) {
      style.withColor(TextColor.parseColor("GRAY"));
    } else {
      style.withColor(TextColor.parseColor("RESET"));
    }

    return style;
  }
}
