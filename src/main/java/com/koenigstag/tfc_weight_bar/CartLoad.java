package com.koenigstag.tfc_weight_bar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The load of the cart a player pulls, as TFC Astikor Carts counts it, and the loads past which that mod makes the
 * player Exhausted and Overburdened. The cart's own scale, apart from the player's weight.
 *
 * <p>A cart can also be overloaded by how many of its slots hold large items, whatever its load: largeItemSlots against
 * maxLargeItemSlots, which is 0 when the server sets no such limit.
 */
public record CartLoad(float load, float exhaustedLoad, float overburdenedLoad, int largeItemSlots,
    int maxLargeItemSlots) {

  public static final StreamCodec<FriendlyByteBuf, CartLoad> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, CartLoad::load,
      ByteBufCodecs.FLOAT, CartLoad::exhaustedLoad,
      ByteBufCodecs.FLOAT, CartLoad::overburdenedLoad,
      ByteBufCodecs.VAR_INT, CartLoad::largeItemSlots,
      ByteBufCodecs.VAR_INT, CartLoad::maxLargeItemSlots,
      CartLoad::new);

  public CartLoad(final float load, final float exhaustedLoad, final float overburdenedLoad) {
    this(load, exhaustedLoad, overburdenedLoad, 0, 0);
  }

  public boolean isExhausted() {
    return load > exhaustedLoad;
  }

  public boolean isOverburdened() {
    return load > overburdenedLoad || hasTooManyLargeItems();
  }

  public boolean hasTooManyLargeItems() {
    return maxLargeItemSlots > 0 && largeItemSlots > maxLargeItemSlots;
  }

  /**
   * How close the cart is to its limit, 1 at it, to tell which of two carts is closer.
   */
  public float getFullness() {
    if (hasTooManyLargeItems()) {
      return Float.MAX_VALUE;
    }
    return overburdenedLoad > 0 ? load / overburdenedLoad : Float.MAX_VALUE;
  }
}
