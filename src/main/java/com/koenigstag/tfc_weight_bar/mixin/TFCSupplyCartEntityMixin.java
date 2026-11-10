package com.koenigstag.tfc_weight_bar.mixin;

import com.koenigstag.tfc_weight_bar.WeightBarHelpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A TFC Astikor Carts supply cart burdens the player who pulls it, as it ticks, by a sum of its own: the mass of its
 * cargo since 1.1.9, its slots up to 1.1.8, neither looking into a container. While this mod weighs that cargo
 * instead, the sum comes out 0, below every threshold, and this mod burdens by the weight.
 *
 * <p>Only the cart's tick asks for these sums; this mod reads them itself only while it does not weigh the cargo. The
 * cart class keeps its name on every version, and the mod is optional, so it is named here, not linked; each version
 * has one of the two methods, so neither is required.
 */
@Pseudo
@Mixin(targets = "tfcastikorcarts.common.entities.carts.TFCSupplyCartEntity", remap = false)
public abstract class TFCSupplyCartEntityMixin {

  // 1.1.9 and later: the mass of the cargo in kilograms, a hundredth of which, never under 1, meets the thresholds
  @Inject(method = "countOverburdenedCargo", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
  private void tfc_weight_bar$leaveCargoToWeight(final CallbackInfoReturnable<Double> cir) {
    if (WeightBarHelpers.weighsAstikorCargo()) {
      cir.setReturnValue(0.0);
    }
  }

  // Up to 1.1.8: the size and weight factors of the slots, which meet the thresholds as they are
  @Inject(method = "countOverburdened", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
  private void tfc_weight_bar$leaveSlotsToWeight(final CallbackInfoReturnable<Float> cir) {
    if (WeightBarHelpers.weighsAstikorCargo()) {
      cir.setReturnValue(0f);
    }
  }
}
