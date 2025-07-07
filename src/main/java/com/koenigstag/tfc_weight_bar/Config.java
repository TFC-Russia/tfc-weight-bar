package com.koenigstag.tfc_weight_bar;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;

import net.dries007.tfc.common.capabilities.size.Weight;
import net.dries007.tfc.common.capabilities.size.Size;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

  public static void init() {
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
  }

  // Client Config
  private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

  private static final ForgeConfigSpec.BooleanValue ENABLE_UI_BAR = CLIENT_BUILDER
    .comment("Enable weight bar on UI. Default: false")
    .define("enable_ui_bar", false);

  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLOT_NAME_STRINGS = CLIENT_BUILDER
    .comment("A list of slots to check in weight calculation.")
    .defineListAllowEmpty("curios_slots", List.of(), Config::validateSlotName);

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
    .comment("Whether to enable food exhaustion coefficient. Default: true")
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

  static final ForgeConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

  // Client config variables
  public static boolean enableUIBar;
  public static List<ISlotType> curios_slots;

  // Server config variables
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
    if (event.getConfig().getSpec() == CLIENT_SPEC) {
      // Client config
      enableUIBar = ENABLE_UI_BAR.get();
      curios_slots = SLOT_NAME_STRINGS.get().stream()
        .map(slotId -> CuriosApi.getSlot(slotId, getIsClient()).get())
        .collect(Collectors.toList());
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
    }
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

  private static boolean getIsClient() {
    // TODO fix this. Needed in CuriosApi.getSlot(String id, boolean isClient)
    return false;
  }

  private static boolean validateSlotName(final Object obj) {
    return obj instanceof final String slotId
      && CuriosApi.getSlot(slotId, getIsClient()).isPresent();
  }
}
