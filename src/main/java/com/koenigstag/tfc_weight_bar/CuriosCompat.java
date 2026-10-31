package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

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
      ICuriosItemHandler curiosInventory = maybeCuriosInventory.get();
      IItemHandlerModifiable curiosContainer = curiosInventory.getEquippedCurios();

      for (int i = 0; i < curiosContainer.getSlots(); i++) {
        final ItemStack stack = curiosContainer.getStackInSlot(i);

        curiosWeightInt += WeightBarHelpers.getItemStackWeightInt(stack, livingEntity.registryAccess());
      }
    }

    return curiosWeightInt;
  }

  public static boolean isSlot(final String slotId) {
    // TODO fix this. CuriosApi.getSlot(String id, boolean isClient) needs to know the side
    return CuriosApi.getSlot(slotId, false).isPresent();
  }
}
