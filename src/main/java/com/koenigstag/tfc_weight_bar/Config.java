package com.koenigstag.tfc_weight_bar;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import net.dries007.tfc.common.capabilities.size.Weight;
import net.dries007.tfc.common.capabilities.size.Size;

import java.util.List;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

  public static void init() {
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
  }

  /**
   * How the player sees their weight.
   */
  public enum WeightDisplay {
    /** The bar on the HUD */
    HUD,
    /** A message above the hotbar every 10 seconds */
    TEXT,
    /** Neither: the weight only shows in item tooltips */
    OFF
  }

  /**
   * A server's say over weight_display: leave it to each player, or show every player the weight one way.
   */
  public enum ForceWeightDisplay {
    CLIENT,
    HUD,
    TEXT,
    OFF
  }

  /**
   * Where the HUD bar sits.
   */
  public enum BarPosition {
    /** Above the TFC food and thirst bars, on the right */
    FOOD,
    /** Above the health bar, on the left */
    HEALTH
  }

  /**
   * A server's say over a client display option: leave it to each player, or turn it on or off for everyone.
   */
  public enum ForceToggle {
    CLIENT,
    ON,
    OFF
  }

  /**
   * A server's say over where the HUD bar sits: leave it to each player, or put every player's bar in one place.
   */
  public enum ForceBarPosition {
    CLIENT,
    FOOD,
    HEALTH
  }

  /**
   * What an animal does with a cart loaded past its limit: let go of it, or keep it and stand still.
   */
  public enum OverloadedCart {
    REFUSE,
    STOP
  }

  /**
   * How a vessel's load holds it back: not at all until past its limit, where it gets no thrust, or more the more it is
   * loaded, down to none at its limit.
   */
  public enum OverloadedVessel {
    STOP,
    SLOW
  }

  // Client Config: Mods > TFC Weight Bar > Config edits it in game, and edits apply at once
  private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

  // Replaces enable_ui_bar of 1.0.x. Its value is not carried over: the config loader drops the key, and every player
  // starts on the HUD bar, false or not, since the message above the hotbar was only a stopgap before the bar
  private static final ForgeConfigSpec.EnumValue<WeightDisplay> WEIGHT_DISPLAY = CLIENT_BUILDER
    .comment("How you see your weight: HUD as a bar on the HUD, TEXT as a message above the hotbar every 10 seconds, OFF not at all, apart from item tooltips. A server can decide this for everyone with force_weight_display. Default: HUD")
    .defineEnum("weight_display", WeightDisplay.HUD);

  private static final ForgeConfigSpec.EnumValue<BarPosition> BAR_POSITION = CLIENT_BUILDER
    .comment("Where the HUD bar sits: FOOD above the TFC food and thirst bars, HEALTH above the health bar, and above the armour bar while that shows. A server can decide this for everyone with force_bar_position. Default: FOOD")
    .defineEnum("bar_position", BarPosition.FOOD);

  private static final ForgeConfigSpec.BooleanValue SHOW_BAR_NUMBERS = CLIENT_BUILDER
    .comment("Whether the HUD bar shows the weight as numbers, such as 720/1000. A server can decide this for everyone with force_bar_numbers. Default: true")
    .define("show_bar_numbers", true);

  private static final ForgeConfigSpec.BooleanValue SHOW_CART_BAR = CLIENT_BUILDER
    .comment("Whether a second bar over the weight bar shows the load of the cart you pull, with TFC Astikor Carts or Pull Cart. A server can decide this for everyone with force_cart_bar. Default: true")
    .define("show_cart_bar", true);

  static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

  // Server Config
  private static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

  private static final ForgeConfigSpec.BooleanValue ENABLE_WEIGHT_SYSTEM = SERVER_BUILDER
    .comment("Enable the weight system on server. Default: true")
    .define("enable_weight_calculations", true);

  private static final ForgeConfigSpec.BooleanValue ENABLE_WEIGHT_DEBUFFS = SERVER_BUILDER
    .comment("Whether to enable weight debuffs. Default: true")
    .define("enable_weight_debuffs", true);

  private static final ForgeConfigSpec.BooleanValue ENABLE_FOOD_EXHAUSTION = SERVER_BUILDER
    .comment("Whether an exhausting load makes the player hungrier, up to twice TFC's passive hunger at the maximum weight. Default: true")
    .define("enable_food_exhaustion", true);

  private static final ForgeConfigSpec.IntValue CALCULATE_WEIGHT_EACH_N_TICKS = SERVER_BUILDER
    .comment("Calculate weight every N ticks. Default: 20 ticks = 1 sec")
    .defineInRange("calculate_weight_each_n_ticks", 20, 1, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue MAX_INV_WEIGHT = SERVER_BUILDER
    .comment("Maximum inventory weight. Default: 1000")
    .defineInRange("max_inv_weight", 1000, 0, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue EXHAUSTED_WEIGHT_PERCENTAGE = SERVER_BUILDER
    .comment("Percentage of max weight that is considered exhausted. Default: 80")
    .defineInRange("exhausted_weight_percentage", 80, 0, 100);

  private static final ForgeConfigSpec.BooleanValue HEALTH_AFFECTS_WEIGHT_CAPACITY = SERVER_BUILDER
    .comment("Whether player health affects maximum weight capacity. Default: true")
    .define("health_affects_weight_capacity", true);

  private static final ForgeConfigSpec.DoubleValue MIN_HEALTH_WEIGHT_PERCENTAGE = SERVER_BUILDER
    .comment("Minimum weight capacity percentage when at low health. Default: 0.1 (10%)")
    .defineInRange("min_health_weight_percentage", 0.1, 0.0, 1.0);

  private static final ForgeConfigSpec.IntValue VERY_LIGHT_WEIGHT = SERVER_BUILDER
    .comment("Weight of very light items. Default: 1")
    .defineInRange("very_light_item_weight", 1, 0, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue LIGHT_WEIGHT = SERVER_BUILDER
    .comment("Weight of light items. Default: 4")
    .defineInRange("light_item_weight", 4, 0, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue MEDIUM_WEIGHT = SERVER_BUILDER
    .comment("Weight of medium items. Default: 16")
    .defineInRange("medium_item_weight", 16, 0, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue HEAVY_WEIGHT = SERVER_BUILDER
    .comment("Weight of heavy items. Default: 32")
    .defineInRange("heavy_item_weight", 32, 0, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue VERY_HEAVY_WEIGHT = SERVER_BUILDER
    .comment("Weight of very heavy items. Default: 64")
    .defineInRange("very_heavy_item_weight", 64, 0, Integer.MAX_VALUE);

  // The ids are not checked against Curios: its slots may not be loaded yet when the config is, and an id that no slot
  // has simply matches nothing
  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLOT_NAME_STRINGS = SERVER_BUILDER
    .comment("Curios slot types whose items count towards the weight, such as \"back\" or \"belt\". Empty counts every slot. Default: []")
    .defineListAllowEmpty("curios_slots", List.of(), obj -> obj instanceof String);

  private static final ForgeConfigSpec.BooleanValue ENABLE_CART_LIMITS = SERVER_BUILDER
    .comment("Whether carts have a limit of weight: an animal lets go of a cart loaded past it, and tells the players around, and a player pulling such a cart on foot is burdened as by a load they carry. A cart's load is weighed as a player's inventory, containers with what they hold. With TFC Astikor Carts this is the supply cart, up to astikor_supply_cart_max_load, and that mod's own debuffs for its cargo stop; its animal cart keeps that mod's thresholds. Off, TFC Astikor Carts weighs and burdens as it does by itself. Default: true")
    .define("enable_cart_limits", true);

  private static final ForgeConfigSpec.BooleanValue STOP_OVERBURDENED_BOATS = SERVER_BUILDER
    .comment("Whether a player who is Overburdened, by weight or by TFC's huge and very heavy items, cannot row a boat: the boat they steer neither moves nor turns while they are, and drifts as the water takes it. Covers the vanilla boat and the boats built on it, TFC's among them. Default: true")
    .define("stop_overburdened_boats", true);

  private static final ForgeConfigSpec.BooleanValue ENABLE_VESSEL_LIMITS = SERVER_BUILDER
    .comment("With AlekiShips, and Firmaciv's vessels built on it: whether a vessel has a limit of weight. Its load is what its compartments hold, chests, barrels and vessels with what they hold, and the weight the players aboard carry, all weighed as a player's inventory. The player at the helm sees it as the cart bar, and overloaded_vessel says how the load holds the vessel back. Default: true")
    .define("enable_vessel_limits", true);

  private static final ForgeConfigSpec.EnumValue<OverloadedVessel> OVERLOADED_VESSEL = SERVER_BUILDER
    .comment("Under enable_vessel_limits, how a vessel's load holds it back: STOP rows and sails as ever up to the limit, and past it gets no thrust; SLOW gets less thrust the more it is loaded, as much less as the load is a share of the limit, and none at it. Default: STOP")
    .defineEnum("overloaded_vessel", OverloadedVessel.STOP);

  private static final ForgeConfigSpec.IntValue VESSEL_KAYAK_MAX_LOAD = SERVER_BUILDER
    .comment("Under enable_vessel_limits: the weight a Firmaciv kayak can carry. Default: 2000")
    .defineInRange("vessel_kayak_max_load", 2000, 1, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue VESSEL_CANOE_MAX_LOAD = SERVER_BUILDER
    .comment("Under enable_vessel_limits: the weight a Firmaciv canoe can carry. Default: 5000")
    .defineInRange("vessel_canoe_max_load", 5000, 1, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue VESSEL_ROWBOAT_MAX_LOAD = SERVER_BUILDER
    .comment("Under enable_vessel_limits: the weight a rowboat can carry, AlekiShips' or Firmaciv's. Default: 10000")
    .defineInRange("vessel_rowboat_max_load", 10000, 1, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue VESSEL_SLOOP_MAX_LOAD = SERVER_BUILDER
    .comment("Under enable_vessel_limits: the weight a sloop can carry, AlekiShips' or Firmaciv's; its load holds back what its sails pull. Default: 40000")
    .defineInRange("vessel_sloop_max_load", 40000, 1, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.EnumValue<OverloadedCart> OVERLOADED_CART = SERVER_BUILDER
    .comment("Under enable_cart_limits, what an animal does with a cart loaded past its limit: REFUSE lets go of the cart, STOP keeps it and stands still, ridden or led, until the cart is under its limit again or taken off it. The server holds the animal, so STOP works whatever mods the players have. Default: REFUSE")
    .defineEnum("overloaded_cart", OverloadedCart.REFUSE);

  private static final ForgeConfigSpec.IntValue ASTIKOR_SUPPLY_CART_MAX_LOAD = SERVER_BUILDER
    .comment("With TFC Astikor Carts: the weight a supply cart can hold before the cart is overloaded, counted as in a player's inventory. It is exhausting from exhausted_weight_percentage of this. Default: 10000")
    .defineInRange("astikor_supply_cart_max_load", 10000, 1, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue PULL_CART_MAX_LOAD = SERVER_BUILDER
    .comment("With Pull Cart: the weight a cart's chest can hold before the cart is overloaded, counted as in a player's inventory. It is exhausting from exhausted_weight_percentage of this. Default: 5000")
    .defineInRange("pull_cart_max_load", 5000, 1, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.BooleanValue COUNT_CART_RIDERS = SERVER_BUILDER
    .comment("With TFC Astikor Carts: whether a player sitting on a cart adds to its load. On a supply cart under enable_cart_limits that is the weight they carry, as their own bar shows it; on an animal cart, from 1.1.9, their body weighed as that mod weighs a passenger and what they carry weighed as cargo. Such a player also sees the cart bar. Default: false")
    .define("count_cart_riders", false);

  private static final ForgeConfigSpec.EnumValue<Size> LARGE_ITEM_SIZE = SERVER_BUILDER
    .comment("The smallest TFC size of item that counts as large for astikor_max_large_item_slots and pull_cart_max_large_item_slots. Default: LARGE")
    .defineEnum("large_item_size", Size.LARGE);

  private static final ForgeConfigSpec.IntValue ASTIKOR_MAX_LARGE_ITEM_SLOTS = SERVER_BUILDER
    .comment("With TFC Astikor Carts: how many slots of a cart may hold large items before the cart is overloaded, whatever its load. 0 sets no such limit. Default: 0")
    .defineInRange("astikor_max_large_item_slots", 0, 0, Integer.MAX_VALUE);

  private static final ForgeConfigSpec.IntValue PULL_CART_MAX_LARGE_ITEM_SLOTS = SERVER_BUILDER
    .comment("With Pull Cart: how many slots of a cart's chest may hold large items before the cart is overloaded, whatever its load. 0 sets no such limit. Default: 0")
    .defineInRange("pull_cart_max_large_item_slots", 0, 0, Integer.MAX_VALUE);

  // The server sends its config to every client that joins, so these settle the HUD for all players on it
  private static final ForgeConfigSpec.EnumValue<ForceWeightDisplay> FORCE_WEIGHT_DISPLAY = SERVER_BUILDER
    .comment("Decides weight_display for every player: CLIENT leaves it to each player's client config, HUD, TEXT or OFF shows every player the weight that way. Default: CLIENT")
    .defineEnum("force_weight_display", ForceWeightDisplay.CLIENT);

  private static final ForgeConfigSpec.EnumValue<ForceBarPosition> FORCE_BAR_POSITION = SERVER_BUILDER
    .comment("Decides bar_position for every player: CLIENT leaves it to each player's client config, FOOD or HEALTH puts every player's bar there. Default: CLIENT")
    .defineEnum("force_bar_position", ForceBarPosition.CLIENT);

  private static final ForgeConfigSpec.EnumValue<ForceToggle> FORCE_BAR_NUMBERS = SERVER_BUILDER
    .comment("Decides show_bar_numbers for every player: CLIENT leaves it to each player's client config, ON or OFF shows or hides the numbers on every player's bar. Default: CLIENT")
    .defineEnum("force_bar_numbers", ForceToggle.CLIENT);

  private static final ForgeConfigSpec.EnumValue<ForceToggle> FORCE_CART_BAR = SERVER_BUILDER
    .comment("Decides show_cart_bar for every player: CLIENT leaves it to each player's client config, ON or OFF shows or hides every player's cart bar. Default: CLIENT")
    .defineEnum("force_cart_bar", ForceToggle.CLIENT);

  static final ForgeConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

  // Client config variables
  public static WeightDisplay weightDisplay = WeightDisplay.HUD;
  public static BarPosition barPosition = BarPosition.FOOD;
  public static boolean showBarNumbers = true;
  public static boolean showCartBar = true;

  // Server config variables
  public static ForceWeightDisplay forceWeightDisplay = ForceWeightDisplay.CLIENT;
  public static ForceBarPosition forceBarPosition = ForceBarPosition.CLIENT;
  public static ForceToggle forceBarNumbers = ForceToggle.CLIENT;
  public static ForceToggle forceCartBar = ForceToggle.CLIENT;
  public static List<String> curios_slots = List.of();
  public static boolean enableWeightCalculations;
  public static int maxInvWeight;
  public static int exhaustedWeightPercentage;
  public static int calculateWeightEachNTicks;
  public static boolean enableWeightDebuffs;
  public static boolean enableFoodExhaustion;
  public static boolean enableCartLimits;
  public static boolean stopOverburdenedBoats = true;
  public static boolean enableVesselLimits = true;
  public static OverloadedVessel overloadedVessel = OverloadedVessel.STOP;
  public static int vesselKayakMaxLoad;
  public static int vesselCanoeMaxLoad;
  public static int vesselRowboatMaxLoad;
  public static int vesselSloopMaxLoad;
  public static OverloadedCart overloadedCart = OverloadedCart.REFUSE;
  public static int astikorSupplyCartMaxLoad;
  public static int pullCartMaxLoad;
  public static boolean countCartRiders;
  public static Size largeItemSize = Size.LARGE;
  public static int astikorMaxLargeItemSlots;
  public static int pullCartMaxLargeItemSlots;
  public static boolean healthAffectsWeightCapacity;
  public static double minHealthWeightPercentage;

  public static int veryLightWeightInt;
  public static int lightWeightInt;
  public static int mediumWeightInt;
  public static int heavyWeightInt;
  public static int veryHeavyWeightInt;

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    // Unloading comes when a world closes, and its values can no longer be read by then
    if (event instanceof ModConfigEvent.Unloading) {
      return;
    }

    if (event.getConfig().getSpec() == CLIENT_SPEC) {
      // Client config
      weightDisplay = WEIGHT_DISPLAY.get();
      barPosition = BAR_POSITION.get();
      showBarNumbers = SHOW_BAR_NUMBERS.get();
      showCartBar = SHOW_CART_BAR.get();
    } else if (event.getConfig().getSpec() == SERVER_SPEC) {
      // Server config
      enableWeightCalculations = ENABLE_WEIGHT_SYSTEM.get();
      maxInvWeight = MAX_INV_WEIGHT.get();
      exhaustedWeightPercentage = EXHAUSTED_WEIGHT_PERCENTAGE.get();
      healthAffectsWeightCapacity = HEALTH_AFFECTS_WEIGHT_CAPACITY.get();
      minHealthWeightPercentage = MIN_HEALTH_WEIGHT_PERCENTAGE.get();
      calculateWeightEachNTicks = CALCULATE_WEIGHT_EACH_N_TICKS.get();
      enableWeightDebuffs = ENABLE_WEIGHT_DEBUFFS.get();
      enableFoodExhaustion = ENABLE_FOOD_EXHAUSTION.get();
      enableCartLimits = ENABLE_CART_LIMITS.get();
      stopOverburdenedBoats = STOP_OVERBURDENED_BOATS.get();
      enableVesselLimits = ENABLE_VESSEL_LIMITS.get();
      overloadedVessel = OVERLOADED_VESSEL.get();
      vesselKayakMaxLoad = VESSEL_KAYAK_MAX_LOAD.get();
      vesselCanoeMaxLoad = VESSEL_CANOE_MAX_LOAD.get();
      vesselRowboatMaxLoad = VESSEL_ROWBOAT_MAX_LOAD.get();
      vesselSloopMaxLoad = VESSEL_SLOOP_MAX_LOAD.get();
      overloadedCart = OVERLOADED_CART.get();
      astikorSupplyCartMaxLoad = ASTIKOR_SUPPLY_CART_MAX_LOAD.get();
      pullCartMaxLoad = PULL_CART_MAX_LOAD.get();
      countCartRiders = COUNT_CART_RIDERS.get();
      largeItemSize = LARGE_ITEM_SIZE.get();
      astikorMaxLargeItemSlots = ASTIKOR_MAX_LARGE_ITEM_SLOTS.get();
      pullCartMaxLargeItemSlots = PULL_CART_MAX_LARGE_ITEM_SLOTS.get();

      veryLightWeightInt = VERY_LIGHT_WEIGHT.get();
      lightWeightInt = LIGHT_WEIGHT.get();
      mediumWeightInt = MEDIUM_WEIGHT.get();
      heavyWeightInt = HEAVY_WEIGHT.get();
      veryHeavyWeightInt = VERY_HEAVY_WEIGHT.get();

      // Slot ids rather than Curios slot types, so that loading the config does not need Curios
      curios_slots = List.copyOf(SLOT_NAME_STRINGS.get());

      forceWeightDisplay = FORCE_WEIGHT_DISPLAY.get();
      forceBarPosition = FORCE_BAR_POSITION.get();
      forceBarNumbers = FORCE_BAR_NUMBERS.get();
      forceCartBar = FORCE_CART_BAR.get();
    }
  }

  /**
   * How the player sees their weight: the server's force_weight_display, else the player's weight_display.
   */
  public static WeightDisplay getWeightDisplay() {
    return switch (forceWeightDisplay) {
      case CLIENT -> weightDisplay;
      case HUD -> WeightDisplay.HUD;
      case TEXT -> WeightDisplay.TEXT;
      case OFF -> WeightDisplay.OFF;
    };
  }

  /**
   * Where the HUD bar sits: the server's force_bar_position, else the player's bar_position.
   */
  public static BarPosition getBarPosition() {
    return switch (forceBarPosition) {
      case CLIENT -> barPosition;
      case FOOD -> BarPosition.FOOD;
      case HEALTH -> BarPosition.HEALTH;
    };
  }

  /**
   * Whether the HUD bar shows the weight as numbers: the server's force_bar_numbers, else the player's show_bar_numbers.
   */
  public static boolean areBarNumbersShown() {
    return switch (forceBarNumbers) {
      case CLIENT -> showBarNumbers;
      case ON -> true;
      case OFF -> false;
    };
  }

  // The config screen's setters: the field changes at once, so the HUD follows without waiting for Forge to notice
  // the saved file

  public static void setWeightDisplay(final WeightDisplay value) {
    weightDisplay = value;
    WEIGHT_DISPLAY.set(value);
    CLIENT_SPEC.save();
  }

  public static void setBarPosition(final BarPosition value) {
    barPosition = value;
    BAR_POSITION.set(value);
    CLIENT_SPEC.save();
  }

  public static void setShowCartBar(final boolean value) {
    showCartBar = value;
    SHOW_CART_BAR.set(value);
    CLIENT_SPEC.save();
  }

  public static void setShowBarNumbers(final boolean value) {
    showBarNumbers = value;
    SHOW_BAR_NUMBERS.set(value);
    CLIENT_SPEC.save();
  }

  /**
   * Whether the items in a Curios slot type count towards the weight: every slot does while curios_slots is empty.
   */
  public static boolean isCuriosSlotWeighed(final String slotId) {
    return curios_slots.isEmpty() || curios_slots.contains(slotId);
  }

  /**
   * Whether the cart bar shows: the server's force_cart_bar, else the player's show_cart_bar.
   */
  public static boolean isCartBarShown() {
    return switch (forceCartBar) {
      case CLIENT -> showCartBar;
      case ON -> true;
      case OFF -> false;
    };
  }

  public static double getExhaustedWeightCoefficient() {
    return (double) exhaustedWeightPercentage / (double) 100;
  }

  public static String getNormalBarColor() {
    return "gray";
  }

  public static String getExhaustedBarColor() {
    return "pink";
  }

  public static String getOverburdenedBarColor() {
    return "red";
  }
}
