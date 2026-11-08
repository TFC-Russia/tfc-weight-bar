package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import tfcastikorcarts.common.container.DynamicGridContainer;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnEntity;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnInventoryEntity;
import tfcastikorcarts.config.TFCAstikorCartsConfig;

import javax.annotation.Nullable;

/**
 * Everything that touches TFC Astikor Carts. It is optional, so call this only through {@link WeightBarHelpers}: loading
 * this class without it fails.
 */
public final class AstikorCartsCompat {
  // A pulled cart stays within a few blocks of whoever pulls it
  private static final double CART_SEARCH_RADIUS = 8;


  private AstikorCartsCompat() {}

  /**
   * The load of the cart this player pulls on foot, or that the animal they ride pulls, or null when there is none. A
   * plough or seed drill does not count.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final Entity vehicle = player.getVehicle();
    final Entity puller = vehicle != null ? vehicle : player;

    return player.level().getEntitiesOfClass(TFCAbstractDrawnEntity.class,
            player.getBoundingBox().inflate(CART_SEARCH_RADIUS),
            drawn -> drawn.getPulling() == puller && drawn.affectedByCargo())
        .stream().findFirst().map(AstikorCartsCompat::getLoad).orElse(null);
  }

  /**
   * Makes each animal near this player let go of a cart loaded past what TFC Astikor Carts lets a player pull. That mod
   * has no limit of its own for an animal. A player pulling on foot stays with that mod's debuffs.
   */
  public static void refuseOverloadedCarts(final Player player) {
    for (final TFCAbstractDrawnEntity cart : player.level().getEntitiesOfClass(TFCAbstractDrawnEntity.class,
        player.getBoundingBox().inflate(CART_SEARCH_RADIUS),
        drawn -> drawn.getPulling() instanceof LivingEntity && !(drawn.getPulling() instanceof Player)
            && drawn.affectedByCargo())) {
      final CartLoad load = getLoad(cart);

      if (load.isOverburdened() && !isBeingLoaded(cart)) {
        refuse(cart, load);
      }
    }
  }

  // The cart's inventory hides most of the message over the hotbar, so an animal with no rider waits until the player
  // loading the cart closes it, and they read it whole
  private static boolean isBeingLoaded(final TFCAbstractDrawnEntity cart) {
    return cart.getPulling().getPassengers().isEmpty()
        && !cart.level().getEntitiesOfClass(Player.class, cart.getBoundingBox().inflate(CART_SEARCH_RADIUS),
            near -> near.containerMenu instanceof DynamicGridContainer menu && menu.container == cart).isEmpty();
  }

  private static void refuse(final TFCAbstractDrawnEntity cart, final CartLoad load) {
    final Entity puller = cart.getPulling();

    cart.setPulling(null);

    WeightBarHelpers.announceCartRefused(puller, cart, load);
  }

  // The same sums the cart weighs against its thresholds: the cargo of a cart with an inventory, or else its passengers,
  // either one a hundredth of its mass in kilograms and never under 1
  private static CartLoad getLoad(final TFCAbstractDrawnEntity cart) {
    final double mass = cart instanceof TFCAbstractDrawnInventoryEntity inventoryCart
        ? inventoryCart.countOverburdenedCargo()
        : cart.countOverburdened();

    return new CartLoad((float) Math.max(mass * 0.01, 1.0),
        TFCAstikorCartsConfig.COMMON.exhaustedThreshold.get().floatValue(),
        TFCAstikorCartsConfig.COMMON.overburdenedThreshold.get().floatValue());
  }
}
