package com.koenigstag.tfc_weight_bar.mixin;

import com.koenigstag.tfc_weight_bar.WeightBarHelpers;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * An AlekiShips sloop, AlekiShips' own or Firmaciv's, has no paddles: each sail it has up raises its acceleration to
 * what the wind gives that sail, the first and third time tickWindInput sets it, for the mainsail and the jib; the
 * second and fourth only let it ebb. This scales what the sails give by the sloop's load. Named, not linked, as
 * AlekiShips is not required.
 */
@Pseudo
@Mixin(targets = "com.alekiponi.alekiships.common.entity.vehicle.SloopEntity", remap = false)
public abstract class SloopSailMixin {
  private static final String SET_ACCELERATION =
      "Lcom/alekiponi/alekiships/common/entity/vehicle/SloopEntity;setAcceleration(F)V";

  @ModifyArg(method = "tickWindInput",
      at = @At(value = "INVOKE", target = SET_ACCELERATION, ordinal = 0, remap = false), require = 0, remap = false)
  private float tfc_weight_bar$holdBackMainsail(final float acceleration) {
    return acceleration * WeightBarHelpers.getSailFactor((Entity) (Object) this);
  }

  @ModifyArg(method = "tickWindInput",
      at = @At(value = "INVOKE", target = SET_ACCELERATION, ordinal = 2, remap = false), require = 0, remap = false)
  private float tfc_weight_bar$holdBackJib(final float acceleration) {
    return acceleration * WeightBarHelpers.getSailFactor((Entity) (Object) this);
  }
}
