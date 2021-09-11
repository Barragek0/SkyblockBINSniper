package me.vikame.binsnipe.util;

import me.doubledutch.lazyjson.LazyObject;
import me.vikame.binsnipe.Config;
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
    return object.has("bin") && !object.getBoolean("claimed")
        && object.getLong("end") > System.currentTimeMillis();
  }

  public static String stripInvalidChars(String itemName) {
    return itemName.replaceAll("\\p{C}}", "");
  }

  public static String stripItemName(LazyObject object) {
    return stripItemName(object.getString("item_name"));
  }

  public static String stripItemName(String itemName) {
    String ret = itemName;

    ret = Constants.STRIP_COLOR_PATTERN.matcher(ret).replaceAll("");

    for (String text : Constants.IGNORED_TEXT) {
      ret = ret.replace(text, "");
    }

    if (Config.IGNORE_STARS) {
      ret = ret.replace(" ✪", "").replace("✪", "");
    } else {
      ret = ret.replace("✪", "*");
    }

    ret = ret.replaceFirst("\\[Lvl \\d*] ", "");
    return ret.trim();
  }

  public static boolean isRecombed(LazyObject object) {
    return isRecombed(object.getString("item_lore"));
  }

  public static boolean isRecombed(String itemLore) {
    return itemLore.endsWith("§ka");
  }

  public static boolean isUsedCakeSoul(LazyObject object) {
    return isUsedCakeSoul(object.getString("item_lore"));
  }

  public static boolean isUsedCakeSoul(String itemLore) {
    return itemLore.startsWith("§dCake Soul");
  }

  public static boolean isFurniture(LazyObject object) {
    return isFurniture(object.getString("item_lore"));
  }

  public static boolean isFurniture(String itemLore) {
    return itemLore.startsWith("§8Furniture");
  }

  public static boolean isCosmetic(LazyObject object) {
    return isCosmetic(object.getString("item_lore"));
  }

  public static boolean isCosmetic(String itemLore) {
    return itemLore.endsWith("COSMETIC") || itemLore.endsWith("COSMETIC §ka");
  }

}
