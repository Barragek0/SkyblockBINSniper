package me.vikame.binsnipe;

import java.awt.SystemTray;

public class Config {

  // NOTE: This config does not support generic-typed collections such as List<String> and instead requires the use of arrays in their place.

  // Whether to output errors to the console or not
  public static boolean OUTPUT_ERRORS = false;

  // The number of threads to use for making API requests, and parsing auction data, when finding
  // flips.
  static int POOLED_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
  
  // The maximum time, in milliseconds, that is allowed to pass before we determine a snipe attempt
  // to be dead.
  static int HYPIXEL_API_TIMEOUT = 20000;
  
  // The maximum time, in milliseconds, that is allowed to pass before we determine the api to be offline.
  static int NEU_API_TIMEOUT = 10000;

  // Whether to play a sound when a flip has been found.
  static boolean SOUND_WHEN_FLIP_FOUND = !SystemTray.isSupported();

  // Whether to show a notification when a flip has been found.
  static boolean NOTIFICATION_WHEN_FLIP_FOUND = SystemTray.isSupported();

  /* Whether to add the "Accept-Encoding: gzip" header to API requests.
   * I would highly recommend against disabling this as it greatly reduces bandwith usage when enabled.
   */
  static boolean USE_GZIP_COMPRESSION_ON_API_REQUESTS = true;

  /* Whether to ensure all API requests do not return a cached response.
   * This employs many tactics to ensure that we do not get a cached response,
   * such as the use of the "Pragma: no-cache" and "Cache-Control: no-cache" headers,
   * alongside a cache-buster parameter within the API request URL.
   *
   * I would recommend keeping this disabled, but if you do not care about your bandwith usage you
   * *may* see a speed-up from enabling this setting.
   */
  static boolean FORCE_NO_CACHE_API_REQUESTS = false;

  // Whether to ignore furniture items, as these are commonly market-manipulated.
  static boolean IGNORE_FURNITURE = true;

  // Whether to ignore cosmetic items, as these are commonly market-manipulated.
  static boolean IGNORE_COSMETICS = true;

  // Whether to ignore used cake souls, as these are more likely than not over-priced or will never
  // sell.
  static boolean IGNORE_USED_CAKE_SOULS = true;

  // Whether to treat all "dungeonizable" items as if they were starless.
  static boolean IGNORE_STARS = true;

  // Whether to treat all reforgeable items the same regardless of applied reforge.
  static boolean IGNORE_REFORGES = true;

  // Whether to ignore whether an item has hot potato books applied.
  static boolean IGNORE_HOT_POTATO = true;

  // Whether to treat skinned items as if they weren't skinned.
  static boolean IGNORE_SKINS = true;

  // Whether to treat recombobulated as their base rarity.
  static boolean IGNORE_RECOMB = false;

  // The maximum amount of time, in milliseconds, that an item must be on the auction house for to
  // be determined "old" and thus ignored.
  static long OLD_THRESHOLD = 180_000;

  // The maximum number of flips to print in the console. Do note that only the highest value flip
  // will be copied to your clipboard.
  static int MAX_FLIPS_TO_SHOW = 5;

  // The minimum number of the same item that must be on the market to be considered for a flip.
  static int MIN_ITEMS_ON_MARKET = 3;

  // The maximum price you are willing to pay for an item to flip.
  static int MAX_BIN_PRICE = Integer.MAX_VALUE;

  // The minimum price the item to be flipped must be able to sell for.
  static int MIN_BIN_PRICE = 1_000_000;

  // The amount of profit required to log a flip. When checking this, we check if *either* the
  // percentage profit gain or raw profit amount are above their respective threshold.
  static float MIN_PROFIT_PERCENTAGE = 110.0f;
  static int MIN_PROFIT_AMOUNT = 1_000_000;

  /* Whether to cache AtomicPrice objects in a pool rather than re-creating them every time.
   *
   * This can be quite memory intensive, however it should also prove to be a speed-up as we
   * will no longer create 6 atomic objects every time we require an AtomicPrice.
   */
  static boolean CACHE_ATOMIC_OBJECTS = true;

  /* Whether to explicitly ask the JVM to dispose of unused objects in memory. This should only be enabled if your machine is struggling with memory.
   *
   * I am well aware of the bad practice surrounding explicit GC calls, but in this situation an explicit GC can actually be quite beneficial as
   * it happens at the very end of any BIN snipe attempt, when no further calls are needed for an extended period, and many dead objects are being
   * held in memory. Do note that this setting will effectively be nullified by the CACHE_ATOMIC_OBJECTS setting, as it will retain AtomicPrice
   * objects thus creating no immediate need for garbage collection.
   */
  static boolean EXPLICIT_GC_AFTER_FLIP = false;

  /* Whether to iterate across results and copy the /viewauction command for each result,
   * one by one, waiting until control + v is pressed before moving onto the next result.
   * Much easier than copying each command manually if you want to check more than one result.
   */
  static boolean ITERATE_RESULTS_TO_CLIPBOARD = true;

  /* List of item names that are 'blacklisted' and won't be added to the list of flips.
   */
  static String[] BLACKLIST = new String[] {"Decoy", "Trap", "null"};

  // Blacklist names need to match the exact item name.
  static boolean BLACKLIST_EXACT_MATCH = true;

  // The minimum amount of sales the item needs to have per day to be considered as a flip. (Using NotEnoughUpdates API)
  static int MINIMUM_DAILY_SALES = 5;

  /* The rate in which we check for API updates, in milliseconds. Setting this value too low can put a
   * large load on your network, and could also get you blocked from the Hypixel API, so we enforce a
   * minimum of 500ms.
   */
  static long UPDATE_CHECK_RATE = 1000;

  // The number of segments to a console-based loading bar used when displaying current BIN snipe
  // progress.
  static transient int LOADING_BAR_SEGMENTS = 40;

  // Whether to enable debug logging. Do note that this could be spammy and is not meant to be used
  // normally.
  static transient boolean DEBUG = false;

  // Whether to enable key listener debug logging. Very spammy.
  public static transient boolean KEY_LISTENER_DEBUG = false;
}
