package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;
import java.util.Optional;

/**
 * Everything that touches the Curios API. Curios is optional, so call this only when
 * {@link WeightBarHelpers#isCuriosLoaded()} says it is there: loading this class without Curios fails.
 */
public final class CuriosCompat {

  private CuriosCompat() {}

  public static int calculateCuriosSlotsWeight(final Player livingEntity) {
    int curiosWeightInt = 0;

    // Curios 9 returns a plain Optional; Curios 5 handed out a Forge LazyOptional
    Optional<ICuriosItemHandler> maybeCuriosInventory = CuriosApi.getCuriosInventory(livingEntity);

    if (maybeCuriosInventory.isPresent()) {
      // Slot type id to its slots; the curios_slots config picks which slot types count
      for (Map.Entry<String, ICurioStacksHandler> slotType : maybeCuriosInventory.get().getCurios().entrySet()) {
        if (!Config.isCuriosSlotWeighed(slotType.getKey())) {
          continue;
        }

        IDynamicStackHandler stacks = slotType.getValue().getStacks();

        for (int i = 0; i < stacks.getSlots(); i++) {
          curiosWeightInt += WeightBarHelpers.getItemStackWeightInt(stacks.getStackInSlot(i), livingEntity.registryAccess());
        }
      }
    }

    return curiosWeightInt;
  }
}
