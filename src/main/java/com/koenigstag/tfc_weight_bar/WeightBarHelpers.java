package com.koenigstag.tfc_weight_bar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WeightBarHelpers {

  // Curios and TFC Curios Weight are optional: their classes are only reached through CuriosCompat and
  // CuriosWeightCompat, behind these checks
  public static boolean isCuriosLoaded() {
    return ModList.get().isLoaded("curios");
  }

  public static boolean isCuriosWeightLoaded() {
    return ModList.get().isLoaded("tfc_curios_weight");
  }

  // Each set once a version of that mod turns out not to match what this mod reads from it, which should cost the cart
  // features, not the server
  private static boolean astikorCartsBroken = false;
  private static boolean pullCartBroken = false;
  private static boolean alekiShipsBroken = false;

  private static boolean isAstikorCartsUsable() {
    return !astikorCartsBroken && ModList.get().isLoaded("tfcastikorcarts");
  }

  private static boolean areVesselLimitsUsable() {
    return Config.enableWeightCalculations && Config.enableVesselLimits && !alekiShipsBroken
        && ModList.get().isLoaded("alekiships");
  }

  private static void turnAlekiShipsOff(final Throwable cause) {
    alekiShipsBroken = true;
    Constants.LOGGER.error("Cannot read the vessels of AlekiShips, so their bar and limit stay off", cause);
  }

  private static boolean isPullCartUsable() {
    return !pullCartBroken && ModList.get().isLoaded("pull_cart");
  }

  private static void turnAstikorCartsOff(final Throwable cause) {
    astikorCartsBroken = true;
    Constants.LOGGER.error("Cannot read the cart load from TFC Astikor Carts, so its cart bar and limit stay off", cause);
  }

  private static void turnPullCartOff(final Throwable cause) {
    pullCartBroken = true;
    Constants.LOGGER.error("Cannot read the cart load from Pull Cart, so its cart bar and limit stay off", cause);
  }

  /**
   * The load of the cart the player pulls, or the animal they ride pulls, or null when there is none. Each mod allows one
   * cart per player only among its own, so a player can pull one of each, and then this is the one closer to its limit.
   * Pull Cart has no load of its own, so with enable_cart_limits off it has none here either. The carts' classes are only
   * reached through AstikorCartsCompat and PullCartCompat, behind these checks. A player at the helm of a vessel with a
   * limit gets its load instead, under enable_vessel_limits, since they pull no cart from there.
   */
  @Nullable
  public static CartLoad getCartLoad(final Player player) {
    if (areVesselLimitsUsable()) {
      try {
        final CartLoad vesselLoad = VesselCompat.getHelmLoad(player);
        if (vesselLoad != null) {
          return vesselLoad;
        }
      } catch (LinkageError | RuntimeException e) {
        turnAlekiShipsOff(e);
      }
    }

    CartLoad astikorLoad = null;
    CartLoad pullCartLoad = null;

    if (isAstikorCartsUsable()) {
      try {
        astikorLoad = AstikorCartsCompat.getCartLoad(player);
      } catch (LinkageError | RuntimeException e) {
        turnAstikorCartsOff(e);
      }
    }

    if (Config.enableCartLimits && isPullCartUsable()) {
      try {
        pullCartLoad = PullCartCompat.getCartLoad(player);
      } catch (LinkageError | RuntimeException e) {
        turnPullCartOff(e);
      }
    }

    if (astikorLoad == null || pullCartLoad == null) {
      return astikorLoad != null ? astikorLoad : pullCartLoad;
    }

    return pullCartLoad.getFullness() > astikorLoad.getFullness() ? pullCartLoad : astikorLoad;
  }

  /**
   * How many slots of this container hold items of large_item_size or bigger.
   */
  public static int countLargeItemSlots(final Container container) {
    int count = 0;

    for (int i = 0; i < container.getContainerSize(); i++) {
      final ItemStack stack = container.getItem(i);
      if (!stack.isEmpty() && !ItemSizeManager.get(stack).getSize(stack).isSmallerThan(Config.largeItemSize)) {
        count++;
      }
    }

    return count;
  }

  /**
   * Every stack the player carries where this mod weighs it: the inventory, armour and offhand included, and the Curios
   * slots that curios_slots picks. Items inside these are left in them.
   */
  public static List<ItemStack> getCarriedStacks(final Player player) {
    final List<ItemStack> stacks = new ArrayList<>();
    final Container inventory = player.getInventory();

    for (int i = 0; i < inventory.getContainerSize(); i++) {
      if (!inventory.getItem(i).isEmpty()) {
        stacks.add(inventory.getItem(i));
      }
    }

    if (isCuriosLoaded()) {
      stacks.addAll(CuriosCompat.getWeighedCurios(player));
    }

    return stacks;
  }

  /**
   * Deals with the animals near the players in this level that pull carts loaded past their limits, as
   * overloaded_cart says, and gives their speed back to the animals held still that no longer do. Runs every tick,
   * limits on or off, so that turning them off lets every held animal go.
   */
  public static void tickCarts(final ServerLevel level) {
    CartHolds.beginTick();

    if (Config.enableWeightCalculations && Config.enableCartLimits) {
      if (isAstikorCartsUsable()) {
        try {
          for (final ServerPlayer player : level.players()) {
            AstikorCartsCompat.checkOverloadedCarts(player);
          }
        } catch (LinkageError | RuntimeException e) {
          turnAstikorCartsOff(e);
        }
      }

      if (isPullCartUsable()) {
        try {
          for (final ServerPlayer player : level.players()) {
            PullCartCompat.checkOverloadedCart(player);
          }
        } catch (LinkageError | RuntimeException e) {
          turnPullCartOff(e);
        }
      }
    }

    CartHolds.endTick(level);
  }

  /**
   * An animal pulls a cart loaded past its limit. With overloaded_cart at STOP it stands still with the cart; else, or
   * when it is no animal, letGo makes it let go of the cart. While a player loads the cart with no one on the animal,
   * it waits until the screen is closed, since the screen covers the middle of the message.
   */
  public static void onCartOverloaded(final Entity animal, final Entity cart, final CartLoad load,
      final boolean beingLoaded, final Runnable letGo) {
    if (Config.overloadedCart == Config.OverloadedCart.STOP && animal instanceof LivingEntity living
        && !(animal instanceof Player)) {
      CartHolds.hold(living, cart, load, beingLoaded);
    } else if (!beingLoaded) {
      letGo.run();
      announceCartOverloaded(animal, cart, load, true);
    }
  }

  /**
   * Whether this mod weighs the cargo of a TFC Astikor Carts supply cart, and limits and burdens by it, so that the
   * cart's own sum must burden no one. TFCSupplyCartEntityMixin asks this as the cart ticks, on both sides; the client
   * has the server's config.
   */
  public static boolean weighsAstikorCargo() {
    return Config.enableWeightCalculations && Config.enableCartLimits && isAstikorCartsUsable();
  }

  /**
   * Burdens a player who pulls an overloaded cart on foot: a TFC Astikor Carts supply cart, whose own debuffs for its
   * cargo are off while this mod weighs it, or a Pull Cart cart, which has none.
   */
  public static void burdenWithCarts(final Player player) {
    if (weighsAstikorCargo()) {
      try {
        AstikorCartsCompat.burden(player);
      } catch (LinkageError | RuntimeException e) {
        turnAstikorCartsOff(e);
      }
    }

    if (isPullCartUsable()) {
      try {
        PullCartCompat.burden(player);
      } catch (LinkageError | RuntimeException e) {
        turnPullCartOff(e);
      }
    }
  }

  /**
   * An animal just let go of a cart, or came to a stop with it: it rears and the players around read why, over the
   * hotbar and with the numbers, since the cart bar shows only to whoever pulls the cart. Letting go also sounds as a
   * lead that breaks.
   */
  public static void announceCartOverloaded(final Entity animal, final Entity cart, final CartLoad load,
      final boolean letGo) {
    if (animal instanceof AbstractHorse horse) {
      horse.makeMad();
      // The smoke of a horse that will not have it, which TFC also sends when a horse kicks a rider off
      cart.level().broadcastEntityEvent(horse, (byte) 6);
    }

    if (letGo) {
      cart.level().playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.LEASH_KNOT_BREAK,
          SoundSource.NEUTRAL, 1f, 1f);
    }

    final String key = Constants.MOD_ID + (letGo ? ".message.cart_refused" : ".message.cart_stopped");
    final Component message = load.load() <= load.overburdenedLoad() && load.hasTooManyLargeItems()
        ? Component.translatable(key + "_large_items", animal.getDisplayName(), load.largeItemSlots(),
            load.maxLargeItemSlots())
        : Component.translatable(key, animal.getDisplayName(), Math.round(load.load()),
            Math.round(load.overburdenedLoad()));

    for (final Player near : cart.level().getEntitiesOfClass(Player.class, cart.getBoundingBox().inflate(CART_MESSAGE_RADIUS))) {
      near.displayClientMessage(message, true);
    }
  }

  private static final double CART_MESSAGE_RADIUS = 8;

  // How often a player who cannot row is told why, while they keep trying
  private static final long TOO_HEAVY_TO_ROW_MESSAGE_TICKS = 40;
  private static long tooHeavyToRowToldAt = Long.MIN_VALUE;

  /**
   * Whether the player steering this boat is too heavy to row it: Overburdened, by weight or by TFC's huge and very
   * heavy items, under stop_overburdened_boats. BoatMixin asks this where the boat is steered, on that player's client,
   * which has the server's config and its own player's effects.
   */
  public static boolean isTooHeavyToRow(final Entity boat) {
    return Config.enableWeightCalculations && Config.stopOverburdenedBoats
        && boat.getControllingPassenger() instanceof Player pilot && pilot.hasEffect(TFCEffects.OVERBURDENED.get());
  }

  /**
   * Tells the player who tries to row while too heavy why the boat does not move, over the hotbar, every two seconds
   * while they keep trying.
   */
  public static void tellTooHeavyToRow(final Entity boat) {
    tellHelm(boat, Component.translatable(Constants.MOD_ID + ".message.too_heavy_to_row"));
  }

  private static void tellHelm(final Entity boat, final Component message) {
    final long now = boat.level().getGameTime();

    if (boat.getControllingPassenger() instanceof Player pilot
        && (now < tooHeavyToRowToldAt || now - tooHeavyToRowToldAt >= TOO_HEAVY_TO_ROW_MESSAGE_TICKS)) {
      tooHeavyToRowToldAt = now;
      pilot.displayClientMessage(message, true);
    }
  }

  /**
   * How much of what its paddles or oars give an AlekiShips vessel keeps, 1 for all of it: none while the player at the
   * helm is too heavy to row, else as its load allows under enable_vessel_limits. VesselPaddleMixin asks this on both
   * sides, as the vessel moves on both; the player at the helm reads why it does not move while they try.
   */
  public static float getPaddleFactor(final Entity vessel) {
    if (isTooHeavyToRow(vessel)) {
      tellHelmWhyStill(vessel, Component.translatable(Constants.MOD_ID + ".message.too_heavy_to_row"));
      return 0;
    }
    return getVesselLoadFactor(vessel);
  }

  /**
   * How much of what its sails give an AlekiShips sloop keeps, 1 for all of it, as its load allows under
   * enable_vessel_limits. A player who is too heavy to row can still sail. SloopSailMixin asks this on both sides.
   */
  public static float getSailFactor(final Entity vessel) {
    return getVesselLoadFactor(vessel);
  }

  private static float getVesselLoadFactor(final Entity vessel) {
    if (!areVesselLimitsUsable()) {
      return 1;
    }

    try {
      final float factor = VesselCompat.getLoadFactor(vessel);
      if (factor <= 0) {
        final WeightSyncPacket data = vessel.level().isClientSide() ? WeightBarClientData.get() : null;
        if (data != null && data.cart() != null) {
          tellHelmWhyStill(vessel, Component.translatable(Constants.MOD_ID + ".message.vessel_overloaded",
              Math.round(data.cart().load()), Math.round(data.cart().overburdenedLoad())));
        }
      }
      return factor;
    } catch (LinkageError | RuntimeException e) {
      turnAlekiShipsOff(e);
      return 1;
    }
  }

  // Only on the client of the player at the helm, while they press a key, so the server sends nothing of its own
  private static void tellHelmWhyStill(final Entity vessel, final Component message) {
    if (vessel.level().isClientSide() && vessel.isControlledByLocalInstance() && ModList.get().isLoaded("alekiships")) {
      try {
        if (VesselCompat.hasHelmInput(vessel)) {
          tellHelm(vessel, message);
        }
      } catch (LinkageError | RuntimeException e) {
        turnAlekiShipsOff(e);
      }
    }
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
