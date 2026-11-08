package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.entity.player.Player;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnEntity;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnInventoryEntity;
import tfcastikorcarts.config.TFCAstikorCartsConfig;

import javax.annotation.Nullable;

/**
 * Everything that touches TFC Astikor Carts. It is optional, so call this only through
 * {@link WeightBarHelpers#getCartLoad(Player)}: loading this class without it fails.
 */
public final class AstikorCartsCompat {
  // A pulled cart stays within a few blocks of whoever pulls it
  private static final double CART_SEARCH_RADIUS = 8;

  private AstikorCartsCompat() {}

  /**
   * The load of the cart that burdens this player, or null when none does. On 1.21.1 TFC Astikor Carts burdens only a
   * player who pulls a cart on foot, not one who rides the animal that pulls it, and not with a plough or seed drill.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final TFCAbstractDrawnEntity cart = player.level().getEntitiesOfClass(TFCAbstractDrawnEntity.class,
            player.getBoundingBox().inflate(CART_SEARCH_RADIUS),
            drawn -> drawn.getPulling() == player && drawn.affectedByCargo())
        .stream().findFirst().orElse(null);

    if (cart == null) {
      return null;
    }

    // The same sums the cart weighs against its thresholds: the cargo of a cart with an inventory, or else its
    // passengers, either one a hundredth of its mass in kilograms and never under 1
    final double mass = cart instanceof TFCAbstractDrawnInventoryEntity inventoryCart
        ? inventoryCart.countOverburdenedCargo()
        : cart.countOverburdened();

    return new CartLoad((float) Math.max(mass * 0.01, 1.0),
        TFCAstikorCartsConfig.COMMON.exhaustedThreshold.get().floatValue(),
        TFCAstikorCartsConfig.COMMON.overburdenedThreshold.get().floatValue());
  }
}
