package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
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

  /**
   * The stacks in the Curios slots that curios_slots picks, empty ones left out.
   */
  public static List<ItemStack> getWeighedCurios(final Player livingEntity) {
    final List<ItemStack> curios = new ArrayList<>();
    LazyOptional<ICuriosItemHandler> lazyCuriosInventory = CuriosApi.getCuriosInventory(livingEntity);
    if (lazyCuriosInventory.isPresent()) {
      for (Map.Entry<String, ICurioStacksHandler> slotType : lazyCuriosInventory.resolve().get().getCurios().entrySet()) {
        if (!Config.isCuriosSlotWeighed(slotType.getKey())) {
          continue;
        }
        IDynamicStackHandler stacks = slotType.getValue().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
          if (!stacks.getStackInSlot(i).isEmpty()) {
            curios.add(stacks.getStackInSlot(i));
          }
        }
      }
    }
    return curios;
  }
}
