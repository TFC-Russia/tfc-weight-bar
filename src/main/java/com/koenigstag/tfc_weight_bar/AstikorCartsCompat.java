package com.koenigstag.tfc_weight_bar;

import net.dries007.tfc.common.component.size.IItemSize;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tfcastikorcarts.common.container.DynamicGridContainer;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnEntity;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnInventoryEntity;
import tfcastikorcarts.config.TFCAstikorCartsConfig;
import tfcastikorcarts.util.AstikorHelpers;

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
   * plough or seed drill does not count. With count_cart_riders, also of the cart the player sits on.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final Entity vehicle = player.getVehicle();
    if (Config.countCartRiders && vehicle instanceof TFCAbstractDrawnEntity cart && cart.affectedByCargo()) {
      return getLoad(cart);
    }

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
  // either one a hundredth of its mass in kilograms and never under 1. count_cart_riders adds the players sitting on it,
  // and a cart with an inventory counts its slots that hold large items.
  private static CartLoad getLoad(final TFCAbstractDrawnEntity cart) {
    if (cart instanceof TFCAbstractDrawnInventoryEntity inventoryCart) {
      return new CartLoad(getHundredth(inventoryCart.countOverburdenedCargo() + countRiders(cart, false)),
          getExhaustedLoad(), getOverburdenedLoad(), WeightBarHelpers.countLargeItemSlots(inventoryCart),
          Config.enableCartLimits ? Config.astikorMaxLargeItemSlots : 0);
    }

    return new CartLoad(getHundredth(cart.countOverburdened() + countRiders(cart, true)), getExhaustedLoad(),
        getOverburdenedLoad());
  }

  /**
   * The mass of the players sitting on this cart, as the cart counts its own: each one's body as the mod weighs a
   * passenger, and each stack they carry as it weighs cargo. A cart without an inventory already counts the bodies of
   * its passengers but the one who drives it, so then only the driver's body is added. 0 unless count_cart_riders is on.
   */
  private static double countRiders(final TFCAbstractDrawnEntity cart, final boolean countsPassengerBodies) {
    if (!Config.countCartRiders) {
      return 0;
    }

    double mass = 0;
    for (final Entity passenger : cart.getPassengers()) {
      if (passenger instanceof Player rider) {
        if (!countsPassengerBodies || passenger == cart.getControllingPassenger()) {
          mass += AstikorHelpers.getEstimatedMassKg(rider.getBoundingBox());
        }
        for (final ItemStack stack : WeightBarHelpers.getCarriedStacks(rider)) {
          final IItemSize size = ItemSizeManager.get(stack);
          mass += AstikorHelpers.getEstimatedMassKg(stack, size.getSize(stack), size.getWeight(stack));
        }
      }
    }
    return mass;
  }

  private static float getHundredth(final double mass) {
    return (float) Math.max(mass * 0.01, 1.0);
  }

  private static float getExhaustedLoad() {
    return TFCAstikorCartsConfig.COMMON.exhaustedThreshold.get().floatValue();
  }

  private static float getOverburdenedLoad() {
    return TFCAstikorCartsConfig.COMMON.overburdenedThreshold.get().floatValue();
  }
}
