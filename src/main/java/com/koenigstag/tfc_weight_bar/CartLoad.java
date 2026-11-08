package com.koenigstag.tfc_weight_bar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The load of the cart a player pulls, as TFC Astikor Carts counts it, and the loads past which that mod makes the
 * player Exhausted and Overburdened. The cart's own scale, apart from the player's weight.
 */
public record CartLoad(float load, float exhaustedLoad, float overburdenedLoad) {

  public static final StreamCodec<FriendlyByteBuf, CartLoad> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, CartLoad::load,
      ByteBufCodecs.FLOAT, CartLoad::exhaustedLoad,
      ByteBufCodecs.FLOAT, CartLoad::overburdenedLoad,
      CartLoad::new);

  public boolean isExhausted() {
    return load > exhaustedLoad;
  }

  public boolean isOverburdened() {
    return load > overburdenedLoad;
  }
}
