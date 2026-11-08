package com.koenigstag.tfc_weight_bar;

import java.lang.reflect.Method;

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
 *
 * <p>Two ways of weighing a cart are around. Since 1.1.9 the cart weighs its cargo as a mass, every item of a stack
 * counted, compares a hundredth of it with the thresholds in its config, and burdens only a player who pulls it on
 * foot. Up to 1.1.8 a slot weighed the same whether it held one item or a full stack, and the rider of an animal that
 * pulls the cart was burdened too. This is built against 1.1.9 and reaches the old counting method by name, since
 * 1.1.9 removed it.
 */
public final class AstikorCartsCompat {
  // Up to 1.1.8 the animal cart weighs its passengers against these, written into its code rather than its config
  private static final float LEGACY_ANIMAL_CART_EXHAUSTED_LOAD = 10;
  private static final float LEGACY_ANIMAL_CART_OVERBURDENED_LOAD = 20;

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
   * The load of the cart that burdens this player, or null when none does.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    final boolean legacy = LEGACY_SUPPLY_CART_LOAD != null;

    // Up to 1.1.8 the rider who steers the animal that pulls the cart is burdened as well
    final Entity vehicle = player.getVehicle();
    final Entity puller = legacy && vehicle != null && vehicle.getControllingPassenger() == player ? vehicle : player;

    final AbstractDrawnEntity cart = AstikorWorld.get(player.level()).resolve()
        .flatMap(world -> world.getDrawn(puller))
        .orElse(null);

    if (cart instanceof TFCSupplyCartEntity supplyCart) {
      return new CartLoad(legacy ? getLegacyLoad(supplyCart) : getLoad(supplyCart.countOverburdenedCargo()),
          getExhaustedLoad(), getOverburdenedLoad());
    }

    if (cart instanceof TFCAnimalCartEntity animalCart) {
      return legacy
          ? new CartLoad(animalCart.countOverburdened(), LEGACY_ANIMAL_CART_EXHAUSTED_LOAD,
              LEGACY_ANIMAL_CART_OVERBURDENED_LOAD)
          : new CartLoad(getLoad(animalCart.countOverburdened()), getExhaustedLoad(), getOverburdenedLoad());
    }

    // A plough, or a cart of another mod, carries no load
    return null;
  }

  // Since 1.1.9 a cart compares a hundredth of what it counts with its thresholds, and never less than 1
  private static float getLoad(final double count) {
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
