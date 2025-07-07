package com.koenigstag.tfc_weight_bar;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class Tags {
  public static class Items {
    public static TagKey<Item> VERY_LIGHT = create("weight/very_light");
    public static TagKey<Item> LIGHT = create("weight/light");
    public static TagKey<Item> MEDIUM = create("weight/medium");
    public static TagKey<Item> HEAVY = create("weight/heavy");
    public static TagKey<Item> VERY_HEAVY = create("weight/very_heavy");

    private static TagKey<Item> create(String path) {
      return TagKey.create(Registries.ITEM, new ResourceLocation(Constants.MOD_ID, path));
    }
  }
}
