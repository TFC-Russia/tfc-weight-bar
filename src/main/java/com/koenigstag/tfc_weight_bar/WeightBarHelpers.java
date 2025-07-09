package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import net.dries007.tfc.common.capabilities.VesselLike;
import net.dries007.tfc.common.capabilities.size.IItemSize;
import net.dries007.tfc.common.capabilities.size.ItemSizeManager;
import net.dries007.tfc.common.capabilities.size.Size;
import net.dries007.tfc.common.capabilities.size.Weight;
import net.dries007.tfc.common.items.VesselItem;
import net.dries007.tfc.common.TFCEffects;

public class WeightBarHelpers {

  public static int getMaxInventoryWeight(Player player) {
    Container container = player.getInventory();

    if (container == null) {
      return 0;
    }

    // Use the effective health percentage method
    double effectiveHealthPercentage = getEffectiveHealthPercentage(player);

    // Calculate max inventory weight based on effective health percentage
    int adjustedMaxWeight = (int) Math.round(Config.maxInvWeight * effectiveHealthPercentage);

    return adjustedMaxWeight;
  }

  public static double getBarPercentage(int playerInvWeight, Player player) {
    // 80 / 100 = 0.8
    return (double) playerInvWeight / (double) getMaxInventoryWeight(player);
  }

  public static int getItemStackWeightInt(ItemStack itemStack) {
    if (!itemStack.isEmpty()) {

      int weightInt = getWeightInt(itemStack);
      int itemCount = itemStack.getCount();
      int itemStackWeight = Math.round(weightInt * itemCount);

      // if item stack is item holder (e.g. backpack)
      LazyOptional<IItemHandler> itemHandlerCapability = itemStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
      int itemHolderWeight = 0;
      if (itemHandlerCapability.isPresent()) {
        IItemHandler itemHandler = itemHandlerCapability.resolve().get();

        itemHolderWeight += calculateItemHandlerWeight(itemHandler);
      } else if (itemStack.getItem() instanceof VesselItem) {
        VesselLike vessel = VesselItem.getInventoryVessel(itemStack);

        itemHolderWeight += calculateItemHandlerWeight(vessel);
      }

      return itemStackWeight + itemHolderWeight;
    }

    return 0;
  }

  public static int getItemOverweightInt(ItemStack itemStack) {
    if (!itemStack.isEmpty()) {
      IItemSize size = ItemSizeManager.get(itemStack);

      if (size.getWeight(itemStack) == Weight.VERY_HEAVY
          && size.getSize(itemStack) == Size.HUGE) {
        return 1;
      }
    }

    return 0;
  }

  public static int calculateContainerWeight(final Container container) {
    if (container == null) {
      return 0;
    }

    int totalWeight = 0;

    for (int i = 0; i < container.getContainerSize(); i++) {
      ItemStack itemStack = container.getItem(i);

      totalWeight += getItemStackWeightInt(itemStack);
    }

    return totalWeight;
  }

  public static int calculateItemHandlerWeight(final IItemHandler itemHandler) {
    if (itemHandler == null) {
      return 0;
    }

    int totalWeight = 0;

    for (int i = 0; i < itemHandler.getSlots(); i++) {
      ItemStack itemStack = itemHandler.getStackInSlot(i);

      totalWeight += getItemStackWeightInt(itemStack);
    }

    return totalWeight;
  }

  public static int calculateCuriosSlotsWeight(final Player livingEntity) {
    int curiosWeightInt = 0;

    // Counts Curios slots to the full weight
    if (ModList.get().isLoaded("curios")) {
      LazyOptional<ICuriosItemHandler> lazyCuriosInventory = CuriosApi.getCuriosInventory(livingEntity);

      if (lazyCuriosInventory.isPresent()) {
        ICuriosItemHandler curiosInventory = lazyCuriosInventory.resolve().get();
        IItemHandlerModifiable curiosContainer = curiosInventory.getEquippedCurios();

        for (int i = 0; i < curiosContainer.getSlots(); i++) {
          final ItemStack stack = curiosContainer.getStackInSlot(i);

          curiosWeightInt += WeightBarHelpers.getItemStackWeightInt(stack);
        }
      }
    }

    return curiosWeightInt;
  }

  public static int calculatePlayerFullWeight(final Player livingEntity) {
    int totalWeight = 0;

    totalWeight += calculateContainerWeight(livingEntity.getInventory());
    totalWeight += calculateCuriosSlotsWeight(livingEntity);

    return totalWeight;
  }

  public static boolean isPlayerExhausted(int playerInvWeight, int maxInvWeight) {
    return playerInvWeight >= (maxInvWeight * Config.getExhaustedWeightCoefficient());
  }

  public static boolean isPlayerOverburdened(int playerInvWeight, int maxInvWeight) {
    return playerInvWeight > maxInvWeight;
  }

