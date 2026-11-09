package com.koenigstag.tfc_weight_bar;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The load of the cart a player pulls, or draws with the animal they ride, as TFC Astikor Carts counts it, and the loads
 * past which that mod makes the player Exhausted and Overburdened. The cart's own scale, apart from the player's weight.
 *
 * <p>A cart can also be overloaded by how many of its slots hold large items, whatever its load: largeItemSlots against
 * maxLargeItemSlots, which is 0 when the server sets no such limit.
 */
public record CartLoad(float load, float exhaustedLoad, float overburdenedLoad, int largeItemSlots,
    int maxLargeItemSlots) {

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

  public void encode(final FriendlyByteBuf buffer) {
    buffer.writeFloat(load);
    buffer.writeFloat(exhaustedLoad);
    buffer.writeFloat(overburdenedLoad);
    buffer.writeVarInt(largeItemSlots);
    buffer.writeVarInt(maxLargeItemSlots);
  }

  public static CartLoad decode(final FriendlyByteBuf buffer) {
    return new CartLoad(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(),
        buffer.readVarInt());
  }
}
