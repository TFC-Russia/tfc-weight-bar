package com.koenigstag.tfc_weight_bar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

import com.koenigstag.tfc_curios_weight.CuriosHelpers;

import java.util.HashMap;

public class WeightBarData {
  public static Map<String, Integer> playerWeights = new HashMap<String, Integer>();

  // server side
  public static int serverGetCurrentWeight(Player player) {
    if (playerWeights.containsKey(player.getUUID().toString())) {
      return playerWeights.get(player.getUUID().toString());
    }

    return 0;
  }

  // server side
  public static void serverChangeCurrentWeight(Player player, int newWeight) {
    playerWeights.put(player.getUUID().toString(), newWeight);

    triggerServerSideEffects(player, newWeight, WeightBarHelpers.getMaxInventoryWeight(player));
  }

  // server side
  // triggered on player tick event
  public static void triggerServerSideEffects(Player player, int playerInvWeight, int maxInvWeight) {

    Constants.debug("TriggerServerSideEffects");

    final int hugeHeavyCount = CuriosHelpers.countOverburdened(player);

    boolean isOverburdened = WeightBarHelpers.getIsOverburdened(playerInvWeight, maxInvWeight, hugeHeavyCount);

    if (isOverburdened) {
      Constants.debug("overburdened effects");

      // since player cannot move when overburdened - do need to add hunger to player
      // if (Config.enableFoodExhaustion) {
      // addPlayerFoodExhaustion(player, playerInvWeight, maxInvWeight);
      // }

      if (Config.enableWeightDebuffs) {
        player.addEffect(WeightBarHelpers.getOverburdened(false));
      }
    }

    boolean isExhausted = WeightBarHelpers.getIsExhausted(playerInvWeight, maxInvWeight, hugeHeavyCount);

    // condition to not run twice
    if (!isOverburdened && isExhausted) {
      Constants.debug("exhausted effects");

      // Add exhaustion to food data
      if (Config.enableFoodExhaustion) {
        serverAddPlayerFoodExhaustion(player, playerInvWeight, maxInvWeight);
      }

      if (Config.enableWeightDebuffs) {
        player.addEffect(WeightBarHelpers.getExhausted(false));
      }
    }

    // send current state to the client HUD bar
    if (player instanceof ServerPlayer serverPlayer) {
      WeightBarNetwork.sendToPlayer(serverPlayer,
          new WeightSyncPacket(playerInvWeight, maxInvWeight, isExhausted, isOverburdened));
    }

    Constants.debug("Nickname: " + player.getName() + "; Current Weight: " + playerInvWeight + "; Max Weight: "
        + maxInvWeight + "; Is Exhausted: "
        + isExhausted + "; Is Overburdened: " + isOverburdened + "; Bar color: "
        + WeightBarHelpers.getBarColor(playerInvWeight, maxInvWeight, hugeHeavyCount) + "; Bar percentage: "
        + WeightBarHelpers.getBarPercentage(playerInvWeight, player));
  }

  // server side
  private static void serverAddPlayerFoodExhaustion(Player player, int playerInvWeight, int maxInvWeight) {
    final int overburdened = playerInvWeight - maxInvWeight;
    final int overburdenedPercent = (int) Mth.clamp(overburdened / (float) maxInvWeight * 100, 0, 100);

    if (overburdenedPercent > 0) {
      player.getFoodData().addExhaustion(overburdenedPercent * 0.01f);
    } else {
      player.getFoodData().setExhaustion(0.01f);
    }
  }
}
