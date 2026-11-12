package com.koenigstag.tfc_weight_bar;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The animals that stand still with a cart loaded past its limit, under overloaded_cart = STOP. The server takes the
 * animal's speed and jump to nothing, and both reach every client, which moves a ridden animal by them, so this holds
 * whatever mods a player has. An animal no one rides the server moves itself, and a lead would drag it: that one also
 * loses whatever motion it has, every tick, so the lead breaks rather than pull it along.
 *
 * <p>The modifiers are transient, so a world saved meanwhile keeps none. Each tick of a level the carts near its
 * players are checked again, and an animal not found pulling an overloaded cart gets its speed back.
 */
final class CartHolds {
  private static final ResourceLocation MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "overloaded_cart");

  // Each held animal and the rider last told why, or null while no one has been; weak, so an animal that leaves the
  // world goes with it
  private static final Map<LivingEntity, Told> HELD = new WeakHashMap<>();

  // The animals found pulling an overloaded cart in the level being ticked
  private static final Set<LivingEntity> FOUND = new HashSet<>();

  private CartHolds() {}

  private record Told(@Nullable Entity rider) {}

  static void beginTick() {
    FOUND.clear();
  }

  /**
   * Holds this animal still with the cart it cannot pull, for this tick. The players around read why once, unless a
   * player is loading the cart, whose screen would hide it, and again when a player gets on the animal.
   */
  static void hold(final LivingEntity animal, final Entity cart, final CartLoad load, final boolean beingLoaded) {
    FOUND.add(animal);

    if (!HELD.containsKey(animal)) {
      addModifier(animal, Attributes.MOVEMENT_SPEED);
      addModifier(animal, Attributes.JUMP_STRENGTH);
      HELD.put(animal, null);
    }

    final Told told = HELD.get(animal);
    final Entity rider = animal.getControllingPassenger();
    if (!beingLoaded && (told == null || rider instanceof Player && rider != told.rider())) {
      WeightBarHelpers.announceCartOverloaded(animal, cart, load, false);
      HELD.put(animal, new Told(rider));
    }

    if (rider == null) {
      final Vec3 motion = animal.getDeltaMovement();
      animal.setDeltaMovement(0, motion.y, 0);
    }
  }

  /**
   * Gives back their speed to the animals of this level that were not held this tick.
   */
  static void endTick(final ServerLevel level) {
    for (final Iterator<LivingEntity> held = HELD.keySet().iterator(); held.hasNext(); ) {
      final LivingEntity animal = held.next();

      if (animal.level() == level && !FOUND.contains(animal)) {
        removeModifier(animal, Attributes.MOVEMENT_SPEED);
        removeModifier(animal, Attributes.JUMP_STRENGTH);
        held.remove();
      }
    }
  }

  private static void addModifier(final LivingEntity animal, final Holder<Attribute> attribute) {
    final AttributeInstance instance = animal.getAttribute(attribute);

    if (instance != null && !instance.hasModifier(MODIFIER_ID)) {
      instance.addTransientModifier(
          new AttributeModifier(MODIFIER_ID, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
  }

  private static void removeModifier(final LivingEntity animal, final Holder<Attribute> attribute) {
    final AttributeInstance instance = animal.getAttribute(attribute);

    if (instance != null) {
      instance.removeModifier(MODIFIER_ID);
    }
  }
}
