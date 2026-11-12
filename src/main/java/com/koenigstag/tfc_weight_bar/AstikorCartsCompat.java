package com.koenigstag.tfc_weight_bar;

import net.dries007.tfc.common.component.size.IItemSize;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tfcastikorcarts.common.container.DynamicGridContainer;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnEntity;
import tfcastikorcarts.common.entities.carts.TFCAbstractDrawnInventoryEntity;
import tfcastikorcarts.common.entities.carts.TFCHandCartEntity;
import tfcastikorcarts.config.TFCAstikorCartsConfig;
import tfcastikorcarts.util.AstikorHelpers;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything that touches TFC Astikor Carts. It is optional, so call this only through {@link WeightBarHelpers}: loading
 * this class without it fails.
 *
 * <p>Under enable_cart_limits a cart with an inventory, the supply cart or the hand cart, is weighed as a player's
 * inventory, each against a limit of its own, and burdens by that alone: TFCAbstractDrawnInventoryEntityMixin keeps the
 * cart's own sum of its cargo from burdening anyone. The animal cart, which carries animals, keeps that mod's scale.
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
   * Burdens a player who pulls an overloaded cart with an inventory on foot, with the effects of a load they carry. The
   * cart's own debuffs for its cargo are off while this mod weighs it, so these take their place.
   */
  public static void burden(final Player player) {
    if (player.getVehicle() != null) {
      return;
    }

    for (final TFCAbstractDrawnInventoryEntity cart : player.level().getEntitiesOfClass(
        TFCAbstractDrawnInventoryEntity.class, player.getBoundingBox().inflate(CART_SEARCH_RADIUS),
        drawn -> drawn.getPulling() == player && drawn.affectedByCargo())) {
      final CartLoad load = getLoad(cart);

      if (load.isOverburdened()) {
        player.addEffect(WeightBarHelpers.getOverburdened(false));
      } else if (load.isExhausted()) {
        player.addEffect(WeightBarHelpers.getExhausted(false));
      }
      return;
    }
  }

  /**
   * Hands each animal near this player that pulls a cart loaded past its limit to WeightBarHelpers.onCartOverloaded,
   * which makes it let go of the cart or stand still with it: a cart with an inventory past its astikor_*_max_load, the
   * animal cart past what TFC Astikor Carts lets a player pull. That mod has no limit of its own for an animal.
   */
  public static void checkOverloadedCarts(final Player player) {
    for (final TFCAbstractDrawnEntity cart : player.level().getEntitiesOfClass(TFCAbstractDrawnEntity.class,
        player.getBoundingBox().inflate(CART_SEARCH_RADIUS),
        drawn -> drawn.getPulling() instanceof LivingEntity && !(drawn.getPulling() instanceof Player)
            && drawn.affectedByCargo())) {
      final CartLoad load = getLoad(cart);

      if (load.isOverburdened()) {
        WeightBarHelpers.onCartOverloaded(cart.getPulling(), cart, load, isBeingLoaded(cart),
            () -> cart.setPulling(null));
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

  // A cart with an inventory under enable_cart_limits: what it holds weighed as a player's inventory, containers with
  // what they hold, its riders' own weight when count_cart_riders adds them, and its animals' packs when
  // count_animal_inventories does, against its limit; and its slots that hold large items. Otherwise the same sums the
  // cart weighs against its thresholds: the cargo of a cart with an inventory, or else its passengers, either one a
  // hundredth of its mass in kilograms and never under 1, with the riders and the packs added as cargo.
  private static CartLoad getLoad(final TFCAbstractDrawnEntity cart) {
    if (cart instanceof TFCAbstractDrawnInventoryEntity inventoryCart) {
      if (Config.enableCartLimits) {
        final int maxLoad = getMaxLoad(inventoryCart);

        return new CartLoad(
            WeightBarHelpers.calculateContainerWeight(inventoryCart, cart.level().registryAccess()) + weighRiders(cart)
                + weighAnimalPacks(cart),
            (float) (maxLoad * Config.getExhaustedWeightCoefficient()), maxLoad,
            WeightBarHelpers.countLargeItemSlots(inventoryCart), Config.astikorMaxLargeItemSlots);
      }

      return new CartLoad(
          getHundredth(inventoryCart.countOverburdenedCargo() + countRiders(cart, false) + countAnimalPacks(cart)),
          getExhaustedLoad(), getOverburdenedLoad());
    }

    return new CartLoad(getHundredth(cart.countOverburdened() + countRiders(cart, true) + countAnimalPacks(cart)),
        getExhaustedLoad(), getOverburdenedLoad());
  }

  /**
   * What the animal pulling this cart and the animals riding in it carry in inventories of their own, weighed as a
   * player's inventory: an animal hauls its pack along with the cart. 0 unless count_animal_inventories is on.
   */
  private static int weighAnimalPacks(final TFCAbstractDrawnEntity cart) {
    int weight = 0;
    for (final Container pack : getAnimalPacks(cart)) {
      weight += WeightBarHelpers.calculateContainerWeight(pack, cart.level().registryAccess());
    }
    return weight;
  }

  /**
   * The mass of the same packs as weighAnimalPacks, each stack weighed as the cart weighs cargo.
   */
  private static double countAnimalPacks(final TFCAbstractDrawnEntity cart) {
    double mass = 0;
    for (final Container pack : getAnimalPacks(cart)) {
      for (int i = 0; i < pack.getContainerSize(); i++) {
        final ItemStack stack = pack.getItem(i);
        if (!stack.isEmpty()) {
          final IItemSize size = ItemSizeManager.get(stack);
          mass += AstikorHelpers.getEstimatedMassKg(stack, size.getSize(stack), size.getWeight(stack));
        }
      }
    }
    return mass;
  }

  private static List<Container> getAnimalPacks(final TFCAbstractDrawnEntity cart) {
    final List<Container> packs = new ArrayList<>();
    final Container pullerPack = WeightBarHelpers.getAnimalInventory(cart.getPulling());
    if (pullerPack != null) {
      packs.add(pullerPack);
    }
    for (final Entity passenger : cart.getPassengers()) {
      final Container pack = WeightBarHelpers.getAnimalInventory(passenger);
      if (pack != null) {
        packs.add(pack);
      }
    }
    return packs;
  }

  // The hand cart has a limit of its own; the supply cart's covers it and any other cart with cargo
  private static int getMaxLoad(final TFCAbstractDrawnInventoryEntity cart) {
    return cart instanceof TFCHandCartEntity ? Config.astikorHandCartMaxLoad : Config.astikorSupplyCartMaxLoad;
  }

  /**
   * The weight the players sitting on this cart carry, as their own bars show it. 0 unless count_cart_riders is on.
   */
  private static int weighRiders(final Entity cart) {
    if (!Config.countCartRiders) {
      return 0;
    }

    int weight = 0;
    for (final Entity passenger : cart.getPassengers()) {
      if (passenger instanceof Player rider) {
        weight += WeightBarHelpers.calculatePlayerFullWeight(rider);
      }
    }
    return weight;
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
