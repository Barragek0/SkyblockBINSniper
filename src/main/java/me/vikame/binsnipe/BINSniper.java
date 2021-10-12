package me.vikame.binsnipe;

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import me.doubledutch.lazyjson.LazyArray;
import me.doubledutch.lazyjson.LazyObject;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.vikame.binsnipe.util.AtomicPrice;
import me.vikame.binsnipe.util.AtomicPrice.UnboundedAtomicPricePool;
import me.vikame.binsnipe.util.ExpiringSet;
import me.vikame.binsnipe.util.KeyboardListener;
import me.vikame.binsnipe.util.SBHelper;
import me.vikame.binsnipe.util.UnboundedObjectPool;

class BINSniper {

  private static final DecimalFormat PRINT_FORMAT = new DecimalFormat(".##");

  // An object used for synchronization during loading bar printing.
  private final Object lock = new Object();

  // Important information required by the BIN sniper.
  private final AtomicInteger totalPages;
  private final AtomicLong timeLastUpdated;
  private final Map<String, AtomicPrice> binPrices;
  private final UnboundedObjectPool<AtomicPrice> objectPool;

  // We don't want to show the same flip multiple times, so we store a map of already-shown flips
  // and the timestamp in which it should be shown again.
  private final ExpiringSet<String> flipsAlreadyShown;

  // Statistics to be shown when a flip is found.
  private final AtomicInteger totalAuctions;
  private final AtomicInteger totalBins;

  // Used to display notifications to the user
  private final TrayIcon notificationIcon;

  // Used for printing strings that are able to be "erased"
  private int lastClearableLength = -1;

  // Iterative task for copying flips to the clipboard.
  private CompletableFuture<Void> iterativeTask;
  private final AtomicBoolean doingIterativeCopy;

