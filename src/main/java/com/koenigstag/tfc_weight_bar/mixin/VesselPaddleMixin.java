package com.koenigstag.tfc_weight_bar.mixin;

import com.koenigstag.tfc_weight_bar.WeightBarHelpers;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An AlekiShips vessel that is rowed or paddled speeds up by its paddle multiplier, which only the player's paddles or
 * oars change, on both sides, as both move it. This scales it down by the vessel's load and to nothing for a player too
 * heavy to row. The rowboat is AlekiShips' own and Firmaciv's, the canoe and kayak are Firmaciv's; none of these mods
 * is required, so the classes are named, not linked.
 */
@Pseudo
@Mixin(targets = {
    "com.alekiponi.alekiships.common.entity.vehicle.RowboatEntity",
    "com.alekiponi.firmaciv.common.entity.vehicle.CanoeEntity",
    "com.alekiponi.firmaciv.common.entity.vehicle.KayakEntity"
}, remap = false)
public abstract class VesselPaddleMixin {

  @Inject(method = "getPaddleMultiplier", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
  private void tfc_weight_bar$holdBackByLoad(final CallbackInfoReturnable<Float> cir) {
    final float factor = WeightBarHelpers.getPaddleFactor((Entity) (Object) this);

    if (factor < 1) {
      cir.setReturnValue(cir.getReturnValueF() * factor);
    }
  }
}
