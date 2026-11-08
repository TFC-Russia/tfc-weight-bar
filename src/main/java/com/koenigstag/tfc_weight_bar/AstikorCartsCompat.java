package com.koenigstag.tfc_weight_bar;

import de.mennomax.astikorcarts.entity.AbstractDrawnEntity;
import de.mennomax.astikorcarts.world.AstikorWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import tfcastikorcarts.common.entities.carts.TFCAnimalCartEntity;
import tfcastikorcarts.common.entities.carts.TFCSupplyCartEntity;
import tfcastikorcarts.config.TFCAstikorCartsConfig;

/**
 * Everything that touches TFC Astikor Carts. It is optional, so call this only through
 * {@link WeightBarHelpers#getCartLoad(Player)}: loading this class without it fails.
 */
public final class AstikorCartsCompat {
  // TFCAnimalCartEntity weighs its passengers against these, written into its code rather than its config
  private static final float ANIMAL_CART_EXHAUSTED_LOAD = 10;
  private static final float ANIMAL_CART_OVERBURDENED_LOAD = 20;

  private AstikorCartsCompat() {}

  /**
   * The load of the cart that burdens this player, or null when none does. TFC Astikor Carts burdens the player who
   * pulls a cart, and the player who rides and steers the animal that pulls it.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final Entity vehicle = player.getVehicle();
    final Entity puller = vehicle != null && vehicle.getControllingPassenger() == player ? vehicle : player;

    final AbstractDrawnEntity cart = AstikorWorld.get(player.level()).resolve()
        .flatMap(world -> world.getDrawn(puller))
        .orElse(null);

    if (cart instanceof TFCSupplyCartEntity supplyCart) {
      return new CartLoad(supplyCart.countOverburdened(),
          TFCAstikorCartsConfig.COMMON.exhaustedThreshold.get().floatValue(),
          TFCAstikorCartsConfig.COMMON.overburdenedThreshold.get().floatValue());
    }

    if (cart instanceof TFCAnimalCartEntity animalCart) {
      return new CartLoad(animalCart.countOverburdened(), ANIMAL_CART_EXHAUSTED_LOAD, ANIMAL_CART_OVERBURDENED_LOAD);
    }

    // A plough, or a cart of another mod, carries no load
    return null;
  }
}
