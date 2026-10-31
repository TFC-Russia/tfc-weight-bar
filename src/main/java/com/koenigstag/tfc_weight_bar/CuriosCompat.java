package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;

/**
 * Everything that touches the Curios API. Curios is optional, so call this only when
 * {@link WeightBarHelpers#isCuriosLoaded()} says it is there: loading this class without Curios fails.
 */
public final class CuriosCompat {

  private CuriosCompat() {}

  public static int calculateCuriosSlotsWeight(final Player livingEntity) {
    int curiosWeightInt = 0;

    LazyOptional<ICuriosItemHandler> lazyCuriosInventory = CuriosApi.getCuriosInventory(livingEntity);

    if (lazyCuriosInventory.isPresent()) {
      // Slot type id to its slots; the curios_slots config picks which slot types count
      for (Map.Entry<String, ICurioStacksHandler> slotType : lazyCuriosInventory.resolve().get().getCurios().entrySet()) {
        if (!Config.isCuriosSlotWeighed(slotType.getKey())) {
          continue;
        }

        IDynamicStackHandler stacks = slotType.getValue().getStacks();

        for (int i = 0; i < stacks.getSlots(); i++) {
          curiosWeightInt += WeightBarHelpers.getItemStackWeightInt(stacks.getStackInSlot(i));
        }
      }
    }

    return curiosWeightInt;
  }
}
