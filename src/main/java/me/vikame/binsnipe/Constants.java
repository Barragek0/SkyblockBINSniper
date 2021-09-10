package me.vikame.binsnipe;

import java.util.regex.Pattern;

public class Constants {

  public static final String AUCTIONS_ENDPOINT = "https://api.hypixel.net/skyblock/auctions"; // ?page={num}

  public static final float AUCTION_CREATION_TAX = 0.01f;
  public static final float AUCTION_COLLECTION_TAX = 0.01f;
  public static final int MAX_COLLECTION_TAX = 1_000_000;

  public static final String[] IGNORED_TEXT =
      new String[]{" ✦", "⚚ ", "Stiff ", "Lucky ", "Jerry's ", "Dirty ", "Fabled ", "Suspicious ", "Gilded ", "Warped ", "Withered ", "Bulky ", "Stellar ", "Heated ", "Ambered ", "Fruitful ", "Magnetic ", "Fleet ", "Mithraic ", "Auspicious ", "Refined ", "Headstrong ", "Precise ", "Spiritual ", "Moil ", "Blessed ", "Toil ", "Bountiful ", "Candied ", "Submerged ", "Reinforced ", "Cubic ", "Warped ", "Undead ", "Ridiculous ", "Necrotic ", "Spiked ", "Jaded ", "Loving ", "Perfect ", "Renowned ", "Giant ", "Empowered ", "Ancient ", "Sweet ", "Silky ", "Bloody ", "Shaded ", "Gentle ", "Odd ", "Fast ", "Fair ", "Epic ", "Sharp ", "Heroic ", "Spicy ", "Legendary ", "Deadly ", "Fine ", "Grand ", "Hasty ", "Neat ", "Rapid ", "Unreal ", "Awkward ", "Rich ", "Clean ", "Fierce ", "Heavy ", "Light ", "Mythic ", "Pure ", "Smart ", "Titanic ", "Wise ", "Bizarre ", "Itchy ", "Ominous ", "Pleasant ", "Pretty ", "Shiny ", "Simple ", "Strange ", "Vivid ", "Godly ", "Demonic ", "Forceful ", "Hurtful ", "Keen ", "Strong ", "Superior ", "Unpleasant ", "Zealous "};

  public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");

}
