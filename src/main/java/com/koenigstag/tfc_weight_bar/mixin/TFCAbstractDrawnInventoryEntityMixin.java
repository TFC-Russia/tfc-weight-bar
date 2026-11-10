package com.koenigstag.tfc_weight_bar.mixin;

import com.koenigstag.tfc_weight_bar.WeightBarHelpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A TFC Astikor Carts cart with cargo, the supply cart or the hand cart, burdens the player who pulls it, as it ticks, by
 * the mass of its cargo, which never looks into a container. While this mod weighs that cargo instead, the mass comes
 * out 0, a hundredth of which, never under 1, is below every threshold, and this mod burdens by the weight.
 *
 * <p>Only the cart's tick asks for this mass; this mod reads it itself only while it does not weigh the cargo. The mod
 * is optional, so its class is named here, not linked, and nothing is required.
 */
@Pseudo
@Mixin(targets = "tfcastikorcarts.common.entities.carts.TFCAbstractDrawnInventoryEntity", remap = false)
public abstract class TFCAbstractDrawnInventoryEntityMixin {

  @Inject(method = "countOverburdenedCargo", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
  private void tfc_weight_bar$leaveCargoToWeight(final CallbackInfoReturnable<Double> cir) {
    if (WeightBarHelpers.weighsAstikorCargo()) {
      cir.setReturnValue(0.0);
    }
  }
}