  public static String getBarColor(int playerInvWeight, int maxInvWeight, int hugeHeavyCount) {
    boolean isOverburdened = getIsOverburdened(playerInvWeight, maxInvWeight, hugeHeavyCount);
    boolean isExhausted = getIsExhausted(playerInvWeight, maxInvWeight, hugeHeavyCount);

    if (isOverburdened) {
      return Config.getOverburdenedBarColor();
    } else if (isExhausted) {
      return Config.getExhaustedBarColor();
    } else {
      return Config.getNormalBarColor();
    }
  }

  public static MobEffectInstance getOverburdened(boolean visible) {
    return new MobEffectInstance(TFCEffects.OVERBURDENED.get(), Config.calculateWeightEachNTicks + 5, 0, false,
        visible);
  }

  public static MobEffectInstance getExhausted(boolean visible) {
    return new MobEffectInstance(TFCEffects.EXHAUSTED.get(), Config.calculateWeightEachNTicks + 5, 0, false,
        visible);
  }

  public static boolean getIsExhausted(int playerInvWeight, int maxInvWeight, int hugeHeavyCount) {
    return WeightBarHelpers.isPlayerExhausted(playerInvWeight, maxInvWeight) || hugeHeavyCount >= 1;
  }

  public static boolean getIsOverburdened(int playerInvWeight, int maxInvWeight, int hugeHeavyCount) {
    return WeightBarHelpers.isPlayerOverburdened(playerInvWeight, maxInvWeight)
        || hugeHeavyCount >= 2;
  }

  public static double getHealthPercentage(Player player) {
    float currentHealth = player.getHealth();
    float maxHealth = player.getMaxHealth();
    return (double) currentHealth / (double) maxHealth;
  }

  public static double getEffectiveHealthPercentage(Player player) {
    if (!Config.healthAffectsWeightCapacity) {
      return 1.0;
    }

    double healthPercentage = getHealthPercentage(player);
    return Math.max(Config.minHealthWeightPercentage, Math.min(1.0, healthPercentage));
  }

  public static String getPlayerWeightStatus(Player player) {
    int currentWeight = calculatePlayerFullWeight(player);
    int maxWeight = getMaxInventoryWeight(player);
    double effectiveHealthPercentage = getEffectiveHealthPercentage(player);

    if (effectiveHealthPercentage < 1.0) {
      return String.format("Weight: %d/%d (Health: %.0f%%)", 
          currentWeight, maxWeight, effectiveHealthPercentage * 100);
    } else {
      return String.format("Weight: %d/%d", currentWeight, maxWeight);
    }
  }

  // Debug method to test health percentage logic
  public static void debugHealthPercentage(Player player) {
    double healthPercentage = getHealthPercentage(player);
    double effectiveHealthPercentage = getEffectiveHealthPercentage(player);
    int maxWeight = getMaxInventoryWeight(player);

    Constants.debug(String.format("Player Health: %.1f/%.1f (%.1f%%)",
        player.getHealth(), player.getMaxHealth(), healthPercentage * 100));
    Constants.debug(String.format("Effective Health Percentage: %.1f%%",
        effectiveHealthPercentage * 100));
    Constants.debug(String.format("Max Weight: %d (Config: %d)",
        maxWeight, Config.maxInvWeight));
    Constants.debug(String.format("Health Affects Weight: %s",
        Config.healthAffectsWeightCapacity ? "YES" : "NO"));
  }

  public static int getWeightInt(ItemStack itemStack) {
    int tagWeightInt = getWeightIntFromTag(itemStack);

    if (tagWeightInt > 0) {
      return tagWeightInt;
    }

    IItemSize size = ItemSizeManager.get(itemStack);
    Weight weight = size.getWeight(itemStack);

    /*
     * From TFC Weight Enum
     * Default values are:
     * VERY_LIGHT(64), // 1 points
     * LIGHT(32), // 4 points
     * MEDIUM(16), // 16 points
     * HEAVY(4), // 32 points
     * VERY_HEAVY(1); // 64 points
     */
    switch (weight) {
      case VERY_LIGHT:
        return Config.veryLightWeightInt;
      case LIGHT:
        return Config.lightWeightInt;
      case MEDIUM:
        return Config.mediumWeightInt;
      case HEAVY:
        return Config.heavyWeightInt;
      case VERY_HEAVY:
        return Config.veryHeavyWeightInt;
      default:
        // any other item defaults to this int
        return Config.veryLightWeightInt;
    }
  }

  public static int getWeightIntFromTag(ItemStack itemStack) {
    if (itemStack.is(Tags.Items.VERY_LIGHT)) {
      return Config.veryLightWeightInt;
    } else if (itemStack.is(Tags.Items.LIGHT)) {
      return Config.lightWeightInt;
    } else if (itemStack.is(Tags.Items.MEDIUM)) {
      return Config.mediumWeightInt;
    } else if (itemStack.is(Tags.Items.HEAVY)) {
      return Config.heavyWeightInt;
    } else if (itemStack.is(Tags.Items.VERY_HEAVY)) {
      return Config.veryHeavyWeightInt;
    }

    // Default weight if no tag matches
    return 0;
  }
}
