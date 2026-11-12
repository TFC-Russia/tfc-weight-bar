package com.koenigstag.tfc_weight_bar;

import com.alekiponi.alekiships.common.entity.vehicle.AbstractVehicle;
import com.alekiponi.alekiships.common.entity.vehicle.RowboatEntity;
import com.alekiponi.alekiships.common.entity.vehicle.SloopEntity;
import com.alekiponi.alekiships.common.entity.vehiclehelper.compartment.AbstractCompartmentEntity;
import com.alekiponi.alekiships.common.entity.vehiclehelper.compartment.EmptyCompartmentEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Everything that touches AlekiShips, whose vessels Firmaciv builds its own on. It is optional, so call this only
 * through {@link WeightBarHelpers}: loading this class without it fails. Firmaciv's kayak and canoe are told apart by
 * class name, so Firmaciv itself is not needed.
 *
 * <p>A vessel's load is what its compartments hold, weighed as a player's inventory, containers with what they hold, and
 * the weight the players aboard carry. Each kind of vessel has a limit of its own, and an unknown kind has none.
 */
public final class VesselCompat {
  private static final String KAYAK = "com.alekiponi.firmaciv.common.entity.vehicle.KayakEntity";
  private static final String CANOE = "com.alekiponi.firmaciv.common.entity.vehicle.CanoeEntity";

  // Server only: each vessel's load, weighed again every calculate_weight_each_n_ticks rather than every tick it
  // moves; weak, so a vessel that leaves the world goes with it
  private static final Map<Entity, Weighed> WEIGHED = new WeakHashMap<>();

  private record Weighed(long at, @Nullable CartLoad load) {}

  private VesselCompat() {}

  /**
   * The load of the vessel this player is at the helm of, or null when they are at no helm or the vessel has no limit.
   */
  @Nullable
  public static CartLoad getHelmLoad(final Player player) {
    if (player.getRootVehicle() instanceof AbstractVehicle vessel && vessel.getControllingPassenger() == player) {
      return getLoad(vessel);
    }
    return null;
  }

  /**
   * How much of its thrust this vessel keeps, 1 for all of it, by its load under overloaded_vessel. The server weighs
   * the vessel now and then; the client of the player at its helm goes by the load the server last sent them, as their
   * cart bar shows it; any other client keeps it all, since the server moves the vessel for them.
   */
  public static float getLoadFactor(final Entity entity) {
    if (!(entity instanceof AbstractVehicle vessel)) {
      return 1;
    }

    final CartLoad load;
    if (vessel.level().isClientSide()) {
      final WeightSyncPacket data = WeightBarClientData.get();
      load = vessel.isControlledByLocalInstance() && data != null ? data.cart() : null;
    } else {
      load = getWeighedLoad(vessel);
    }

    if (load == null || load.overburdenedLoad() <= 0) {
      return 1;
    }
    if (Config.overloadedVessel == Config.OverloadedVessel.SLOW) {
      return Math.max(0, Math.min(1, 1 - load.load() / load.overburdenedLoad()));
    }
    return load.load() > load.overburdenedLoad() ? 0 : 1;
  }

  /**
   * Whether the player at this vessel's helm is pressing a key to row or steer it.
   */
  public static boolean hasHelmInput(final Entity entity) {
    if (entity instanceof AbstractVehicle vessel) {
      final EmptyCompartmentEntity helm = vessel.getPilotCompartment();
      return helm != null
          && (helm.getInputUp() || helm.getInputDown() || helm.getInputLeft() || helm.getInputRight());
    }
    return false;
  }

  @Nullable
  private static CartLoad getWeighedLoad(final AbstractVehicle vessel) {
    final long now = vessel.level().getGameTime();
    final Weighed weighed = WEIGHED.get(vessel);

    if (weighed != null && now >= weighed.at() && now - weighed.at() < Config.calculateWeightEachNTicks) {
      return weighed.load();
    }

    final CartLoad load = getLoad(vessel);
    WEIGHED.put(vessel, new Weighed(now, load));
    return load;
  }

  @Nullable
  private static CartLoad getLoad(final AbstractVehicle vessel) {
    final int maxLoad = getMaxLoad(vessel);
    if (maxLoad <= 0) {
      return null;
    }

    int weight = 0;
    for (final AbstractCompartmentEntity compartment : vessel.collectCompartments()) {
      if (compartment instanceof Container container) {
        weight += WeightBarHelpers.calculateContainerWeight(container);
      }
    }
    for (final Player aboard : vessel.collectPlayerPassengers()) {
      weight += WeightBarHelpers.calculatePlayerFullWeight(aboard);
    }

    return new CartLoad(weight, (float) (maxLoad * Config.getExhaustedWeightCoefficient()), maxLoad);
  }

  private static int getMaxLoad(final AbstractVehicle vessel) {
    if (vessel instanceof SloopEntity) {
      return Config.vesselSloopMaxLoad;
    }
    if (vessel instanceof RowboatEntity) {
      return Config.vesselRowboatMaxLoad;
    }
    if (isA(vessel, CANOE)) {
      return Config.vesselCanoeMaxLoad;
    }
    if (isA(vessel, KAYAK)) {
      return Config.vesselKayakMaxLoad;
    }
    return 0;
  }

  private static boolean isA(final Object object, final String className) {
    for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
      if (type.getName().equals(className)) {
        return true;
      }
    }
    return false;
  }
}
