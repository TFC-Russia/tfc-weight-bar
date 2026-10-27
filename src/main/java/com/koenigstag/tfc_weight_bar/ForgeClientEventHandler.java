package com.koenigstag.tfc_weight_bar;

import com.koenigstag.tfc_curios_weight.CuriosHelpers;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventHandler {

  private static int showMessageEachTicks = 20 * 10; // 10 sec

  public static void init() {
    final IEventBus bus = NeoForge.EVENT_BUS;

    bus.register(ForgeClientEventHandler.class);

    Constants.LOGGER.info("Setup client event handler complete");
  }

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    // PlayerTickEvent.Post fires once, after the tick
    if (Config.enableWeightCalculations == false) {
      return;
    }

    final Player player = event.getEntity();
    final Level level = player.level();

    if (level.getGameTime() % Config.calculateWeightEachNTicks == 0) {
      final int playerInvWeight = WeightBarHelpers.calculatePlayerFullWeight(player);

      final int maxInvWeight = WeightBarHelpers.getMaxInventoryWeight(player);

      final int hugeHeavyCount = CuriosHelpers.countOverburdened(player);

      final double barPercentage = WeightBarHelpers.getBarPercentage(playerInvWeight, player);

      final String barColor = WeightBarHelpers.getBarColor(playerInvWeight, maxInvWeight, hugeHeavyCount);

      displayCurrentWeightClientMessage(player, level, playerInvWeight, maxInvWeight, hugeHeavyCount, barPercentage,
          barColor);
    }
  }

  @SubscribeEvent
  public static void itemToolTip(ItemTooltipEvent event) {
    final ItemStack stack = event.getItemStack();
    final int weight = WeightBarHelpers.getItemStackWeightInt(stack);
    final int countOverweight = WeightBarHelpers.getItemOverweightInt(stack);

    final Component tooHeavyText = countOverweight >= 1 ? translateComponent(Constants.TooHeavyLangKey) : Component.empty();

    final Component tooltipComponent = formatComponent(Constants.WeightTooltipFormatLangKey, weight, tooHeavyText);
    event.getToolTip().add(tooltipComponent);
  }

  private static void displayCurrentWeightClientMessage(Player player, Level level, int playerInvWeight,
      int maxInvWeight,
      int hugeHeavyCount, double barPercentage, String barColor) {
    if (Config.enableUIBar == false && level.getGameTime() % showMessageEachTicks == 0) {
      Component message = createWeightMessage(playerInvWeight, maxInvWeight, barPercentage, hugeHeavyCount);

      MutableComponent msgComponent = message.copy();
      msgComponent.withStyle(getTextStyle(barColor));

      player.displayClientMessage(msgComponent, true);

      Constants.debug(message.getString());
    }
  }

  private static Component createWeightMessage(int playerInvWeight, int maxInvWeight, double barPercentage, int hugeHeavyCount) {
    boolean isOverburdened = WeightBarHelpers.getIsOverburdened(playerInvWeight, maxInvWeight, hugeHeavyCount);

    if (isOverburdened) {
      return translateComponent(Constants.WeightStatusOverburdenedLangKey);
    }

    return formatComponent(Constants.WeightMessageFormatLangKey,
        playerInvWeight,
        maxInvWeight,
        Math.round(barPercentage * 100));
  }

  private static Style getTextStyle(String barColor) {
    // Style is immutable: withColor returns a new instance
    Style style = Style.EMPTY;

    if (barColor.equals("red")) {
      style = style.withColor(ChatFormatting.RED);
    } else if (barColor.equals("pink")) {
      style = style.withColor(ChatFormatting.LIGHT_PURPLE);
    } else if (barColor.equals("gray")) {
      style = style.withColor(ChatFormatting.GRAY);
    }

    return style;
  }

  // Utility methods for localization
  private static Component translateComponent(String key) {
    return Component.translatable(key);
  }

  private static Component formatComponent(String key, Object... args) {
    return Component.translatable(key, args);
  }
}
