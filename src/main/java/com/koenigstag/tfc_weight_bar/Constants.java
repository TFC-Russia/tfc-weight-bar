package com.koenigstag.tfc_weight_bar;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class Constants {
  public static final String MOD_ID = "tfc_weight_bar";

  public static final Logger LOGGER = LogUtils.getLogger(MOD_ID);

  public static final boolean DEBUG_MODE = false;

  public static void debug(String message) {
    if (DEBUG_MODE) {
      LOGGER.info(message);
    }
  }
}