  BINSniper() {
    System.out.println("Starting BIN sniper...");

    totalPages = new AtomicInteger(-1);
    timeLastUpdated = new AtomicLong(-1);
    binPrices = new ConcurrentHashMap<>();

    if (Config.CACHE_ATOMIC_OBJECTS) {
      if (Config.EXPLICIT_GC_AFTER_FLIP) {
        System.err.println(
            "WARNING: The use-case of EXPLICIT_GC_AFTER_FLIP is nullified when CACHE_ATOMIC_OBJECTS is used. We recommend the use of one or the other, not both.");
      }

      objectPool = new UnboundedAtomicPricePool(2048);
    } else {
      objectPool = null;
    }

    flipsAlreadyShown = new ExpiringSet<>(60000 * 5);

    totalAuctions = new AtomicInteger();
    totalBins = new AtomicInteger();

    if (SystemTray.isSupported()) {
      InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream("icon.png");
      BufferedImage image;
      try {
        assert stream != null;
        image = ImageIO.read(stream);
      } catch (AssertionError | IOException e) {
        image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        System.err.println("Failed to find icon! Defaulting to blank icon...");
      }

      notificationIcon = new TrayIcon(image, "BIN Sniper");
      notificationIcon.setImageAutoSize(true);
    } else {
      this.notificationIcon = null;
    }

    doingIterativeCopy = new AtomicBoolean(false);

    // Make an API request to get initial data before the sniper starts.
    getAuctions(0);

    Main.schedule(
        () -> {
          long lastUpdateTime = timeLastUpdated.get();
          if ((System.currentTimeMillis() - lastUpdateTime) > 60000) {
            getAuctions(0);
            long newUpdateTime = timeLastUpdated.get();
            long actualDelay = System.currentTimeMillis() - newUpdateTime;

            if (lastUpdateTime != newUpdateTime) {
              // Notify the user that they took too long to paste the command, cleanup old data, and
              // continue.
              if (iterativeTask != null && !iterativeTask.isDone()) {
                cleanupAuctionData();
                clearString();
                doingIterativeCopy.set(false);
                iterativeTask.cancel(true);
                KeyboardListener.pasteCallback
                    .run(); // HACK: Interrupt the indefinite sleep in the iterative copy task!
                System.out.println(
                    "Commands were not pasted before we started a new flip attempt.");
              }

              AtomicInteger completed = new AtomicInteger(0);
              final int maxPages = totalPages.get();

              System.out.println();
              printLoadingBar(0, maxPages);

              // noinspection rawtypes: We will only store CompletableFuture<Void> in this array.
              CompletableFuture[] futures = new CompletableFuture[maxPages];

              long start = System.currentTimeMillis();
              for (int page = 0; page < maxPages; page++) {
                final int workingPage = page;
                CompletableFuture<Void> future =
                    Main.exec(
                        () -> {
                          // Ensure that this is still a valid page we need to look at.
                          if (workingPage < totalPages.get()) {
                            LazyArray auctionArray = getAuctions(workingPage);

                            Main.printDebug("Got page " + workingPage + " from API.");

                            if (auctionArray != null) {
                              long pageStart = System.currentTimeMillis();
                              for (int i = 0; i < auctionArray.length(); i++) {
                                LazyObject binData = auctionArray.getJSONObject(i);
                                if (!SBHelper.isExistingBIN(binData)) {
                                  continue;
                                }

                                if (Config.IGNORE_FURNITURE && SBHelper.isFurniture(binData)) {
                                  continue;
                                }
                                if (Config.IGNORE_COSMETICS && SBHelper.isCosmetic(binData)) {
                                  continue;
                                }
                                if (Config.IGNORE_USED_CAKE_SOULS
                                    && SBHelper.isUsedCakeSoul(binData)) {
                                  continue;
                                }

                                String itemId;
                                StringBuilder itemName =
                                    new StringBuilder(
                                        SBHelper.stripInvalidChars(binData.getString("item_name")));

                                try {
                                  NBTCompound itemData =
                                      NBTReader.readBase64(binData.getString("item_bytes"))
                                          .getList("i")
                                          .stream()
                                          .findFirst()
                                          .map(
                                              o ->
                                                  o instanceof NBTCompound ? (NBTCompound) o : null)
                                          .orElseThrow(
                                              () ->
                                                  new RuntimeException("Failed to parse NBT data."))
                                          .getCompound("tag");

                                  NBTCompound skyblockAttributes =
                                      itemData.getCompound("ExtraAttributes");

                                  String reforge = skyblockAttributes.getString("modifier");
                                  String id = skyblockAttributes.getString("id");
                                  boolean recombed =
                                      skyblockAttributes.getInt("rarity_upgrades", 0) > 0;
                                  int hotPotatoBooks =
                                      skyblockAttributes.getInt("hot_potato_count", 0);
                                  int stars = skyblockAttributes.getInt("dungeon_item_level", 0);
                                  String skin = skyblockAttributes.getString("skin");

                                  if (Config.IGNORE_STARS && id.startsWith("STARRED_")) {
                                    id = id.substring(8);
                                  }

                                  if (id.equalsIgnoreCase("PET")) {
                                    LazyObject petInfo =
                                        new LazyObject(skyblockAttributes.getString("petInfo"));

                                    String type = petInfo.getString("type");
                                    id = "PET_" + type;
                                    skin = petInfo.has("skin") ? petInfo.getString("skin") : null;
                                  }

                                  itemId =
                                      id
                                          + (Config.IGNORE_RECOMB ? "" : "|" + recombed)
                                          + (Config.IGNORE_REFORGES ? "" : "|" + reforge)
                                          + (Config.IGNORE_HOT_POTATO ? "" : "|" + hotPotatoBooks)
                                          + (Config.IGNORE_STARS ? "" : "|" + stars)
                                          + (Config.IGNORE_SKINS ? "" : "|" + skin);

                                  for (int star = 0; star < stars; star++) {
                                    itemName.append("*");
                                  }

                                  itemName
                                      .append(" [")
                                      .append(binData.getString("tier"))
                                      .append(recombed ? ", RECOMB" : "")
                                      .append("]");

                                  if (skin != null) {
                                    itemName.append(" [").append(skin).append("]");
                                  }
                                } catch (Exception e) {
                                  e.printStackTrace();
                                  continue;
                                }

                                String filteredName =
                                    itemName
                                        .toString()
                                        .replace("✪", "")
                                        .replace(" ✦", "")
                                        .replace("⚚", "Fragged");

                                binPrices
                                    .computeIfAbsent(itemId, ign -> createAtomicPrice())
                                    .tryUpdatePrice(filteredName, binData);
                                totalBins.incrementAndGet();
                              }

                              long pageEnd = System.currentTimeMillis();

                              Main.printDebug(
                                  "Parsed page "
                                      + workingPage
                                      + " in "
                                      + (pageEnd - pageStart)
                                      + "ms.");
                            }
                          }
                        });

                futures[workingPage] =
                    future.thenRun(() -> printLoadingBar(completed.incrementAndGet(), maxPages));
              }

              CompletableFuture<Void> all = CompletableFuture.allOf(futures);
              try {
                all.get(Config.TIMEOUT, TimeUnit.MILLISECONDS);
              } catch (InterruptedException | ExecutionException | TimeoutException e) {
                all.cancel(true);
                clearString();
                System.out.println("Could not retrieve all auctions from the Hypixel API in time!");
                System.out.println("This may be due to your internet connection being slow, or");
                System.out.println("the Hypixel API may be responding slowly.");
                return;
              }

              clearString();

              Main.printDebug(binPrices.size() + " total BINs processed!");

              TreeSet<Map.Entry<String, AtomicPrice>> flips =
                  new TreeSet<>(Comparator.comparingInt(o -> o.getValue().getProjectedProfit()));

              for (Map.Entry<String, AtomicPrice> entry : binPrices.entrySet()) {
                if (flipsAlreadyShown.contains(entry.getKey())) {
                  continue;
                }

                AtomicPrice price = entry.getValue();
                int lowest = price.getLowestValue();
                int second = price.getSecondLowestValue();

                if (price.getTotalCount() >= Config.MIN_ITEMS_ON_MARKET
                    && second >= Config.MIN_BIN_PRICE
                    && lowest <= Config.MAX_BIN_PRICE
                    && price.getLowestElapsedTime() <= Config.OLD_THRESHOLD) {
                  int secondWithTaxes = SBHelper.calculateWithTaxes(second);

                  int diff = secondWithTaxes - lowest;
                  float profitPercentage = ((float) secondWithTaxes / (float) lowest) * 100.0f;

                  if (profitPercentage >= Config.MIN_PROFIT_PERCENTAGE
                      || diff >= Config.MIN_PROFIT_AMOUNT) {
                    Main.printDebug(
                        "Found flippable item '"
                            + entry.getKey()
                            + "' ("
                            + entry.getValue().getLowestItemName()
                            + ")");
                    boolean itemOnBlacklist = false;
                    for (String blacklistItem : Config.BLACKLIST) {
                      if (Config.BLACKLIST_EXACT_MATCH
                          ? price.getLowestItemName().equalsIgnoreCase(blacklistItem)
                          : price.getLowestItemName().toLowerCase().contains(blacklistItem)) {
                        Main.printDebug(price.getLowestItemName() + " is blacklisted.");
                        itemOnBlacklist = true;
                      }
                    }
                    if (Config.MINIMUM_MARKET_VOLUME <= price.getTotalCount() && !itemOnBlacklist) {
                      if (flips.size() < Config.MAX_FLIPS_TO_SHOW) {
                        flips.add(entry);
                      } else {
                        Map.Entry<String, AtomicPrice> first = flips.first();
                        AtomicPrice firstPrice = first.getValue();

                        if (firstPrice.getProjectedProfit() < price.getProjectedProfit()) {
                          flips.pollFirst();
                          flips.add(entry);
                        }
                      }
                    } else {
                      Main.printDebug(
                          price.getLowestItemName()
                              + " is blacklisted or doesn't meet required minimum market volume ("
                              + Config.MINIMUM_MARKET_VOLUME
                              + " <= "
                              + price.getTotalCount()
                              + ").");
                    }
                  }
                }
              }

              long timeTaken = System.currentTimeMillis() - start;
              if (flips.isEmpty()) {
                System.out.println("Unable to find a flip after " + timeTaken + " ms.");
              } else {
                for (Map.Entry<String, AtomicPrice> entry : flips) {
                  flipsAlreadyShown.add(entry.getKey());

                  AtomicPrice price = entry.getValue();

                  int lowest = price.getLowestValue();
                  int second = price.getSecondLowestValue();

                  int secondWithTaxes = SBHelper.calculateWithTaxes(second);

                  int diff = secondWithTaxes - lowest;
                  float profitPercentage =
                      (((float) secondWithTaxes / (float) lowest) * 100.0f) - 100;

                  System.out.println(
                      "/viewauction "
                          + price.getLowestKey()
                          + " | Item: "
                          + price.getLowestItemName()
                          + " | Volume: "
                          + price.getTotalCount()
                          + " | Price: "
                          + formatValue(lowest)
                          + " | Next: "
                          + formatValue(second)
                          + " | Profit: "
                          + formatValue(diff)
                          + " (+"
                          + PRINT_FORMAT.format(profitPercentage)
                          + "%) ("
                          + timeTaken
                          + "ms)");
                }

                AtomicPrice best = flips.last().getValue();

                copyCommandToClipboard("/viewauction " + best.getLowestKey());

                Main.printDebug("Flip timing information:");
                Main.printDebug(" > " + timeTaken + "ms for page processing");
                Main.printDebug(" > " + actualDelay + "ms for page update");
                Main.printDebug(
                    " > In total, " + (System.currentTimeMillis() - newUpdateTime) + "ms late");

                // If iterating is enabled, we can do these for each item while iterating instead
                if (!Config.ITERATE_RESULTS_TO_CLIPBOARD) {
                  sendSound();
                  sendNotification(best);
                }
              }

              // If we are iterating over results and copying, start the task and only cleanup
              // afterward.
              if (Config.ITERATE_RESULTS_TO_CLIPBOARD) {
                doingIterativeCopy.set(true);
                iterativeTask =
                    Main.exec(() -> iterateResultsToClipboard(flips))
                        .thenRun(this::cleanupAuctionData);
              } else {
                cleanupAuctionData();
              }
            }
          }
        },
        0,
        1000,
        TimeUnit.MILLISECONDS);
  }

