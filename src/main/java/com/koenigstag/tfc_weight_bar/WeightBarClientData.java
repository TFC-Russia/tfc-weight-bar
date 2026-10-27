package com.koenigstag.tfc_weight_bar;

// client side
// last weight state received from the server, read by the HUD bar
public class WeightBarClientData {
  private static WeightSyncPacket current = null;

  public static void update(WeightSyncPacket packet) {
    current = packet;
  }

  public static void reset() {
    current = null;
  }

  public static WeightSyncPacket get() {
    return current;
  }
}
