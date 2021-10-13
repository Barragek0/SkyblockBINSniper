package me.vikame.binsnipe;

import java.util.regex.Pattern;

public class Constants {

  static final String AUCTIONS_ENDPOINT =
      "https://api.hypixel.net/skyblock/auctions"; // ?page={num}
  static final String NOTENOUGHUPDATES_ENDPOINT =
      "https://moulberry.codes/auction_averages/1day.json"; // auction_averages_lbin for lowest bin,
                                                            // any timeframe can be appended to the
                                                            // end: 1minute.json, 1hour.json,
                                                            // 3hour.json, etc

  public static final float AUCTION_CREATION_TAX = 0.01f;
  public static final float AUCTION_COLLECTION_TAX = 0.01f;
  public static final int MAX_COLLECTION_TAX = 1_000_000;

  public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");
}