  private static String formatValue(final long amount, final long div, final char suffix) {
    return PRINT_FORMAT.format(amount / (double) div) + suffix;
  }

  private static String formatValue(final long amount) {
    if (amount >= 1_000_000_000_000_000L) {
      return formatValue(amount, 1_000_000_000_000_000L, 'q');
    } else if (amount >= 1_000_000_000_000L) {
      return formatValue(amount, 1_000_000_000_000L, 't');
    } else if (amount >= 1_000_000_000L) {
      return formatValue(amount, 1_000_000_000L, 'b');
    } else if (amount >= 1_000_000L) {
      return formatValue(amount, 1_000_000L, 'm');
    } else if (amount >= 100_000L) {
      return formatValue(amount, 1000L, 'k');
    }

    return NumberFormat.getInstance().format(amount);
  }

  private void cleanupAuctionData() {
    if (objectPool != null) {
      binPrices.values().forEach(objectPool::offer);
    }

    binPrices.clear();
    totalBins.lazySet(0);

    if (Config.EXPLICIT_GC_AFTER_FLIP) {
      System.gc();
    }
  }

  private void sendNotification(AtomicPrice best) {
    if (Config.NOTIFICATION_WHEN_FLIP_FOUND && notificationIcon != null) {
      try {
        // Add our notification icon to the system tray.
        SystemTray.getSystemTray().add(notificationIcon);
      } catch (AWTException e) {
        e.printStackTrace();
        System.err.println("Failed to initialize notification system.");
      }

      notificationIcon.displayMessage(
          best.getLowestItemName() + " (Volume: " + best.getTotalCount() + ")",
          "Price: "
              + NumberFormat.getInstance().format(best.getLowestValue())
              + "\n"
              + "Next: "
              + NumberFormat.getInstance().format(best.getSecondLowestValue())
              + "\n"
              + "Profit: "
              + NumberFormat.getInstance().format(best.getProjectedProfit()),
          MessageType.INFO);

      // Remove the system tray icon once we have displayed a message.
      SystemTray.getSystemTray().remove(notificationIcon);
    }
  }

