package com.koenigstag.tfc_weight_bar.mixin;

import com.koenigstag.tfc_weight_bar.WeightBarHelpers;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A boat moves by the keys of the player who steers it, read where it is steered, on that player's client, and by
 * nothing of theirs else: an Overburdened player rows as fast as anyone. Under stop_overburdened_boats their keys are
 * dropped before the boat reads them, so it neither moves nor turns, and its paddles stop, as for a boat no one rows.
 * Every boat built on the vanilla one shares this method, TFC's included.
 */
@Mixin(Boat.class)
public abstract class BoatMixin {
  @Shadow
  private boolean inputLeft;
  @Shadow
  private boolean inputRight;
  @Shadow
  private boolean inputUp;
  @Shadow
  private boolean inputDown;

  @Inject(method = "controlBoat", at = @At("HEAD"))
  private void tfc_weight_bar$tooHeavyToRow(final CallbackInfo ci) {
    final Boat boat = (Boat) (Object) this;

    if ((inputLeft || inputRight || inputUp || inputDown) && WeightBarHelpers.isTooHeavyToRow(boat)) {
      inputLeft = false;
      inputRight = false;
      inputUp = false;
      inputDown = false;
      WeightBarHelpers.tellTooHeavyToRow(boat);
    }
  }
}
