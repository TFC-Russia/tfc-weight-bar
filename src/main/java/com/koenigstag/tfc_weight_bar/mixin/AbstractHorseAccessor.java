package com.koenigstag.tfc_weight_bar.mixin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A horse's inventory, which the game keeps to itself: the saddle, the armour or a llama's carpet, and the chest of a
 * donkey, mule or llama. TFC's horses, donkeys, mules and llamas are the game's own underneath.
 */
@Mixin(AbstractHorse.class)
public interface AbstractHorseAccessor {

  @Accessor("inventory")
  SimpleContainer tfc_weight_bar$getInventory();
}
