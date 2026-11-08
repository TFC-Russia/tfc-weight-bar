package com.koenigstag.tfc_weight_bar;

import java.lang.reflect.Method;

import de.mennomax.astikorcarts.entity.AbstractDrawnEntity;
import de.mennomax.astikorcarts.world.AstikorWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import tfcastikorcarts.common.container.SupplyCartContainer;
import tfcastikorcarts.common.entities.carts.TFCAnimalCartEntity;
import tfcastikorcarts.common.entities.carts.TFCSupplyCartEntity;
import tfcastikorcarts.config.TFCAstikorCartsConfig;

/**
 * Everything that touches TFC Astikor Carts. It is optional, so call this only through {@link WeightBarHelpers}: loading
 * this class without it fails.
 *
 * <p>Two ways of weighing a cart are around. Since 1.1.9 the cart weighs its cargo as a mass, every item of a stack
 * counted, and compares a hundredth of it with the thresholds in its config. Up to 1.1.8 a slot weighed the same whether
 * it held one item or a full stack. This is built against 1.1.9 and reaches the old counting method by name, since 1.1.9
 * removed it.
 */
public final class AstikorCartsCompat {
  // Up to 1.1.8 the animal cart weighs its passengers against these, written into its code rather than its config
  private static final float LEGACY_ANIMAL_CART_EXHAUSTED_LOAD = 10;
  private static final float LEGACY_ANIMAL_CART_OVERBURDENED_LOAD = 20;

  // A pulled cart stays within a few blocks of whoever pulls it
  private static final double CART_SEARCH_RADIUS = 8;


  // The supply cart's counting method up to 1.1.8, or null on 1.1.9 and later
  @Nullable
  private static final Method LEGACY_SUPPLY_CART_LOAD = findLegacySupplyCartLoad();

  private AstikorCartsCompat() {}

  @Nullable
  private static Method findLegacySupplyCartLoad() {
    try {
      TFCSupplyCartEntity.class.getMethod("countOverburdenedCargo");
      return null;
    } catch (NoSuchMethodException newMethodMissing) {
      try {
        return TFCSupplyCartEntity.class.getMethod("countOverburdened");
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException("TFC Astikor Carts weighs its supply cart in no way this mod knows", e);
      }
    }
  }

  /**
   * The load of the cart this player pulls on foot, or that the animal they ride pulls, or null when there is none. A
   * plough does not count.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final Entity vehicle = player.getVehicle();
    final Entity puller = vehicle != null ? vehicle : player;

    return AstikorWorld.get(player.level()).resolve()
        .flatMap(world -> world.getDrawn(puller))
        .map(AstikorCartsCompat::getLoad)
        .orElse(null);
  }

  /**
   * Makes each animal near this player let go of a cart loaded past what TFC Astikor Carts lets a player pull. Since
   * 1.1.9 that mod has no limit of its own for an animal, and up to 1.1.8 it burdens the rider, whom a TFC horse then
   * kicks off, with the cart still hitched. A player pulling on foot stays with that mod's debuffs.
   */
  public static void refuseOverloadedCarts(final Player player) {
    for (final AbstractDrawnEntity cart : player.level().getEntitiesOfClass(AbstractDrawnEntity.class,
        player.getBoundingBox().inflate(CART_SEARCH_RADIUS),
        drawn -> drawn.getPulling() instanceof LivingEntity && !(drawn.getPulling() instanceof Player))) {
      final CartLoad load = getLoad(cart);

      if (load != null && load.isOverburdened() && !isBeingLoaded(cart)) {
        refuse(cart, load);
      }
    }
  }

  // The cart's inventory hides most of the message over the hotbar, so an animal with no rider waits until the player
  // loading the cart closes it, and they read it whole
  private static boolean isBeingLoaded(final AbstractDrawnEntity cart) {
    return cart.getPulling().getPassengers().isEmpty()
        && !cart.level().getEntitiesOfClass(Player.class, cart.getBoundingBox().inflate(CART_SEARCH_RADIUS),
            near -> near.containerMenu instanceof SupplyCartContainer menu && menu.cart == cart).isEmpty();
  }

  private static void refuse(final AbstractDrawnEntity cart, final CartLoad load) {
    final Entity puller = cart.getPulling();

    cart.setPulling(null);

    WeightBarHelpers.announceCartRefused(puller, cart, load);
  }

  // The same sums the cart weighs against its thresholds, or null for a cart with no load, such as a plough
  @Nullable
  private static CartLoad getLoad(final AbstractDrawnEntity cart) {
    final boolean legacy = LEGACY_SUPPLY_CART_LOAD != null;

    if (cart instanceof TFCSupplyCartEntity supplyCart) {
      return new CartLoad(legacy ? getLegacyLoad(supplyCart) : getHundredth(supplyCart.countOverburdenedCargo()),
          getExhaustedLoad(), getOverburdenedLoad());
    }

    if (cart instanceof TFCAnimalCartEntity animalCart) {
      return legacy
          ? new CartLoad(animalCart.countOverburdened(), LEGACY_ANIMAL_CART_EXHAUSTED_LOAD,
              LEGACY_ANIMAL_CART_OVERBURDENED_LOAD)
          : new CartLoad(getHundredth(animalCart.countOverburdened()), getExhaustedLoad(), getOverburdenedLoad());
    }

    return null;
  }

  // Since 1.1.9 a cart compares a hundredth of what it counts with its thresholds, and never less than 1
  private static float getHundredth(final double count) {
    return (float) Math.max(count * 0.01, 1.0);
  }

  private static float getLegacyLoad(final TFCSupplyCartEntity supplyCart) {
    try {
      return (float) LEGACY_SUPPLY_CART_LOAD.invoke(supplyCart);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Cannot count the load of a TFC Astikor Carts supply cart", e);
    }
  }

  private static float getExhaustedLoad() {
    return TFCAstikorCartsConfig.COMMON.exhaustedThreshold.get().floatValue();
  }

  private static float getOverburdenedLoad() {
    return TFCAstikorCartsConfig.COMMON.overburdenedThreshold.get().floatValue();
  }
}
