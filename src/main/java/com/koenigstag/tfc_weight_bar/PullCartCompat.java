package com.koenigstag.tfc_weight_bar;

import java.lang.reflect.Field;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Pull Cart's carts, which limit nothing but anvils themselves. Here a cart's load is the weight of what its chest holds,
 * counted as in a player's inventory, up to pull_cart_max_load. On foot that burdens the player, and an animal they ride
 * refuses the cart, as with TFC Astikor Carts.
 *
 * <p>Pull Cart keeps who pulls a cart and its chest private and this mod is not built against it, so they are reached by
 * name, only through {@link WeightBarHelpers}. A version that renames them costs these features, not the server.
 */
public final class PullCartCompat {
  private static final Class<? extends Entity> CART;
  private static final Field PULLER;
  private static final Field CHEST;

  // As far as Pull Cart itself looks for a player's cart, and more than a cart can fall behind by default
  private static final double CART_SEARCH_RADIUS = 32;

  static {
    try {
      CART = Class.forName("com.koenigstag.pull_cart.CartEntity").asSubclass(Entity.class);
      PULLER = CART.getDeclaredField("puller");
      PULLER.setAccessible(true);
      CHEST = CART.getDeclaredField("chest");
      CHEST.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private PullCartCompat() {}

  /**
   * The load of the cart this player pulls, by themselves or with the animal they ride, or null when there is none.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final Entity cart = findCart(player);

    return cart == null ? null : getLoad(cart);
  }

  /**
   * Burdens a player who pulls an overloaded cart on foot, with the effects of a load they carry.
   */
  public static void burden(final Player player) {
    if (player.getVehicle() != null) {
      return;
    }

    final CartLoad load = getCartLoad(player);

    if (load != null && load.isOverburdened()) {
      player.addEffect(WeightBarHelpers.getOverburdened(false));
    } else if (load != null && load.isExhausted()) {
      player.addEffect(WeightBarHelpers.getExhausted(false));
    }
  }

  /**
   * Makes the animal this player rides let go of an overloaded cart.
   */
  public static void refuseOverloadedCart(final Player player) {
    if (player.getVehicle() == null) {
      return;
    }

    final Entity cart = findCart(player);

    if (cart != null) {
      final CartLoad load = getLoad(cart);

      if (load.isOverburdened()) {
        setPuller(cart, null);
        WeightBarHelpers.announceCartRefused(player.getRootVehicle(), cart, load);
      }
    }
  }

  @Nullable
  private static Entity findCart(final Player player) {
    return player.level().getEntitiesOfClass(CART, player.getBoundingBox().inflate(CART_SEARCH_RADIUS),
            cart -> getPuller(cart) == player)
        .stream().findFirst().orElse(null);
  }

  private static CartLoad getLoad(final Entity cart) {
    final int maxLoad = Config.pullCartMaxLoad;
    final Container chest;

    try {
      chest = (Container) CHEST.get(cart);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot read the chest of a Pull Cart cart", e);
    }

    return new CartLoad(WeightBarHelpers.calculateContainerWeight(chest),
        (float) (maxLoad * Config.getExhaustedWeightCoefficient()), maxLoad);
  }

  @Nullable
  private static Object getPuller(final Entity cart) {
    try {
      return PULLER.get(cart);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot read who pulls a Pull Cart cart", e);
    }
  }

  private static void setPuller(final Entity cart, @Nullable final Player puller) {
    try {
      PULLER.set(cart, puller);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot let go of a Pull Cart cart", e);
    }
  }
}
