package com.koenigstag.tfc_weight_bar;

import com.koenigstag.tfc_curios_weight.CuriosHelpers;

import net.minecraft.world.entity.player.Player;

/**
 * Everything that touches TFC Curios Weight. It is optional, so call this only when
 * {@link WeightBarHelpers#isCuriosWeightLoaded()} says it is there: loading this class without it fails.
 */
public final class CuriosWeightCompat {

  private CuriosWeightCompat() {}

  public static int countOverburdened(final Player player) {
    return CuriosHelpers.countOverburdened(player);
  }
}
