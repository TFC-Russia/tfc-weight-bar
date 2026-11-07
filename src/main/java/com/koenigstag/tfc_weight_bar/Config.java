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

  // Client Config: Mods > TFC Weight Bar > Config edits it in game, and edits apply at once
  private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

  private static final ForgeConfigSpec.BooleanValue ENABLE_UI_BAR = CLIENT_BUILDER
    .comment("Show the weight as a bar on the HUD. When off, the weight shows above the hotbar every 10 seconds instead. A server can decide this for everyone with force_ui_bar. Default: true")
    .define("enable_ui_bar", true);

  private static final ForgeConfigSpec.EnumValue<BarPosition> BAR_POSITION = CLIENT_BUILDER
    .comment("Where the HUD bar sits: FOOD above the TFC food and thirst bars, HEALTH above the health bar, and above the armour bar while that shows. A server can decide this for everyone with force_bar_position. Default: FOOD")
    .defineEnum("bar_position", BarPosition.FOOD);

  private static final ForgeConfigSpec.BooleanValue SHOW_BAR_NUMBERS = CLIENT_BUILDER
    .comment("Whether the HUD bar shows the weight as numbers, such as 720/1000. A server can decide this for everyone with force_bar_numbers. Default: true")
    .define("show_bar_numbers", true);

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

  // The server sends its config to every client that joins, so these settle the HUD for all players on it
  private static final ForgeConfigSpec.EnumValue<ForceToggle> FORCE_UI_BAR = SERVER_BUILDER
    .comment("Decides enable_ui_bar for every player: CLIENT leaves it to each player's client config, ON shows every player the HUD bar, OFF shows every player the weight above the hotbar instead. Default: CLIENT")
    .defineEnum("force_ui_bar", ForceToggle.CLIENT);

  private static final ForgeConfigSpec.EnumValue<ForceBarPosition> FORCE_BAR_POSITION = SERVER_BUILDER
    .comment("Decides bar_position for every player: CLIENT leaves it to each player's client config, FOOD or HEALTH puts every player's bar there. Default: CLIENT")
    .defineEnum("force_bar_position", ForceBarPosition.CLIENT);

  private static final ForgeConfigSpec.EnumValue<ForceToggle> FORCE_BAR_NUMBERS = SERVER_BUILDER
    .comment("Decides show_bar_numbers for every player: CLIENT leaves it to each player's client config, ON or OFF shows or hides the numbers on every player's bar. Default: CLIENT")
    .defineEnum("force_bar_numbers", ForceToggle.CLIENT);

  static final ForgeConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

  // Client config variables
  public static boolean enableUIBar;
  public static BarPosition barPosition = BarPosition.FOOD;
  public static boolean showBarNumbers = true;

  // Server config variables
  public static ForceToggle forceUIBar = ForceToggle.CLIENT;
  public static ForceBarPosition forceBarPosition = ForceBarPosition.CLIENT;
  public static ForceToggle forceBarNumbers = ForceToggle.CLIENT;
  public static List<String> curios_slots = List.of();
  public static boolean enableWeightCalculations;
  public static int maxInvWeight;
  public static int exhaustedWeightPercentage;
  public static int calculateWeightEachNTicks;
  public static boolean enableWeightDebuffs;
  public static boolean enableFoodExhaustion;
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
      enableUIBar = ENABLE_UI_BAR.get();
      barPosition = BAR_POSITION.get();
      showBarNumbers = SHOW_BAR_NUMBERS.get();
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

      veryLightWeightInt = VERY_LIGHT_WEIGHT.get();
      lightWeightInt = LIGHT_WEIGHT.get();
      mediumWeightInt = MEDIUM_WEIGHT.get();
      heavyWeightInt = HEAVY_WEIGHT.get();
      veryHeavyWeightInt = VERY_HEAVY_WEIGHT.get();

      // Slot ids rather than Curios slot types, so that loading the config does not need Curios
      curios_slots = List.copyOf(SLOT_NAME_STRINGS.get());

      forceUIBar = FORCE_UI_BAR.get();
      forceBarPosition = FORCE_BAR_POSITION.get();
      forceBarNumbers = FORCE_BAR_NUMBERS.get();
    }
  }

  /**
   * Whether the weight shows as the HUD bar rather than above the hotbar: the server's force_ui_bar, else the player's
   * enable_ui_bar.
   */
  public static boolean isUIBarShown() {
    return switch (forceUIBar) {
      case CLIENT -> enableUIBar;
      case ON -> true;
      case OFF -> false;
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

  public static void setEnableUIBar(final boolean value) {
    enableUIBar = value;
    ENABLE_UI_BAR.set(value);
    CLIENT_SPEC.save();
  }

  public static void setBarPosition(final BarPosition value) {
    barPosition = value;
    BAR_POSITION.set(value);
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