  private void iterateResultsToClipboard(final TreeSet<Map.Entry<String, AtomicPrice>> flips) {
    System.out.println();

    int finished = 0;
    int size = flips.size();

    // iterate from most profit to least
    for (Map.Entry<String, AtomicPrice> entry : flips.descendingSet()) {
      if (!doingIterativeCopy.get())
        return; // Exit early if we have been told to stop doing the iterative copy.

      String key = entry.getValue().getLowestKey();

      Main.printDebug("Copying auction " + key + "!");

      copyCommandToClipboard("/viewauction " + key);
      // wait here until control + v pressed
      printClearableString(
          "Clipboard set to "
              + "/viewauction "
              + key
              + ", paste the command in-game ("
              + (finished + 1)
              + "/"
              + size
              + ")");

      sendSound();
      sendNotification(entry.getValue());

      Thread current = Thread.currentThread();

      // Interrupt the sleeping thread when the paste callback is triggered.
      KeyboardListener.pasteCallback = current::interrupt;

      try {
        // Block the thread indefinitely until the pasteCallback interrupts the thread.
        Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException ignored) {
      }

      if (!doingIterativeCopy.get())
        return; // Exit early if we have been told to stop doing the iterative copy.

      finished++;
    }

    clearString();
  }

  private void sendSound() {
    if (Config.SOUND_WHEN_FLIP_FOUND) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  private void copyCommandToClipboard(String command) {
    Toolkit.getDefaultToolkit()
        .getSystemClipboard()
        .setContents(new StringSelection(command), null);
  }

  private AtomicPrice createAtomicPrice() {
    return objectPool != null ? objectPool.borrow() : new AtomicPrice();
  }

  private LazyArray getAuctions(int page) {
    String targetURL =
        Constants.AUCTIONS_ENDPOINT
            + "?page="
            + page
            + (Config.FORCE_NO_CACHE_API_REQUESTS ? "&_=" + System.currentTimeMillis() : "");
    URL apiURL;
    try {
      apiURL = new URL(targetURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      System.err.println("Malformed URL '" + targetURL + "'");
      System.exit(1);
      return null;
    }

    try {
      HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36");
      connection.setRequestProperty("Content-Type", "application/json");

      if (Config.USE_GZIP_COMPRESSION_ON_API_REQUESTS) {
        connection.setRequestProperty("Accept-Encoding", "gzip");
      }

      if (Config.FORCE_NO_CACHE_API_REQUESTS) {
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setUseCaches(false);
      }

      try {
        connection.connect();
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Failed to connect to " + targetURL + ": " + e.getMessage());
        System.err.println("This may be due to your firewall, or anti-virus software.");
        System.err.println(
            "Please ensure that the Java Virtual Machine is able to access the internet.");
        return null;
      }

      if (connection.getResponseCode() != 200) {
        connection.disconnect();
        return null; // As per https://api.hypixel.net/#tag/SkyBlock/paths/~1skyblock~1auctions/get,
        // all responses other than 200 indicate failure.
      }

      BufferedReader responseStreamReader;
      if (connection.getContentEncoding().equalsIgnoreCase("gzip")) {
        responseStreamReader =
            new BufferedReader(
                new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
      } else {
        responseStreamReader =
            new BufferedReader(new InputStreamReader(connection.getInputStream()));
      }

      StringBuilder response = new StringBuilder();

      int ch;
      while ((ch = responseStreamReader.read()) != -1) {
        response.append((char) ch);
      }

      responseStreamReader.close();
      connection.disconnect();

      LazyObject responseJsonObject = new LazyObject(response.toString());
      if (!responseJsonObject.getBoolean("success")) {
        return null;
      }

      long timeLastUpdated = responseJsonObject.getLong("lastUpdated");

      if (timeLastUpdated > this.timeLastUpdated.get()) {
        this.timeLastUpdated.set(timeLastUpdated);
        this.totalPages.set(responseJsonObject.getInt("totalPages"));
        this.totalAuctions.set(responseJsonObject.getInt("totalAuctions"));
      }

      return responseJsonObject.getJSONArray("auctions");
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    return null;
  }

  private void printClearableString(String clearable) {
    synchronized (lock) {
      int lastClearable = lastClearableLength;

      lastClearableLength = clearable.length();
      StringBuilder output = new StringBuilder("\r").append(clearable);

      if (lastClearable != -1 && lastClearable > lastClearableLength) {
        for (int i = 0; i < lastClearable - lastClearableLength; i++) {
          output.append(' ');
        }
      }

      System.out.print(output);
    }
  }

  private void clearString() {
    synchronized (lock) {
      if (lastClearableLength != -1) {
        clearChars(lastClearableLength);
        lastClearableLength = -1;
      }
    }
  }

  private void printLoadingBar(int current, int max) {
    synchronized (lock) {
      float progress = (float) current / (float) max;

      StringBuilder output = new StringBuilder("\r[");

      int segments = (int) Math.floor(progress * Config.LOADING_BAR_SEGMENTS);
      for (int i = 0; i < Config.LOADING_BAR_SEGMENTS; i++) {
        if (i < segments - 1) {
          output.append('=');
        } else if (i == segments - 1) {
          output.append('>');
        } else {
          output.append(' ');
        }
      }

      output
          .append("] ")
          .append((int) (Math.ceil(progress * 100.0F)))
          .append("% (")
          .append(current)
          .append("/")
          .append(max)
          .append(")");

      printClearableString(output.toString());
    }
  }

  private void clearChars(int amt) {
    synchronized (lock) {
      StringBuilder output = new StringBuilder("\r");
      for (int i = 0; i < amt; i++) {
        output.append(' ');
      }
      output.append("\r");

      System.out.print(output);
    }
  }

  void cleanup() {
    if (SystemTray.isSupported() && notificationIcon != null) {
      SystemTray.getSystemTray().remove(notificationIcon);
    }
  }
}
