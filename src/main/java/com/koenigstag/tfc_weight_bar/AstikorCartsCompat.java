package com.koenigstag.tfc_weight_bar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.dries007.tfc.common.capabilities.size.IItemSize;
import net.dries007.tfc.common.capabilities.size.ItemSizeManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import tfcastikorcarts.common.container.SupplyCartContainer;
import tfcastikorcarts.common.entities.carts.TFCAnimalCartEntity;
import tfcastikorcarts.common.entities.carts.TFCSupplyCartEntity;
import tfcastikorcarts.config.TFCAstikorCartsConfig;
import tfcastikorcarts.util.AstikorHelpers;

/**
 * Everything that touches TFC Astikor Carts. It is optional, so call this only through {@link WeightBarHelpers}: loading
 * this class without it fails.
 *
 * <p>Three versions of that mod are around, and this reads them all. Since 1.1.9 the cart weighs its cargo as a mass, every
 * item of a stack counted, and compares a hundredth of it with the thresholds in its config. Up to 1.1.8 a slot weighed
 * the same whether it held one item or a full stack. The fork for AstikorCarts Redux 1.2 weighs as 1.1.9 does, but its
 * carts extend Redux's classes, in another package. So this names only the bridge's own cart classes, never their base
 * mod's: it is built against 1.1.9 and the rest resolves by name at runtime.
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

  // Who a cart's inventory screen belongs to; the field's type is the base mod's, so it is reached by name
  private static final Field MENU_CART = findMenuCart();

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

  private static Field findMenuCart() {
    try {
      return SupplyCartContainer.class.getField("cart");
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException("The TFC Astikor Carts inventory screen has no cart this mod knows", e);
    }
  }

  /**
   * The load of the cart this player pulls on foot, or that the animal they ride pulls, or null when there is none. A
   * plough does not count. With count_cart_riders, also of the cart the player sits on.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final Entity vehicle = player.getVehicle();
    if (Config.countCartRiders && vehicle instanceof TFCSupplyCartEntity supplyCart) {
      return getLoad(supplyCart);
    }
    if (Config.countCartRiders && vehicle instanceof TFCAnimalCartEntity animalCart) {
      return getLoad(animalCart);
    }

    final Entity puller = vehicle != null ? vehicle : player;
    final AABB area = player.getBoundingBox().inflate(CART_SEARCH_RADIUS);

    for (final TFCSupplyCartEntity cart : player.level().getEntitiesOfClass(TFCSupplyCartEntity.class, area,
        drawn -> drawn.getPulling() == puller)) {
      return getLoad(cart);
    }

    for (final TFCAnimalCartEntity cart : player.level().getEntitiesOfClass(TFCAnimalCartEntity.class, area,
        drawn -> drawn.getPulling() == puller)) {
      return getLoad(cart);
    }

    return null;
  }

  /**
   * Makes each animal near this player let go of a cart loaded past what TFC Astikor Carts lets a player pull. Since
   * 1.1.9 that mod has no limit of its own for an animal, and up to 1.1.8 it burdens the rider, whom a TFC horse then
   * kicks off, with the cart still hitched. A player pulling on foot stays with that mod's debuffs.
   */
  public static void refuseOverloadedCarts(final Player player) {
    final AABB area = player.getBoundingBox().inflate(CART_SEARCH_RADIUS);

    for (final TFCSupplyCartEntity cart : player.level().getEntitiesOfClass(TFCSupplyCartEntity.class, area,
        drawn -> isAnimal(drawn.getPulling()))) {
      final CartLoad load = getLoad(cart);
      final Entity puller = cart.getPulling();

      if (load.isOverburdened() && !isBeingLoaded(cart, puller)) {
        cart.setPulling(null);
        WeightBarHelpers.announceCartRefused(puller, cart, load);
      }
    }

    for (final TFCAnimalCartEntity cart : player.level().getEntitiesOfClass(TFCAnimalCartEntity.class, area,
        drawn -> isAnimal(drawn.getPulling()))) {
      final CartLoad load = getLoad(cart);
      final Entity puller = cart.getPulling();

      if (load.isOverburdened()) {
        cart.setPulling(null);
        WeightBarHelpers.announceCartRefused(puller, cart, load);
      }
    }
  }

  private static boolean isAnimal(@Nullable final Entity puller) {
    return puller instanceof LivingEntity && !(puller instanceof Player);
  }

  // The cart's inventory hides most of the message over the hotbar, so an animal with no rider waits until the player
  // loading the cart closes it, and they read it whole
  private static boolean isBeingLoaded(final TFCSupplyCartEntity cart, final Entity puller) {
    return puller.getPassengers().isEmpty()
        && !cart.level().getEntitiesOfClass(Player.class, cart.getBoundingBox().inflate(CART_SEARCH_RADIUS),
            near -> isMenuOf(near, cart)).isEmpty();
  }

  private static boolean isMenuOf(final Player player, final TFCSupplyCartEntity cart) {
    try {
      return player.containerMenu instanceof SupplyCartContainer && MENU_CART.get(player.containerMenu) == cart;
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot read the cart of a TFC Astikor Carts inventory screen", e);
    }
  }

  // The same sums the cart weighs against its thresholds, with its riders when count_cart_riders adds them, and its
  // slots that hold large items. Every version's supply cart is a Container.
  private static CartLoad getLoad(final TFCSupplyCartEntity supplyCart) {
    return new CartLoad(LEGACY_SUPPLY_CART_LOAD != null ? getLegacyLoad(supplyCart)
        : getHundredth(supplyCart.countOverburdenedCargo() + countRiders(supplyCart)), getExhaustedLoad(),
        getOverburdenedLoad(), WeightBarHelpers.countLargeItemSlots(supplyCart), getMaxLargeItemSlots());
  }

  private static CartLoad getLoad(final TFCAnimalCartEntity animalCart) {
    return LEGACY_SUPPLY_CART_LOAD != null
        ? new CartLoad(animalCart.countOverburdened(), LEGACY_ANIMAL_CART_EXHAUSTED_LOAD,
            LEGACY_ANIMAL_CART_OVERBURDENED_LOAD)
        : new CartLoad(getHundredth(animalCart.countOverburdened() + countRiders(animalCart)), getExhaustedLoad(),
            getOverburdenedLoad());
  }

  /**
   * The mass of the players sitting on this cart, as the cart counts its own: each one's body as the mod weighs an
   * animal cart's passenger, and each stack they carry as it weighs cargo. 0 unless count_cart_riders is on. Only 1.1.9
   * and later have these sums, so up to 1.1.8 riders never count.
   */
  private static double countRiders(final Entity cart) {
    if (!Config.countCartRiders) {
      return 0;
    }

    double mass = 0;
    for (final Entity passenger : cart.getPassengers()) {
      if (passenger instanceof Player rider) {
        mass += AstikorHelpers.getEstimatedMassKg(rider.getBoundingBox());
        for (final ItemStack stack : WeightBarHelpers.getCarriedStacks(rider)) {
          final IItemSize size = ItemSizeManager.get(stack);
          mass += AstikorHelpers.getEstimatedMassKg(stack, size.getSize(stack), size.getWeight(stack));
        }
      }
    }
    return mass;
  }

  private static int getMaxLargeItemSlots() {
    return Config.enableCartLimits ? Config.astikorMaxLargeItemSlots : 0;
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
