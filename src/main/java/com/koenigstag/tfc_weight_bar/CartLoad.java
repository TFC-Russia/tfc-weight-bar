package com.koenigstag.tfc_weight_bar;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The load of the cart a player pulls, or draws with the animal they ride, as TFC Astikor Carts counts it, and the loads
 * past which that mod makes the player Exhausted and Overburdened. The cart's own scale, apart from the player's weight.
 */
public record CartLoad(float load, float exhaustedLoad, float overburdenedLoad) {

  public boolean isExhausted() {
    return load > exhaustedLoad;
  }

  public boolean isOverburdened() {
    return load > overburdenedLoad;
  }

  public void encode(final FriendlyByteBuf buffer) {
    buffer.writeFloat(load);
    buffer.writeFloat(exhaustedLoad);
    buffer.writeFloat(overburdenedLoad);
  }

  public static CartLoad decode(final FriendlyByteBuf buffer) {
    return new CartLoad(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
  }
}
