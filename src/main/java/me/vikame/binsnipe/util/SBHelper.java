package me.vikame.binsnipe.util;

import me.doubledutch.lazyjson.LazyObject;
import me.vikame.binsnipe.Constants;

public class SBHelper {

  public static int calculateWithTaxes(int price) {
    float taxes = price * Constants.AUCTION_CREATION_TAX;
    taxes += Math.min(Constants.MAX_COLLECTION_TAX, price * Constants.AUCTION_COLLECTION_TAX);

    return price - (int) Math.ceil(taxes);
  }

  public static boolean isCategory(LazyObject object, String category) {
    return object.getString("category").equalsIgnoreCase(category);
  }

  public static boolean isExistingBIN(LazyObject object) {
    return object.has("bin")
        && !object.getBoolean("claimed")
        && object.getLong("end") > System.currentTimeMillis();
  }

  public static String stripInvalidChars(String itemName) {
    return Constants.STRIP_COLOR_PATTERN.matcher(itemName).replaceAll("").replaceAll("\\p{C}}", "");
  }

  public static boolean isRecombed(LazyObject object) {
    return isRecombed(object.getString("item_lore"));
  }

  private static boolean isRecombed(String itemLore) {
    return itemLore.endsWith("§ka");
  }

  public static boolean isUsedCakeSoul(LazyObject object) {
    return isUsedCakeSoul(object.getString("item_lore"));
  }

  private static boolean isUsedCakeSoul(String itemLore) {
    return itemLore.startsWith("§dCake Soul");
  }

  public static boolean isFurniture(LazyObject object) {
    return isFurniture(object.getString("item_lore"));
  }

  private static boolean isFurniture(String itemLore) {
    return itemLore.startsWith("§8Furniture");
  }

  public static boolean isCosmetic(LazyObject object) {
    return isCosmetic(object.getString("item_lore"));
  }

  private static boolean isCosmetic(String itemLore) {
    return itemLore.endsWith("COSMETIC") || itemLore.endsWith("COSMETIC §ka");
  }
}
