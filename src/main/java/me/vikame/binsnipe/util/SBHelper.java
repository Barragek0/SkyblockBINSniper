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

  /* For the following methods, we opt to do string manipulation to
   * determine different aspects of the item rather than handling the
   * gzipped NBT data within the 'item_bytes' field of the JSON response.
   *
   * While the gzipped NBT data would contain far more accurate information,
   * speed is the name-of-the-game for a BIN sniper, so taking the time
   * to un-gzip and parse the NBT data is not a worthwhile endeavor.
   */
  public static String stripItemName(LazyObject object) {
    return stripItemName(object.getString("item_name"));
  }

  public static String stripItemName(String itemName) {
    String ret = itemName;

    ret = Constants.STRIP_COLOR_PATTERN.matcher(ret).replaceAll("");

    for (String text : Constants.IGNORED_TEXT) {
      ret = ret.replace(text, "");
    }

    if (Config.STRIP_STARS) {
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
