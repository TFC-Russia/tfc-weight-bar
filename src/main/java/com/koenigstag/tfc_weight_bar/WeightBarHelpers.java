package com.koenigstag.tfc_weight_bar;

import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.dries007.tfc.common.capabilities.VesselLike;
import net.dries007.tfc.common.capabilities.size.IItemSize;
import net.dries007.tfc.common.capabilities.size.ItemSizeManager;
import net.dries007.tfc.common.capabilities.size.Size;
import net.dries007.tfc.common.capabilities.size.Weight;
import net.dries007.tfc.common.items.VesselItem;
import net.dries007.tfc.common.TFCEffects;

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
    if (!itemStack.isEmpty()) {

      int weightInt = getWeightInt(itemStack);
      int itemCount = itemStack.getCount();
      int itemStackWeight = Math.round(weightInt * itemCount);

      // Check for item holders (containers, backpacks, etc.)
      int itemHolderWeight = 0;

      // Method 1: IItemHandler capability
      LazyOptional<IItemHandler> itemHandlerCapability = itemStack.getCapability(ForgeCapabilities.ITEM_HANDLER);
      if (itemHandlerCapability.isPresent()) {
        IItemHandler itemHandler = itemHandlerCapability.resolve().get();
        itemHolderWeight += calculateItemHandlerWeight(itemHandler);
      }
      // Method 2: TFC Vessel items
      else if (itemStack.getItem() instanceof VesselItem) {
        VesselLike vessel = VesselItem.getInventoryVessel(itemStack);
        itemHolderWeight += calculateItemHandlerWeight(vessel);
      }
      // Method 3: Block entity items (chests, barrels, etc.)
      else {
        itemHolderWeight += getBlockEntityInventoryWeight(itemStack);
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

  // Get inventory from Block Entity ItemStack
  public static int getBlockEntityInventoryWeight(ItemStack itemStack) {
    if (itemStack.isEmpty()) {
      return 0;
    }

    // Method 2: Check NBT data for stored inventory (TFC and vanilla patterns)
    CompoundTag nbt = itemStack.getTag();
    if (nbt != null) {
      // TFC InventoryBlockEntity pattern: inventory stored as compound tag named "inventory"
      if (nbt.contains("inventory", Tag.TAG_COMPOUND)) {
        CompoundTag inventoryTag = nbt.getCompound("inventory");
        return calculateNBTInventoryFromCompound(inventoryTag);
      }

      // Standard "Items" tag used by many block entities
      if (nbt.contains("Items", Tag.TAG_LIST)) {
        ListTag itemsList = nbt.getList("Items", Tag.TAG_COMPOUND);
        return calculateNBTInventoryWeight(itemsList);
      }

      // Check for "Inventory" tag (some mods use this)
      if (nbt.contains("Inventory", Tag.TAG_LIST)) {
        ListTag inventoryList = nbt.getList("Inventory", Tag.TAG_COMPOUND);
        return calculateNBTInventoryWeight(inventoryList);
      }

      // Check for BlockEntityTag (vanilla block entities)
      if (nbt.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
        CompoundTag blockEntityTag = nbt.getCompound("BlockEntityTag");

        // TFC pattern inside BlockEntityTag
        if (blockEntityTag.contains("inventory", Tag.TAG_COMPOUND)) {
          CompoundTag inventoryTag = blockEntityTag.getCompound("inventory");
          return calculateNBTInventoryFromCompound(inventoryTag);
        }

        // Vanilla pattern inside BlockEntityTag
        if (blockEntityTag.contains("Items", Tag.TAG_LIST)) {
          ListTag itemsList = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
          return calculateNBTInventoryWeight(itemsList);
        }
      }
    }

    return 0;
  }

  // Helper method to calculate weight from NBT inventory data
  private static int calculateNBTInventoryWeight(ListTag itemsList) {
    int totalWeight = 0;

    for (int i = 0; i < itemsList.size(); i++) {
      CompoundTag itemTag = itemsList.getCompound(i);
      ItemStack stack = ItemStack.of(itemTag);
      if (!stack.isEmpty()) {
        totalWeight += getItemStackWeightInt(stack);
      }
    }

    return totalWeight;
  }

  // Helper method to calculate weight from TFC-style compound NBT inventory data
  private static int calculateNBTInventoryFromCompound(CompoundTag inventoryTag) {
    // Create temporary ItemStackHandler to deserialize the NBT (following TFC pattern)
    ItemStackHandler tempHandler = new ItemStackHandler();
    tempHandler.deserializeNBT(inventoryTag);

    int totalWeight = 0;
    for (int i = 0; i < tempHandler.getSlots(); i++) {
      ItemStack stack = tempHandler.getStackInSlot(i);
      if (!stack.isEmpty()) {
        totalWeight += getItemStackWeightInt(stack);
      }
    }

    return totalWeight;
  }
}
