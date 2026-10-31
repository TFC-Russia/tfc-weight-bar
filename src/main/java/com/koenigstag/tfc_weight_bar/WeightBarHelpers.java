package com.koenigstag.tfc_weight_bar;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.dries007.tfc.common.component.size.IItemSize;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.dries007.tfc.common.effect.TFCEffects;

import javax.annotation.Nullable;

public class WeightBarHelpers {

  // Curios and TFC Curios Weight are optional: their classes are only reached through CuriosCompat and
  // CuriosWeightCompat, behind these checks
  public static boolean isCuriosLoaded() {
    return ModList.get().isLoaded("curios");
  }

  public static boolean isCuriosWeightLoaded() {
    return ModList.get().isLoaded("tfc_curios_weight");
  }

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
    return getItemStackWeightInt(itemStack, null);
  }

  /**
   * 1.21 replaced item NBT with data components, and reading a block entity inventory back out of a component needs a
   * registry lookup. Callers that have one pass it; the rest get everything except that last case.
   */
  public static int getItemStackWeightInt(ItemStack itemStack, @Nullable HolderLookup.Provider registries) {
    if (!itemStack.isEmpty()) {

      int weightInt = getWeightInt(itemStack);
      int itemCount = itemStack.getCount();
      int itemStackWeight = Math.round(weightInt * itemCount);

      // Check for item holders (containers, backpacks, etc.)
      int itemHolderWeight = 0;

      // Method 1: the item handler capability. A TFC IVessel is an IItemHandler now, so vessels come through here
      // and no longer need the VesselLike case they used to.
      final IItemHandler itemHandler = itemStack.getCapability(Capabilities.ItemHandler.ITEM);

      if (itemHandler != null) {
        itemHolderWeight += calculateItemHandlerWeight(itemHandler, registries);
      }
      // Method 2: the vanilla container component, which holds what a shulker box and friends carry
      else if (itemStack.has(DataComponents.CONTAINER)) {
        final ItemContainerContents contents = itemStack.get(DataComponents.CONTAINER);

        if (contents != null) {
          for (ItemStack stack : contents.nonEmptyItems()) {
            itemHolderWeight += getItemStackWeightInt(stack, registries);
          }
        }
      }
      // Method 3: block entity items (TFC barrels and chests, vanilla block entities)
      else if (registries != null) {
        itemHolderWeight += getBlockEntityInventoryWeight(itemStack, registries);
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

  /**
   * How many huge and very heavy items the player carries. TFC Curios Weight also counts the Curios slots, with its
   * exception for one backpack; without it this is TFC's own count over the inventory.
   */
  public static int countOverburdened(final Player player) {
    if (isCuriosWeightLoaded()) {
      return CuriosWeightCompat.countOverburdened(player);
    }

    final Container inventory = player.getInventory();
    int count = 0;

    for (int i = 0; i < inventory.getContainerSize(); i++) {
      count += getItemOverweightInt(inventory.getItem(i));
    }

    return count;
  }

  public static int calculateCuriosSlotsWeight(final Player livingEntity) {
    // Counts Curios slots to the full weight
    return isCuriosLoaded() ? CuriosCompat.calculateCuriosSlotsWeight(livingEntity) : 0;
  }

  public static int calculatePlayerFullWeight(final Player livingEntity) {
    int totalWeight = 0;

    totalWeight += calculateContainerWeight(livingEntity.getInventory(), livingEntity.registryAccess());
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
    // MobEffectInstance takes a Holder in 1.21, and the TFC effect id carries a DeferredHolder
    return new MobEffectInstance(TFCEffects.OVERBURDENED.holder(), Config.calculateWeightEachNTicks + 5, 0, false,
        visible);
  }

  public static MobEffectInstance getExhausted(boolean visible) {
    return new MobEffectInstance(TFCEffects.EXHAUSTED.holder(), Config.calculateWeightEachNTicks + 5, 0, false,
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

  public static int calculateContainerWeight(final Container container) {
    return calculateContainerWeight(container, null);
  }

  public static int calculateContainerWeight(final Container container, @Nullable HolderLookup.Provider registries) {
    if (container == null) {
      return 0;
    }

    int totalWeight = 0;

    for (int i = 0; i < container.getContainerSize(); i++) {
      ItemStack itemStack = container.getItem(i);

      totalWeight += getItemStackWeightInt(itemStack, registries);
    }

    return totalWeight;
  }

  public static int calculateItemHandlerWeight(final IItemHandler itemHandler) {
    return calculateItemHandlerWeight(itemHandler, null);
  }

  public static int calculateItemHandlerWeight(final IItemHandler itemHandler,
      @Nullable HolderLookup.Provider registries) {
    if (itemHandler == null) {
      return 0;
    }

    int totalWeight = 0;

    for (int i = 0; i < itemHandler.getSlots(); i++) {
      ItemStack itemStack = itemHandler.getStackInSlot(i);

      totalWeight += getItemStackWeightInt(itemStack, registries);
    }

    return totalWeight;
  }

  /**
   * Get the inventory out of a block entity item. What used to sit in the item NBT is the BLOCK_ENTITY_DATA component
   * in 1.21, and the tags inside it are the same ones the block entity writes, so the patterns below are unchanged.
   */
  public static int getBlockEntityInventoryWeight(ItemStack itemStack, HolderLookup.Provider registries) {
    if (itemStack.isEmpty()) {
      return 0;
    }

    final CustomData blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);

    if (blockEntityData == null || blockEntityData.isEmpty()) {
      return 0;
    }

    final CompoundTag nbt = blockEntityData.copyTag();

    // TFC InventoryBlockEntity pattern: inventory stored as compound tag named "inventory"
    if (nbt.contains("inventory", Tag.TAG_COMPOUND)) {
      CompoundTag inventoryTag = nbt.getCompound("inventory");
      return calculateNBTInventoryFromCompound(inventoryTag, registries);
    }

    // Standard "Items" tag used by many block entities
    if (nbt.contains("Items", Tag.TAG_LIST)) {
      ListTag itemsList = nbt.getList("Items", Tag.TAG_COMPOUND);
      return calculateNBTInventoryWeight(itemsList, registries);
    }

    // Check for "Inventory" tag (some mods use this)
    if (nbt.contains("Inventory", Tag.TAG_LIST)) {
      ListTag inventoryList = nbt.getList("Inventory", Tag.TAG_COMPOUND);
      return calculateNBTInventoryWeight(inventoryList, registries);
    }

    return 0;
  }

  // Helper method to calculate weight from NBT inventory data
  private static int calculateNBTInventoryWeight(ListTag itemsList, HolderLookup.Provider registries) {
    int totalWeight = 0;

    for (int i = 0; i < itemsList.size(); i++) {
      CompoundTag itemTag = itemsList.getCompound(i);
      // ItemStack.of is gone in 1.21: parsing a stack takes the registries and can fail
      ItemStack stack = ItemStack.parseOptional(registries, itemTag);
      if (!stack.isEmpty()) {
        totalWeight += getItemStackWeightInt(stack, registries);
      }
    }

    return totalWeight;
  }

  // Helper method to calculate weight from TFC-style compound NBT inventory data
  private static int calculateNBTInventoryFromCompound(CompoundTag inventoryTag, HolderLookup.Provider registries) {
    // Create temporary ItemStackHandler to deserialize the NBT (following TFC pattern)
    ItemStackHandler tempHandler = new ItemStackHandler();
    tempHandler.deserializeNBT(registries, inventoryTag);

    int totalWeight = 0;
    for (int i = 0; i < tempHandler.getSlots(); i++) {
      ItemStack stack = tempHandler.getStackInSlot(i);
      if (!stack.isEmpty()) {
        totalWeight += getItemStackWeightInt(stack, registries);
      }
    }

    return totalWeight;
  }
}
